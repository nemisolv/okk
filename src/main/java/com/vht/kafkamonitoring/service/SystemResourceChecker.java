package com.vht.kafkamonitoring.service;

import com.sun.management.OperatingSystemMXBean;
import com.vht.kafkamonitoring.dto.SystemStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class SystemResourceChecker {
    public SystemStatus getCurrentResourceUsage() {
        double currentCpu = getCpuUsage();
        double currentRam = getRamUsage();

        return SystemStatus.builder()
                .cpuUsage(currentCpu)
                .ramUsage(currentRam)
                .build();
    }

    private double getCpuUsage() {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double processCpuLoad = osBean.getProcessCpuLoad();
        processCpuLoad = processCpuLoad < 0 ? 0 : processCpuLoad;
        double processCpuLoadPercent = processCpuLoad * 100;
        return processCpuLoadPercent;
    }

    private double getRamUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long usedMemoryMB = usedMemory / (1024 * 1024);
        return usedMemoryMB;
    }
}
