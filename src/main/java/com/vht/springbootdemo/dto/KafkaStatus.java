package com.vht.springbootdemo.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class KafkaStatus {
    private String name;        // Connector hoặc Task ID
    private String type;        // CONNECTOR hoặc TASK
    private String state;       // RUNNING, FAILED, UNASSIGNED, PAUSED
    private String workerId;    // Địa chỉ worker_id
    private String trace;       // Lỗi trace từ Kafka

    private Long initialTime;
}
