package com.vht.springbootdemo.service;

import java.lang.management.ManagementFactory;

import com.sun.management.OperatingSystemMXBean;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.vht.springbootdemo.dto.AlarmMessage;
import com.vht.springbootdemo.dto.AlarmType;
import com.vht.springbootdemo.util.LogUtil;

@Service
@Slf4j
public class SystemMonitorService {
    private final FaultManagementService faultManagementService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final long RAM_THRESHOLD_MB = 200;
    private static final double CPU_THRESHOLD_PERCENT = 10.0;
    private boolean alarmRaised = false;

    public SystemMonitorService(FaultManagementService faultManagementService) {
        this.faultManagementService = faultManagementService;
//        startSystemMonitor();
    }

    public void startSystemMonitor() {
        scheduler.scheduleAtFixedRate(this::monitorSystemHealth, 0, 30, TimeUnit.SECONDS);
    }

    private void monitorSystemHealth() {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = osBean.getProcessCpuLoad(); // Tỷ lệ CPU mà process này dùng
        long allocatedMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        boolean isCpuOverloaded = cpuLoad > CPU_THRESHOLD_PERCENT;
        boolean isRamLow = allocatedMemory < RAM_THRESHOLD_MB;
        log.info("CPU: {}%, RAM: {}MB", cpuLoad, allocatedMemory);

        if (isCpuOverloaded || isRamLow) {
            if (!alarmRaised) {
                sendSystemAlarm(cpuLoad, allocatedMemory);
                alarmRaised = true;
            }
        } else {
            if (alarmRaised) {
                clearSystemAlarm(cpuLoad, allocatedMemory);
                alarmRaised = false;
            }
        }
    }

    private void sendSystemAlarm(double cpuLoad, long freeMemoryMB) {
        String issue = (cpuLoad > CPU_THRESHOLD_PERCENT ? "CPU quá tải: " + cpuLoad + "% " : "")
                + (freeMemoryMB < RAM_THRESHOLD_MB ? "RAM thấp: " + freeMemoryMB + "MB" : "");

        AlarmMessage alarmMessage = AlarmMessage.builder()
                .eventType(AlarmType.RAISE_ALARM.getCode())
                .location("SystemMonitorService")
                .initialTime(Instant.now().getEpochSecond())
                .triggerTime(Instant.now().getEpochSecond())
                .additionInfo(issue)
                .probableCause("Hệ thống quá tải")
                .isChanged(true)
                .build();

        faultManagementService.sendAlarm(alarmMessage);
        LogUtil.warn("[ALARM] " + issue);
    }

    private void clearSystemAlarm(double cpuLoad, long freeMemoryMB) {
        String issue = "CPU/RAM trở lại bình thường. CPU: " + cpuLoad + "%, RAM: " + freeMemoryMB + "MB";
        AlarmMessage alarmMessage = AlarmMessage.builder()
                .eventType(AlarmType.CLEAR_ALARM.getCode())
                .location("SystemMonitorService")
                .initialTime(Instant.now().getEpochSecond())
                .triggerTime(Instant.now().getEpochSecond())
                .additionInfo(issue)
                .probableCause("Hệ thống ổn định")
                .isChanged(true)
                .build();

        faultManagementService.sendAlarm(alarmMessage);
        LogUtil.info("[INFO] " + issue);
    }
}
