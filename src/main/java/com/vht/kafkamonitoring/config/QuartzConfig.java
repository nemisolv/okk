package com.vht.kafkamonitoring.config;

import com.vht.kafkamonitoring.Constants;
import com.vht.kafkamonitoring.dto.ThresholdConfig;
import com.vht.kafkamonitoring.service.MonitoringJob;
import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import static org.quartz.TriggerBuilder.newTrigger;

@Configuration
@Component
@RequiredArgsConstructor
public class QuartzConfig {
    private final MonitoringThresholdConfig monitoringThresholdConfig;


    @Bean
    public JobDetail monitoringJobDetail() {
        return JobBuilder.newJob(MonitoringJob.class)
                .withIdentity(Constants.JOB_NAME)
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger monitoringJobTrigger() {
        ThresholdConfig config = monitoringThresholdConfig.getThresholdConfig();
        return buildTrigger(config != null ? config.getCron() : "0 */5 * * * ?");
    }

    private Trigger buildTrigger(String cronExpression) {
        return newTrigger()
                .forJob(Constants.JOB_NAME)
                .withIdentity(Constants.JOB_TRIGGER_NAME)
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();
    }


    public void updateTriggerSchedule(String newCronExpression) throws SchedulerException {


    }
}