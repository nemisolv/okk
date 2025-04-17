package com.vht.kafkamonitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AlarmType {
    CLEAR_ALARM("0"),
    RAISE_ALARM("1");


    private String code;

    public void setCode(String code) {
        this.code = code;
    }

    public static AlarmType fromValue(String value) {
        for (AlarmType alarmType : AlarmType.values()) {
            if (alarmType.code.equals(value)) {
                return alarmType;
            }
        }
        return null;
    }

}
