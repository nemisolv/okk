package com.vht.springbootdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AlarmMessage {
    private String ne;
    private String neIp;
    private String alarmId;
    private String internalService;
    private String location;
    private String probableCause;
    private Long initialTime;
    private Long triggerTime;
    private String additionInfo;
    private boolean isChanged;
    private String eventType;

    public static AlarmMessage newCopy(AlarmMessage alarmMessage) {
        return AlarmMessage.builder()
                .ne(alarmMessage.getNe())
                .neIp(alarmMessage.getNeIp())
                .alarmId(alarmMessage.getAlarmId())
                .internalService(alarmMessage.getInternalService())
                .location(alarmMessage.getLocation())
                .probableCause(alarmMessage.getProbableCause())
                .initialTime(alarmMessage.getInitialTime())
                .triggerTime(alarmMessage.getTriggerTime())
                .additionInfo(alarmMessage.getAdditionInfo())
                .isChanged(alarmMessage.isChanged())
                .eventType(alarmMessage.getEventType())
                .build();
    }


}

