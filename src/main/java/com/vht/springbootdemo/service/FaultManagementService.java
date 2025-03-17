package com.vht.springbootdemo.service;

import com.vht.springbootdemo.dto.AlarmMessage;
import com.vht.springbootdemo.dto.KafkaStatus;
import org.springframework.stereotype.Service;

@Service
public class FaultManagementService {
    public void raiseAlarm(AlarmMessage msg) {
        System.out.println("[ALARM] Raising alarm: " + msg);
        // Gửi HTTP request hoặc sự kiện đến FM
    }

    public void clearAlarm(AlarmMessage msg) {
        System.out.println("[ALARM] Clearing alarm: " + msg);
        // Gửi HTTP request hoặc sự kiện đến FM
    }
}
