package com.vht.kafkamonitoring;

import com.vht.kafkamonitoring.service.KafkaMonitorService;
import com.vht.kafkamonitoring.service.RedisService;
import com.vht.kafkamonitoring.service.SystemMonitorService;
import com.vht.kafkamonitoring.util.AppUtil;
import com.vht.kafkamonitoring.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppScheduler {
    private final KafkaMonitorService kafkaMonitorService;
    private final RedisService redisService;
    private final AppUtil appUtil;
    private final SystemMonitorService systemMonitorService;
    public void runMonitoring() {
        log.info("Starting monitoring at: {}", DateUtil.formatDate(Instant.now().toEpochMilli()));

        // Check Redis connectivity
        redisService.pingRedis();

        // Monitor system health
        systemMonitorService.monitorSystemHealth();

        // Monitor Kafka status
        kafkaMonitorService.monitorKafkaStatus();

        log.info("Monitoring completed at: {}", DateUtil.formatDate(Instant.now().toEpochMilli()));
    }


}
