package com.vht.kafkamonitoring.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class AppInfo {
    @Value("${spring.application.name}")
    private String appName;
}
