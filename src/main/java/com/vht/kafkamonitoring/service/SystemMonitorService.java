package com.vht.kafkamonitoring.service;

import java.time.Instant;

import com.vht.kafkamonitoring.config.AppInfo;
import com.vht.kafkamonitoring.config.MonitoringThresholdConfig;
import com.vht.kafkamonitoring.dto.*;
import com.vht.kafkamonitoring.util.DataUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SystemMonitorService {
    private final FaultManagementService faultManagementService;
    private final MonitoringThresholdConfig monitoringThresholdConfig;
    private final SystemResourceChecker systemResourceChecker;
    private final RedisService redisService;
    private final AppInfo appInfo;

    public void monitorSystemHealth() {
        SystemStatus current = systemResourceChecker.getCurrentResourceUsage();
        ThresholdConfig config = monitoringThresholdConfig.getThresholdConfig();

        boolean isCpuOverloaded = current.getCpuUsage() > config.getCpuThreshold();
        boolean isRamOverloaded = current.getRamUsage() > config.getRamThreshold();
        boolean isOverloaded = isCpuOverloaded || isRamOverloaded;

        MonitoredStatus previousState = redisService.get(RedisService.REDIS_KEY_SYSTEM_STATUS, MonitoredStatus.class);
        long now = Instant.now().toEpochMilli();
        long initialTime = previousState != null ? previousState.getInitialTime() : now;
        boolean isChanged = true; // CPU/RAM thay đổi liên tục

        String info = String.format("CPU/RAM: %.2f%%/%.2fMB", current.getCpuUsage(), current.getRamUsage());        String location = previousState != null ? previousState.getLocation() : buildLocation();

        log.info("Current system status: {}", info);
        if (isOverloaded) {
            AlarmMessage alarm = buildAlarmMessage(AlarmType.RAISE_ALARM, info + " is overloaded", info, location, initialTime, now, isChanged);
            if (sendAlarm(alarm)) {
                redisService.set(RedisService.REDIS_KEY_SYSTEM_STATUS,
                        buildMonitoredStatus(location, initialTime, info + " is overloaded")
                );
            }
        } else if (previousState != null && previousState.getState() == MonitoredState.OVERLOADED) {
            AlarmMessage alarm = buildAlarmMessage(AlarmType.CLEAR_ALARM, info + " back to normal", "Clear alarm: " + info + " back to normal", location, initialTime, now, isChanged);
            if (sendAlarm(alarm)) {
                redisService.set(RedisService.REDIS_KEY_SYSTEM_STATUS, null);
            }
        }
    }

    private AlarmMessage buildAlarmMessage(AlarmType type, String probableCause, String additionalInfo,
                                           String location, long initialTime, long triggerTime, boolean isChanged) {
        return AlarmMessage.builder()
                .eventType(type.getCode())
                .location(location)
                .initialTime(initialTime)
                .triggerTime(triggerTime)
                .additionInfo(additionalInfo)
                .probableCause(probableCause)
                .isChanged(isChanged)
                .build();
    }

    private MonitoredStatus buildMonitoredStatus(String location, long initialTime, String probableCause) {
        MonitoredStatus status = new MonitoredStatus();
        status.setType(MonitoredType.SYSTEM_RESOURCE);
        status.setLocation(location);
        status.setState(MonitoredState.OVERLOADED);
        status.setInitialTime(initialTime);
        status.setAdditionalInfo(probableCause);
        status.setProbableCause(probableCause);
        return status;
    }

    private boolean sendAlarm(AlarmMessage alarmMessage) {
        int code = faultManagementService.sendAlarm(alarmMessage);
        if (code != 200) {
            log.warn("Could not send alarm to FM, AlarmMessage:: {}", alarmMessage);
            return false;
        }
        return true;
    }

    private String buildLocation() {
        return DataUtil.isNullOrEmpty(appInfo.getAppName()) ? "SystemMonitorService" : appInfo.getAppName();
    }
}
