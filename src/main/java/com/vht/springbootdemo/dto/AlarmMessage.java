package com.vht.springbootdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlarmMessage {
    private String name;
    private String state;
    private String neIp;
    private String problemCause;
    private boolean isChanged;
    private boolean isAlarmExist;
    private Long initialTime;
    private Long triggerTime;

    public void updateTriggerTime() {
        this.triggerTime = Instant.now().getEpochSecond();
    }
}

