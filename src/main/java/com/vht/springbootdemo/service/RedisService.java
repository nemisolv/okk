package com.vht.springbootdemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vht.springbootdemo.dto.KafkaStatus;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {
    private static final String REDIS_KEY = "kafka-connect";
//    private static final String KAFKA_STATUS_PREFIX_KEY = "kafka_status:";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void saveKafkaStatus(KafkaStatus status) {
        String jsonStatus = null;
        String decodedKey = null;
        try {
            jsonStatus = objectMapper.writeValueAsString(status);
            decodedKey = URLDecoder.decode(status.getLocation(), StandardCharsets.UTF_8);
            redisTemplate.opsForHash().put(REDIS_KEY, decodedKey, jsonStatus);

        } catch (JsonProcessingException e) {
            log.error("Could not serialize KafkaStatus to JSON: {}", e.getMessage());
        }

    }


    // for clear dirty alarms
    public void saveKafkaStatus(KafkaStatus status, String key) {
        String jsonStatus = null;
        String decodedKey = null;
        try {
            jsonStatus = objectMapper.writeValueAsString(status);
            decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);
            redisTemplate.opsForHash().put(REDIS_KEY, decodedKey, jsonStatus);

        } catch (JsonProcessingException e) {
            log.error("Could not serialize KafkaStatus to JSON: {}", e.getMessage());
        }

    }


    public Map<String, KafkaStatus> fetchAllInstancesStatusFromRedis() {

        Map<Object, Object> redisData = redisTemplate.opsForHash().entries(REDIS_KEY);

        Map<String, KafkaStatus> statusMap = new HashMap<>();

        for (Map.Entry<Object, Object> entry : redisData.entrySet()) {
            try {
                KafkaStatus status = objectMapper.readValue(entry.getValue().toString(), KafkaStatus.class);
                statusMap.put(entry.getKey().toString(), status);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error deserializing KafkaStatus from Redis", e);
            }
        }
        return statusMap;
    }

    public void syncStatusToRedis(Map<String, KafkaStatus> newStatusMap) {
        redisTemplate.delete(REDIS_KEY);
        Map<String, String> redisData = new HashMap<>();
        for (Map.Entry<String, KafkaStatus> entry : newStatusMap.entrySet()) {
            try {
                redisData.put(entry.getKey(), objectMapper.writeValueAsString(entry.getValue()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error serializing KafkaStatus to JSON", e);
            }
        }
        redisTemplate.opsForHash().putAll(REDIS_KEY, redisData);
    }

    // 🔹 5. Xóa toàn bộ trạng thái Kafka Connect
    public void deleteAllStatuses() {
        redisTemplate.delete(REDIS_KEY);
        log.info("❌ Đã xóa toàn bộ trạng thái Kafka Connect.");
    }

    public void deleteKafkaStatus(String location) {
        String decodedKey = URLDecoder.decode(location, StandardCharsets.UTF_8);
        redisTemplate.opsForHash().delete(REDIS_KEY, decodedKey);
    }


    @PostConstruct
    public void pingRedis() {
        try {
            String response = Objects.requireNonNull(stringRedisTemplate.getConnectionFactory()).getConnection().ping();
            if ("PONG".equals(response)) {
                System.out.println("Redis is running");
            }
        } catch (Exception ex) {
            System.out.println("Redis is not running");
        }
    }
}
