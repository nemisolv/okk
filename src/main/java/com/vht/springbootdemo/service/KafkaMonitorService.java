package com.vht.springbootdemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vht.springbootdemo.api.KafkaApiClient;
import com.vht.springbootdemo.api.KafkaConnectResponseDeserializer;
import com.vht.springbootdemo.config.AppProperties;
import com.vht.springbootdemo.dto.AlarmMessage;
import com.vht.springbootdemo.dto.AlarmType;
import com.vht.springbootdemo.dto.KafkaStatus;
import com.vht.springbootdemo.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KafkaMonitorService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final FaultManagementService faultManagementService;
    private final RedisService redisService;
    private final List<String> instanceUrls;
    private final AppProperties appProperties;
    private final KafkaConnectResponseDeserializer kafkaConnectResponseDeserializer;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    private static final String REDIS_KEY = "kafka:status";

    public KafkaMonitorService(RedisTemplate<String, Object> redisTemplate,
                               FaultManagementService faultManagementService,
                               RedisService redisService,
                               AppProperties appProperties,
                               KafkaConnectResponseDeserializer kafkaConnectResponseDeserializer) {
        this.redisTemplate = redisTemplate;
        this.faultManagementService = faultManagementService;
        this.redisService = redisService;
        this.appProperties = appProperties;
        this.kafkaConnectResponseDeserializer = kafkaConnectResponseDeserializer;
        this.instanceUrls = this.appProperties.getInstance().getUrl();
    }

    public void monitorKafkaStatus() {
        log.info("🚀 Bắt đầu giám sát trạng thái Kafka Connect...");

        CompletableFuture<Map<String, KafkaStatus>> kafkaFuture = CompletableFuture.supplyAsync(
                () -> instanceUrls.stream()
                        .map(instance -> CompletableFuture.supplyAsync(
                                () -> kafkaConnectResponseDeserializer.deserializeKafkaStatusApiResponse(instance), executorService))
                        .map(CompletableFuture::join)
                        .flatMap(map -> map.entrySet().stream())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                executorService);

        CompletableFuture<Map<String, KafkaStatus>> redisFuture =
                CompletableFuture.supplyAsync(redisService::fetchAllInstancesStatusFromRedis, executorService);

        CompletableFuture.allOf(kafkaFuture, redisFuture)
                .thenRunAsync(() -> processKafkaStatus(kafkaFuture.join(), redisFuture.join()))
                .join();
    }

    private void processKafkaStatus(Map<String, KafkaStatus> newStatusMap, Map<String, KafkaStatus> oldStatusMap) {
        long eventTime = Instant.now().toEpochMilli();
        for (Map.Entry<String, KafkaStatus> newEntry : newStatusMap.entrySet()) {
            String key = newEntry.getKey();
            KafkaStatus newStatus = newEntry.getValue();
            KafkaStatus oldStatus = oldStatusMap.get(key);
            handleStateChange(newStatus, oldStatus, eventTime);
            oldStatusMap.remove(key);
        }
        oldStatusMap.forEach((key, oldStatus) -> clearStaleStatus(oldStatus, eventTime));
    }

    private void handleStateChange(KafkaStatus newStatus, KafkaStatus oldStatus, long eventTime) {
        String newState = newStatus.getState();
        String oldState = oldStatus != null ? oldStatus.getState() : null;
        boolean wasFailedOrUnassigned = oldStatus != null && (oldState.equals("FAILED") || oldState.equals("UNASSIGNED"));
        boolean isNowFailedOrUnassigned = newState.equals("FAILED") || newState.equals("UNASSIGNED");
        boolean isTheSameBothFailedOrBothUnassigned = oldState != null && oldState.equals(newState);
        long initialTime = isTheSameBothFailedOrBothUnassigned ? oldStatus.getInitialTime() : eventTime;
        long triggerTime = eventTime;
        boolean isChanged =( oldStatus == null && isNowFailedOrUnassigned )// trường hợp old RUNNING new RUNNING nếu k so sánh isNowFailedOrUnassigned thì lại thành true( bỏ cũng được vì thực chất nó vãn hoạt động đúng, vì ta không lưu vào redis + gửi alarm nên cũng k sao)
                            || !newState.equals(oldState)
                            || !newStatus.getTrace().equals(oldStatus.getTrace())
                ;
        boolean isAlarmRequired = isNowFailedOrUnassigned;

        AlarmMessage alarmMessage = AlarmMessage.builder()
                .ne(appProperties.getFixedFields().getNe())
                .alarmId(appProperties.getFixedFields().getAlarmId())
                .internalService(appProperties.getFixedFields().getInternalService())
                .neIp(appProperties.getFixedFields().getNeIp())
                .eventType(isAlarmRequired ? AlarmType.RAISE_ALARM.getCode() : AlarmType.CLEAR_ALARM.getCode())
                .location(newStatus.getLocation())
                .initialTime(initialTime)
                .triggerTime(triggerTime)
                .additionInfo("Connector/Task state is " + newState)
                .probableCause(newStatus.getTrace())
                .isChanged(isChanged)
                .build();

        newStatus.setInitialTime(initialTime);

        if (oldStatus != null && wasFailedOrUnassigned) {
            if(!isTheSameBothFailedOrBothUnassigned) {
//                faultManagementService.sendAlarm(alarmMessage);
//                redisTemplate.opsForHash().put(REDIS_KEY, newStatus.getLocation(), newStatus);
//            }else {
                AlarmMessage clearAlarmMessage = AlarmMessage.newCopy(alarmMessage);
                clearAlarmMessage.setEventType(AlarmType.CLEAR_ALARM.getCode());
                clearAlarmMessage.setProbableCause(oldStatus.getTrace());
                clearAlarmMessage.setInitialTime(oldStatus.getInitialTime());
                clearAlarmMessage.setAdditionInfo(oldStatus.getAddtionalInfo());
            int code=     faultManagementService.sendAlarm(clearAlarmMessage);
            if(code ==200) {
                redisService.deleteKafkaStatus(clearAlarmMessage.getLocation());
                log.info("🚀 Gửi clear alarm cho location: {}", newStatus.getLocation());
            }else {
                // neu k gui duoc clear lên FM thi phai cap nhat lai cai cũ rồi lưu vào redis và coi như đây là alarm rác, sẽ được tự động giải phóng ở lần quét sau
                newStatus.setInitialTime(oldStatus.getInitialTime());
                String dirtyAlarmKey = newStatus.getLocation() + "_dirty"+"_"+eventTime;
                redisService.saveKafkaStatus(newStatus,dirtyAlarmKey);
            }
            }
        }

        if (isAlarmRequired) {
           int code =  faultManagementService.sendAlarm(alarmMessage);
            if(code ==200) {
                redisService.saveKafkaStatus(newStatus);
                log.info("🚀 Gửi alarm cho location: {}", newStatus.getLocation());
            }
        }
    }

    private void clearStaleStatus(KafkaStatus oldStatus, long eventTime) {
        AlarmMessage alarmMessage = AlarmMessage.builder()
                .ne(appProperties.getFixedFields().getNe())
                .alarmId(appProperties.getFixedFields().getAlarmId())
                .internalService(appProperties.getFixedFields().getInternalService())
                .neIp(appProperties.getFixedFields().getNeIp())
                .eventType(AlarmType.CLEAR_ALARM.getCode())
                .location(oldStatus.getLocation())
                .initialTime(oldStatus.getInitialTime())
                .triggerTime(eventTime)
                .additionInfo("Connector/Task no longer exists in Kafka response")
                .probableCause(oldStatus.getTrace())
                .isChanged(true)
                .build();
        faultManagementService.sendAlarm(alarmMessage);
        redisService.deleteKafkaStatus(oldStatus.getLocation());
    }


}
