package com.vht.springbootdemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vht.springbootdemo.api.KafkaApiClient;
import com.vht.springbootdemo.dto.AlarmMessage;
import com.vht.springbootdemo.dto.KafkaStatus;
import com.vht.springbootdemo.util.LogUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KafkaMonitorService {
    private final RedisTemplate<String, String> redisTemplate;
    private final FaultManagementService faultManagementService;
    private final KafkaApiClient kafkaApiClient;
    private final ObjectMapper objectMapper;
    private final RedisService redisService;

    private static final String REDIS_KEY = "kafka:status";

    public void monitorKafkaStatus(String kafkaApiResponse) {
        // 1. Parse JSON response từ Kafka API
        Map<String, KafkaStatus> newStatusMap = kafkaApiClient.fetchKafkaStatus(kafkaApiResponse);

        // 2. Lấy trạng thái cũ từ Redis
        Map<String, KafkaStatus> oldStatusMap = redisService.fetchStatusFromRedis();

        // 3. So sánh & xử lý sự thay đổi trạng thái
        processStatusChanges(newStatusMap, oldStatusMap);

        // 4. Cập nhật lại Redis
//        redisService.syncStatusToRedis(newStatusMap);
    }



    private void processStatusChanges(Map<String, KafkaStatus> newStatusMap, Map<String, KafkaStatus> oldStatusMap) {
        for (Map.Entry<String, KafkaStatus> newEntry : newStatusMap.entrySet()) {
            String key = newEntry.getKey();
            KafkaStatus newStatus = newEntry.getValue();
            KafkaStatus oldStatus = oldStatusMap.get(key);

            handleStateChange(newStatus, oldStatus);
            oldStatusMap.remove(key);
        }

        // Xóa các trạng thái lỗi thời (không còn trong Kafka API)
        for (Map.Entry<String, KafkaStatus> oldEntry : oldStatusMap.entrySet()) {
            AlarmMessage alarmMessage = new AlarmMessage(
                    oldEntry.getValue().getName(),
                    oldEntry.getValue().getState(),
                    oldEntry.getValue().getWorkerId(),
                    "Connector/Task no longer exists in Kafka response",
                    true,
                    false,
                    Instant.now().getEpochSecond(),
                    Instant.now().getEpochSecond()
            );
            faultManagementService.clearAlarm(alarmMessage);
            redisTemplate.opsForHash().delete(REDIS_KEY, oldEntry.getKey());
        }
    }


    private void handleStateChange(KafkaStatus newStatus, KafkaStatus oldStatus) {
        // isChanged khi trạng thái mới khác trạng thái cũ
        boolean isChanged = oldStatus == null || !oldStatus.getState().equals(newStatus.getState());
        boolean isAlarmExist = !(newStatus.getState().equals("RUNNING") || newStatus.getState().equals("PAUSED"));

        String problemCause = buildProblemCause(newStatus);

        Long initialTime = isChanged ? Instant.now().getEpochSecond() : oldStatus.getInitialTime();
        Long triggerTime = Instant.now().getEpochSecond(); // Luôn cập nhật triggerTime

        AlarmMessage alarmMessage = new AlarmMessage(
                newStatus.getName(),
                newStatus.getState(),
                newStatus.getWorkerId(),
                problemCause,
                isChanged,
                isAlarmExist,
                initialTime,
                triggerTime
        );

        // chú ý: vì kafka connect không trả về initialTime nên phải set lại để lưu vào redis
        newStatus.setInitialTime(initialTime);



        // Xử lý logic clear/log alarm theo yêu cầu
        if (newStatus.getState().equals("RUNNING")) {
            if (oldStatus != null) { // Nếu trước đó có trạng thái thì mới cần clear
                LogUtil.info("[INFO] Clearing alarm for: " + newStatus.getName());
                faultManagementService.clearAlarm(alarmMessage);
                redisTemplate.opsForHash().delete(REDIS_KEY, newStatus.getName());
            }
        } else if (newStatus.getState().equals("PAUSED")) {
            LogUtil.info("[INFO] Kafka connector/task " + newStatus.getName() + " is PAUSED. No alarm action required.");
        } else {
            // Nếu là trạng thái lỗi (FAILED, UNASSIGNED, v.v.)
            LogUtil.warn("[ALARM] Raising alarm for: " + newStatus.getName() + " - " + problemCause);
            faultManagementService.raiseAlarm(alarmMessage);

            // Cập nhật Redis khi có alarm
            try {
                redisTemplate.opsForHash().put(REDIS_KEY, newStatus.getName(), objectMapper.writeValueAsString(newStatus));
            } catch (JsonProcessingException e) {
                LogUtil.error("Failed to serialize status to Redis", e);
            }
        }



//        if (isAlarmExist) {
//            LogUtil.warn("[ALARM] Raising alarm for: " + newStatus.getName() + " - " + problemCause);
//            faultManagementService.raiseAlarm(alarmMessage);
//
//            // Cập nhật Redis khi có alarm
//            try {
//
//                redisTemplate.opsForHash().put(REDIS_KEY, newStatus.getName(),
//                        objectMapper.writeValueAsString(newStatus));
//            } catch (JsonProcessingException e) {
//                LogUtil.error("Failed to serialize status to Redis", e);
//            }
//        } else {
//            LogUtil.info("[INFO] Clearing alarm for: " + newStatus.getName());
//            faultManagementService.clearAlarm(alarmMessage);
//
//
//            // Xóa khỏi Redis nếu không còn alarm
//            redisTemplate.opsForHash().delete(REDIS_KEY, newStatus.getName());
//        }
    }












    private String buildProblemCause(KafkaStatus status) {
        String state = status.getState();
        String trace = status.getTrace();

        return switch (state) {
            case "FAILED" ->
                    trace != null && !trace.isEmpty() ? "Failure reason: " + trace : "Connector/Task has failed unexpectedly.";
            case "UNASSIGNED" -> "Connector/Task is unassigned and has not yet been assigned to a worker.";
            case "PAUSED" -> "Connector/Task has been administratively paused.";
            default -> "Unknown issue detected in state: " + state;
        };
    }





}
