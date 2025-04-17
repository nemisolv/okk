package com.vht.kafkamonitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThresholdConfig {
    private Integer cpuThreshold;
    private Integer ramThreshold;
    private List<String> kafkaInstances;
    private String cron;
}
