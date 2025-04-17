package com.vht.kafkamonitoring.service;

import com.vht.kafkamonitoring.config.MonitoringConfigProperties;
import com.vht.kafkamonitoring.dto.AlarmMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaultManagementService {
    private final MonitoringConfigProperties defaultConfigFM;

    public int sendAlarm(AlarmMessage alarmMessage) {
        // Thiết lập thông tin mặc định cho alarm
        alarmMessage.setNe(defaultConfigFM.getFm().getNe());
        alarmMessage.setNeIp(defaultConfigFM.getFm().getNeIp());
        alarmMessage.setAlarmId(defaultConfigFM.getFm().getAlarmId());
        alarmMessage.setInternalService(defaultConfigFM.getFm().getInternalService());

        // Gửi alarm đến hệ thống quản lý lỗi
        log.info("Send alarm: {}", alarmMessage);
        return 200;
    }
}
