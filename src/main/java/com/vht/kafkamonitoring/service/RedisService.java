package com.vht.kafkamonitoring.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vht.kafkamonitoring.dto.MonitoredStatus;
import com.vht.kafkamonitoring.util.LogUtil;
import com.vht.kafkamonitoring.util.UrlUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

//    private static final String REDIS_KEY = "kafka-connect";
    public static  final String REDIS_KEY_SYSTEM_STATUS = "system-status";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;





    // for clear dirty alarms
    public void putKafkaStateIntoSpecificInstance(MonitoredStatus status, String key) {
        // Lấy instance URL từ key
        try {
        String instanceUrl = UrlUtil.extractInstanceUrl(key);
            if(status == null) {
                redisTemplate.opsForHash().delete(instanceUrl, key);
                log.info("Deleted status for key: {}", key);
                return;
            }
            String jsonStatus = objectMapper.writeValueAsString(status);
            redisTemplate.opsForHash().put(instanceUrl, key, jsonStatus);

        }
        catch (IllegalArgumentException e) {
            log.error("instanceUrl is not valid: {}", e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("Could not serialize KafkaStatus to JSON: {}", e.getMessage());
        }

    }

    //get


    public MonitoredStatus getElementInAKafkaSpecificInstance(String key) throws JsonProcessingException {
        String instanceUrl = UrlUtil.extractInstanceUrl(key);

        Object o = redisTemplate.opsForHash().get(instanceUrl, key);
        if(o == null) {
            return null;
        }
        MonitoredStatus monitoredStatus = objectMapper.readValue(o.toString(), MonitoredStatus.class);
        return monitoredStatus;
    }



    public Map<String, MonitoredStatus> fetchSingleKafkaInstanceStateFromRedis(String instanceUrl) {

        Map<Object, Object> redisData = redisTemplate.opsForHash().entries(instanceUrl);

        Map<String, MonitoredStatus> statusMap = new HashMap<>();

        for (Map.Entry<Object, Object> entry : redisData.entrySet()) {
            try {
                MonitoredStatus status = objectMapper.readValue(entry.getValue().toString(), MonitoredStatus.class);
                statusMap.put(entry.getKey().toString(), status);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error deserializing KafkaStatus from Redis", e);
            }
        }
        return statusMap;
    }

//    public void syncStatusToRedis(Map<String, MonitoredStatus> newStatusMap) {
//        redisTemplate.delete(REDIS_KEY);
//        Map<String, String> redisData = new HashMap<>();
//        for (Map.Entry<String, MonitoredStatus> entry : newStatusMap.entrySet()) {
//            try {
//                redisData.put(entry.getKey(), objectMapper.writeValueAsString(entry.getValue()));
//            } catch (JsonProcessingException e) {
//                throw new RuntimeException("Error serializing KafkaStatus to JSON", e);
//            }
//        }
//        redisTemplate.opsForHash().putAll(REDIS_KEY, redisData);
//    }

    // 🔹 5. Xóa toàn bộ trạng thái Kafka Connect
//    public void deleteAllStatuses() {
//        redisTemplate.delete(REDIS_KEY);
//        log.info("❌ Đã xóa toàn bộ trạng thái Kafka Connect.");
//    }

//    public void deleteKafkaStatus(String location) {
//        String decodedKey = URLDecoder.decode(location, StandardCharsets.UTF_8);
//        redisTemplate.opsForHash().delete(REDIS_KEY, decodedKey);
//    }

















    // Lưu đối tượng cấu hình vào Redis, nhận kiểu đối tượng từ Class<T>
    public <T> void set(String key, T value) {
        try {

            if(value == null) {
                redisTemplate.delete(key);
            }

            String valueAsJson = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set( key, valueAsJson);
            log.info("Cache set for key: {}", key);
        } catch (Exception e) {
            log.error("Error setting cache for key: {}", key, e);
        }
    }

    // Lấy đối tượng từ Redis theo key và kiểu Class<T>
    public <T> T get(String key, Class<T> valueType) {
        try {
            String valueAsJson = (String) redisTemplate.opsForValue().get(key);
            if (valueAsJson != null) {
                return objectMapper.readValue(valueAsJson, valueType);
            }
            return null;
        } catch (Exception e) {
            log.error("Error getting cache for key: {}", key, e);
            return null;
        }
    }






















    public void pingRedis() {
        try {
            String response = Objects.requireNonNull(stringRedisTemplate.getConnectionFactory()).getConnection().ping();
            if ("PONG".equals(response)) {
                LogUtil.info("Redis is running");
            }
        } catch (Exception ex) {
            LogUtil.error("Redis is not reachable: ", ex.getMessage());
            throw new RuntimeException("Redis is not reachable", ex);
        }
    }




}
