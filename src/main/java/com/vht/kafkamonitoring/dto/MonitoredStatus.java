package com.vht.kafkamonitoring.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")

public class MonitoredStatus {
    private MonitoredType type;
    private String location;
    private MonitoredState state;       // RUNNING, FAILED, UNASSIGNED, PAUSED
    private String workerId;    // Địa chỉ worker_id
    private String probableCause;       // Lỗi trace từ Kafka
    private Long initialTime;
    private String additionalInfo;
}
