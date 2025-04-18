package com.vht.kafkamonitoring.dto;

import com.vht.kafkamonitoring.Constants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.lang.invoke.ConstantCallSite;
import java.util.List;

@Data
public class ConfigDTO {
    @Min(value = 1, message = "CPU threshold must be at least 1")
    @Max(value = 100, message = "CPU threshold must not exceed 100")
    private Integer cpuThreshold;
    @Min(value = 1, message = "RAM threshold must be at least 1")
    private Integer ramThreshold;
    private List< @Pattern(regexp = "^(http|https)://.*", message = "Kafka instance URL must start with http:// or https://")
             String> kafkaInstances;
    @Pattern(
            regexp = Constants.REGEX_CRON,
            message = "Invalid Quartz cron expression format. Must contain 6 or 7 fields (seconds, minutes, hours, day of month, month, day of week, [year])"
    )
    private String cron;
}

