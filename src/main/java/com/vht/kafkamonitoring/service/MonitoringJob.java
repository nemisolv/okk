package com.vht.kafkamonitoring.service;

import com.vht.kafkamonitoring.AppScheduler;
import com.vht.kafkamonitoring.util.LogUtil;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonitoringJob implements Job {
    
    private final AppScheduler appScheduler;

    @Override
    public void execute(JobExecutionContext context) {
        LogUtil.info("📊 Starting monitoring job execution...");
        appScheduler.runMonitoring();
        LogUtil.info("✅ Monitoring job execution completed.");
    }
}