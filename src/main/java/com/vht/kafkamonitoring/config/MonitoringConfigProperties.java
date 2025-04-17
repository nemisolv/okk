package com.vht.kafkamonitoring.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Component
@ConfigurationProperties("monitoring")
@Validated // <== Kích hoạt cơ chế validation
@Getter
@Setter
public class MonitoringConfigProperties {

    @NotNull(message = "cpuThreshold must not be null")
    @Min(value = 1, message = "cpuThreshold must be greater than 0")
    private Integer cpuThreshold;

    @NotNull(message = "ramThreshold must not be null")
    @Min(value = 1, message = "ramThreshold must be greater than 0")
    private Integer ramThreshold;

    @NotEmpty(message = "kafkaInstances must not be empty")
    private List<@NotBlank(message = "Each kafka instance must not be blank") String> kafkaInstances;

    @NotBlank(message = "cron must not be blank")
    private String cron;

    @Valid
    private final Fm fm = new Fm();

    @Getter
    @Setter
    public static class Fm {
        @NotBlank(message = "fm.url must not be blank")
        private String url;

        @NotBlank(message = "fm.ne must not be blank")
        private String ne;

        @NotBlank(message = "fm.alarmId must not be blank")
        private String alarmId;

        @NotBlank(message = "fm.internalService must not be blank")
        private String internalService;

        @NotBlank(message = "fm.neIp must not be blank")
        private String neIp;
    }
}
