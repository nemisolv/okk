package com.vht.springbootdemo;

import com.vht.springbootdemo.service.KafkaMonitorService;
import com.vht.springbootdemo.service.RedisService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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
public class KafkaMonitorScheduler {
    private final KafkaMonitorService kafkaMonitorService;
    private final RedisService redisService;

//    @Scheduled(fixedRate = 60000) // Chạy mỗi 60 giây
    @PostConstruct
    public void runMonitoringTask() throws IOException {
        String kafkaApiResponse = fetchKafkaApiResponse();
        kafkaMonitorService.monitorKafkaStatus(kafkaApiResponse);

//        redisService.syncStatusToRedis(new HashMap<>());
    }


    private String fetchKafkaApiResponse() throws IOException {
        // assume reading JSON from File for testing purpose
        String filePath = "src/main/resources/kafka-api-response.json";
        Path path = Paths.get(filePath);
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes);
    }
}
