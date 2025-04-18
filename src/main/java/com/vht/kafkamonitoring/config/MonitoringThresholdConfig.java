package com.vht.kafkamonitoring.config;

import com.vht.kafkamonitoring.dto.ThresholdConfig;
import com.vht.kafkamonitoring.service.RedisService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class MonitoringThresholdConfig {

    RedisService redisService;
    MonitoringConfigProperties fileConfig;

    public static final String REDIS_KEY = "CONFIG:MONITORING";

    public ThresholdConfig getThresholdConfig() {
        ThresholdConfig fromRedis = redisService.get(REDIS_KEY, ThresholdConfig.class);
        if (fromRedis != null) {
            log.info("Get threshold config from Redis: {}" , fromRedis);
            return fromRedis;
        }

        log.warn("Warning: Config not found in Redis, using default config");
        ThresholdConfig defaultConfig = ThresholdConfig.builder()
                .cpuThreshold(fileConfig.getCpuThreshold())
                .ramThreshold(fileConfig.getRamThreshold())
                .kafkaInstances(fileConfig.getKafkaInstances())
                .cron(fileConfig.getCron())
                .build();

        redisService.set(REDIS_KEY, defaultConfig, ThresholdConfig.class);

        log.info("Set default config to Redis: {}" , defaultConfig);

        return defaultConfig;
    }
}
