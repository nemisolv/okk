package com.vht.springbootdemo.service;

import com.vht.springbootdemo.dto.AlarmMessage;
import com.vht.springbootdemo.dto.KafkaStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FaultManagementService {
    public int sendAlarm(AlarmMessage alarmMessage) {
        // Gửi alarm đến hệ thống quản lý lỗi
        log.info("Send alarm: {}", alarmMessage);
        return 200;
    }
}
