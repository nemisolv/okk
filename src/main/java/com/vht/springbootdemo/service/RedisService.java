package com.vht.springbootdemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vht.springbootdemo.dto.KafkaStatus;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RedisService {
    private static final String REDIS_KEY = "kafka:status";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void pingRedis() {
        try {
            String response = Objects.requireNonNull(stringRedisTemplate.getConnectionFactory()).getConnection().ping();
            if("PONG".equals(response)) {
                System.out.println("Redis is running");
            }
        } catch (Exception ex) {
            System.out.println("Redis is not running");
        }
    }


    public  Map<String, KafkaStatus> fetchStatusFromRedis() {
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
}
