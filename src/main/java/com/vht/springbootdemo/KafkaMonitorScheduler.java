package com.vht.springbootdemo;

import com.vht.springbootdemo.service.KafkaMonitorService;
import com.vht.springbootdemo.service.RedisService;
import com.vht.springbootdemo.service.SystemMonitorService;
import com.vht.springbootdemo.util.AppUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaMonitorScheduler {
    private final KafkaMonitorService kafkaMonitorService;
    private final RedisService redisService;
    private final AppUtil appUtil;
    private final SystemMonitorService systemMonitorService;
//    @Scheduled(fixedRate = 60000) // Chạy mỗi 60 giây
    @PostConstruct
    public void runMonitoringTask() throws IOException {

        systemMonitorService.startSystemMonitor();


        kafkaMonitorService.monitorKafkaStatus();

//        redisService.syncStatusToRedis(new HashMap<>());
    }



}
