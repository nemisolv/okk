package com.vht.kafkamonitoring.service;

import com.vht.kafkamonitoring.api.KafkaConnectResponseDeserializer;
import com.vht.kafkamonitoring.config.MonitoringConfigProperties;
import com.vht.kafkamonitoring.dto.AlarmMessage;
import com.vht.kafkamonitoring.dto.AlarmType;
import com.vht.kafkamonitoring.dto.MonitoredState;
import com.vht.kafkamonitoring.dto.MonitoredStatus;
import com.vht.kafkamonitoring.exception.KafkaConnectApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaMonitorService {
    private final FaultManagementService faultManagementService;
    private final RedisService redisService;
    private final MonitoringConfigProperties monitoringConfigProperties;
    private final KafkaConnectResponseDeserializer kafkaConnectResponseDeserializer;
    private long eventTime = Instant.now().toEpochMilli();

    public void monitorKafkaStatus() {
        log.info("🚀 Bắt đầu giám sát trạng thái Kafka Connect...");

        List<String> instances = monitoringConfigProperties.getKafkaInstances();

        List<CompletableFuture<Void>> futures = instances.stream()
                .map(this::processKafkaInstance)
                .toList();

        // Chờ tất cả hoàn thành nếu cần đồng bộ
//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("✅ Kết thúc kiểm tra Kafka Connect");
    }

    @Async("kafkaMonitorExecutor")
    protected CompletableFuture<Void> processKafkaInstance(String instanceUrl) {
        // call api kafka connect
        try {
            Map<String, MonitoredStatus> currentKafka = kafkaConnectResponseDeserializer.deserializeKafkaStatusApiResponse(instanceUrl);
            //  check nếu có previous trong redis thì clear alarm, cập nhật redis
            MonitoredStatus elementInstanceRedis = redisService.getElementInAKafkaSpecificInstance(instanceUrl);
            if(elementInstanceRedis != null) {
                // clear
                AlarmMessage alarmMessage = AlarmMessage.builder()
                        .eventType(AlarmType.CLEAR_ALARM.getCode())
                        .location(instanceUrl)
                        .initialTime(elementInstanceRedis.getInitialTime())
                        .triggerTime(eventTime)
                        .additionInfo("Clear alarm instance: " + instanceUrl)
                        .probableCause("Clear alarm instance: " + instanceUrl)
                        .isChanged(true)
                        .build();
                int code = faultManagementService.sendAlarm(alarmMessage);
                if(code ==200) {
                    redisService.putKafkaStateIntoSpecificInstance(null, instanceUrl);
                }
            }
            Map<String, MonitoredStatus> redisKafka = redisService.fetchSingleKafkaInstanceStateFromRedis(instanceUrl);
            processKafkaStatus(currentKafka, redisKafka);
            return CompletableFuture.completedFuture(null);


        } catch (KafkaConnectApiException ex) {
            // TODO: send alarm và cập nhật redis
            MonitoredStatus errorStatus = MonitoredStatus.builder()
                    .location(instanceUrl)
                    .state(MonitoredState.FAILED)
                    .probableCause("Could connect to Kafka Connect API instance: " + instanceUrl + " - ex: " + ex.getMessage())
                    .additionalInfo("")
                    .build();

            raiseAlarm(errorStatus);

        } catch (Exception ex) {
            log.warn("Error when fetching Kafka status from " + instanceUrl);
        }
        return null;
    }

    private void processKafkaStatus(Map<String, MonitoredStatus> newStatusMap, Map<String, MonitoredStatus> oldStatusMap) {
        for (Map.Entry<String, MonitoredStatus> newEntry : newStatusMap.entrySet()) {
            String key = newEntry.getKey();
            MonitoredStatus newStatus = newEntry.getValue();
            MonitoredStatus oldStatus = oldStatusMap.get(key);
            handleStateChange(newStatus, oldStatus);
            oldStatusMap.remove(key);
        }
        oldStatusMap.forEach((key, oldStatus) -> clearStaleStatus(oldStatus));
    }

    private void handleStateChange(MonitoredStatus newStatus, MonitoredStatus oldStatus) {
        MonitoredState newState = newStatus.getState();
        MonitoredState oldState = oldStatus != null ? oldStatus.getState() : null;

        // Trường hợp 1: Trạng thái trước là null (RUNNING hoặc PAUSED)
        if (oldStatus == null) {
            if (isFailedOrUnassigned(newState)) {
                raiseAlarm(newStatus);
            }
            return;
        }

        // Trường hợp 2: Trạng thái trước là UNASSIGNED hoặc FAILED
        if (isFailedOrUnassigned(oldState)) {
            handlePreviousFailedOrUnassignedState(newStatus, oldStatus, eventTime);
            return;
        }

        // Trường hợp 3: Các trạng thái khác (RUNNING hoặc PAUSED -> FAILED/UNASSIGNED)
        if (isFailedOrUnassigned(newState)) {
            raiseAlarm(newStatus);
        }
    }
    private boolean isFailedOrUnassigned(MonitoredState state) {
        return state.equals(getMonitoredState("FAILED")) || state.equals(getMonitoredState("UNASSIGNED"));
    }
    private void handlePreviousFailedOrUnassignedState(MonitoredStatus newStatus, MonitoredStatus oldStatus, long eventTime) {
        MonitoredState newState = newStatus.getState();
        MonitoredState oldState = oldStatus.getState();

        if (oldState.equals(newState)) {
            // Trường hợp: Trước đó là UNASSIGNED và bây giờ vẫn UNASSIGNED || trước đó là FAILED và bây giờ vẫn FAILED
            raiseAlarm(newStatus);
        } else {
            // Trường hợp: Chuyển đổi giữa UNASSIGNED <-> FAILED
            clearAlarm(oldStatus, eventTime);
            raiseAlarm(newStatus);
        }
    }
    private void raiseAlarm(MonitoredStatus status) {
        AlarmMessage alarmMessage = buildAlarmMessage(status, AlarmType.RAISE_ALARM.getCode(), eventTime);
        int code = faultManagementService.sendAlarm(alarmMessage);
        if (code == 200) {
            redisService.putKafkaStateIntoSpecificInstance(status, status.getLocation());
            log.info("🚀 Gửi alarm cho location: {}", status.getLocation());
        }
    }
    private void clearAlarm(MonitoredStatus status, long eventTime) {
        AlarmMessage clearAlarmMessage = buildAlarmMessage(status, AlarmType.CLEAR_ALARM.getCode(), status.getInitialTime());
        int code = faultManagementService.sendAlarm(clearAlarmMessage);
        if (code == 200) {
            redisService.putKafkaStateIntoSpecificInstance(null, status.getLocation());
            log.info("🚀 Clear alarm cho location: {}", status.getLocation());
        } else {
            // Xử lý trường hợp không gửi được clear alarm
            String dirtyAlarmKey = status.getLocation() + "_dirty" + "_" + eventTime;
            redisService.putKafkaStateIntoSpecificInstance(status, dirtyAlarmKey);
        }
    }
    private AlarmMessage buildAlarmMessage(MonitoredStatus status, String eventType, long initialTime) {
        return AlarmMessage.builder()
                .eventType(eventType)
                .location(status.getLocation())
                .initialTime(initialTime)
                .triggerTime(eventTime)
                .additionInfo(status.getAdditionalInfo())
                .probableCause(status.getProbableCause())
                .isChanged(true)
                .build();
    }


    private void clearStaleStatus(MonitoredStatus oldStatus) {
        AlarmMessage alarmMessage = buildAlarmMessage(oldStatus, AlarmType.CLEAR_ALARM.getCode(), oldStatus.getInitialTime());

        int code = faultManagementService.sendAlarm(alarmMessage);
        if(code ==200) {
            redisService.putKafkaStateIntoSpecificInstance(null, oldStatus.getLocation());
        }


//        redisService.deleteKafkaStatus(oldStatus.getLocation());
    }

    private MonitoredState getMonitoredState(String state) {
        return MonitoredState.fromString(state);
    }









//    private void handleStateChange(MonitoredStatus newStatus, MonitoredStatus oldStatus, long eventTime) {
//        MonitoredState newState = newStatus.getState();
//        MonitoredState oldState = oldStatus != null ? oldStatus.getState() : null;
//        boolean wasFailedOrUnassigned = oldStatus != null && (oldState.equals(getMonitoredState("FAILED")) || oldState.equals(getMonitoredState("UNASSIGNED")));
//        boolean isNowFailedOrUnassigned = newState.equals(getMonitoredState("FAILED")) || newState.equals(getMonitoredState("UNASSIGNED"));
//        boolean isTheSameBothFailedOrBothUnassigned = oldState != null && oldState.equals(newState);
//        long initialTime = isTheSameBothFailedOrBothUnassigned ? oldStatus.getInitialTime() : eventTime;
//        long triggerTime = eventTime;
//        boolean isChanged = (oldStatus == null && isNowFailedOrUnassigned)// trường hợp old RUNNING new RUNNING nếu k so sánh isNowFailedOrUnassigned thì lại thành true( bỏ cũng được vì thực chất nó vãn hoạt động đúng, vì ta không lưu vào redis + gửi alarm nên cũng k sao)
//                || !newState.equals(oldState)
//                || !newStatus.getProbableCause().equals(oldStatus.getProbableCause());
//        boolean isAlarmRequired = isNowFailedOrUnassigned;
//
//        AlarmMessage alarmMessage = AlarmMessage.builder()
//                .eventType(isAlarmRequired ? AlarmType.RAISE_ALARM.getCode() : AlarmType.CLEAR_ALARM.getCode())
//                .location(newStatus.getLocation())
//                .initialTime(initialTime)
//                .triggerTime(triggerTime)
//                .additionInfo(newStatus.getAdditionalInfo())
//                .probableCause(newStatus.getProbableCause())
//                .isChanged(isChanged)
//                .build();
//
//        newStatus.setInitialTime(initialTime);
//
//        if (wasFailedOrUnassigned) { // nếu trước đó có alarm(redis) (unassigned hoặc failed)
//            if (!isTheSameBothFailedOrBothUnassigned) {  // nếu trước đó là unassigned mà bây giờ là failed hoặc ngược lại
//                int code = faultManagementService.sendAlarm(alarmMessage);
////                redisTemplate.opsForHash().put(REDIS_KEY, newStatus.getLocation(), newStatus);
//                if (code == 200) {
//                    redisService.putKafkaStateIntoSpecificInstance(newStatus, newStatus.getLocation());
//                }
//            } else { // nếu trước đó là unassigned và bây giờ cũng unassigned, hoặc trước đó là failed mà bây giờ vẫn failed
//                AlarmMessage clearAlarmMessage = AlarmMessage.newCopy(alarmMessage);
//                clearAlarmMessage.setEventType(AlarmType.CLEAR_ALARM.getCode());
//                clearAlarmMessage.setProbableCause(oldStatus.getProbableCause());
//                clearAlarmMessage.setInitialTime(oldStatus.getInitialTime());
//                clearAlarmMessage.setAdditionInfo(oldStatus.getAdditionalInfo());
//                int code = faultManagementService.sendAlarm(clearAlarmMessage);
//                if (code == 200) {
////                redisService.deleteKafkaStatus(clearAlarmMessage.getLocation());
//                    redisService.putKafkaStateIntoSpecificInstance(null, newStatus.getLocation());
//
//                    log.info("🚀 Gửi clear alarm cho location: {}", newStatus.getLocation());
//                } else {
//                    // neu k gui duoc clear lên FM thi phai cap nhat lai cai cũ rồi lưu vào redis và coi như đây là alarm rác, sẽ được tự động giải phóng ở lần quét sau
//                    newStatus.setInitialTime(oldStatus.getInitialTime());
//                    String dirtyAlarmKey = newStatus.getLocation() + "_dirty" + "_" + eventTime;
////                redisService.saveKafkaStatus(newStatus,dirtyAlarmKey);
//                    redisService.putKafkaStateIntoSpecificInstance(newStatus,dirtyAlarmKey);
//                }
//            }
//        }
//
//        if (isAlarmRequired) {
//            int code = faultManagementService.sendAlarm(alarmMessage);
//            if (code == 200) {
////                redisService.saveKafkaStatus(newStatus);
//                redisService.putKafkaStateIntoSpecificInstance(newStatus, newStatus.getLocation());
//                log.info("🚀 Gửi alarm cho location: {}", newStatus.getLocation());
//            }
//        }
//    }

}
