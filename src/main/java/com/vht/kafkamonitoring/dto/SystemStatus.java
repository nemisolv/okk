package com.vht.kafkamonitoring.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemStatus {
    private Double cpuUsage;
    private Double ramUsage;
}
