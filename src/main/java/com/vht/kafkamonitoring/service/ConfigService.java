package com.vht.kafkamonitoring.service;

import com.vht.kafkamonitoring.Constants;
import com.vht.kafkamonitoring.config.MonitoringThresholdConfig;
import com.vht.kafkamonitoring.config.QuartzConfig;
import com.vht.kafkamonitoring.dto.ConfigDTO;
import com.vht.kafkamonitoring.dto.ThresholdConfig;
import com.vht.kafkamonitoring.util.LogUtil;
import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigService {
    private final RedisService redisService;
//    private final QuartzConfig quartzConfig;
    private final Scheduler scheduler;




    public void updateConfig(ConfigDTO configDTO) {
        ThresholdConfig fromRedis = redisService.get(MonitoringThresholdConfig.REDIS_KEY, ThresholdConfig.class);
        if(fromRedis != null) {
            if(configDTO.getCpuThreshold() != null) {
                fromRedis.setCpuThreshold(configDTO.getCpuThreshold());
            }
            if(configDTO.getRamThreshold() != null) {
                fromRedis.setRamThreshold(configDTO.getRamThreshold());
            }
            if(configDTO.getKafkaInstances() != null) {
                fromRedis.setKafkaInstances(configDTO.getKafkaInstances());
            }
            if(configDTO.getCron() != null) {
                String cronFromRedis = fromRedis.getCron();
                String cronFromConfig = configDTO.getCron();
                if(!cronFromRedis.equals(cronFromConfig)) {
                    try {
//                        quartzConfig.updateTriggerSchedule(cronFromConfig);

                        JobKey jobKey = new JobKey(Constants.JOB_NAME);
                        TriggerKey triggerKey = new TriggerKey(Constants.JOB_TRIGGER_NAME);

                        // Nếu job cũ tồn tại thì xoá
                        if (scheduler.checkExists(jobKey)) {
                            LogUtil.info("Delete job: " + jobKey);
                            scheduler.deleteJob(jobKey);
                        }

                        JobDetail jobDetail = JobBuilder.newJob(MonitoringJob.class)
                                .withIdentity(jobKey)
                                .storeDurably()
                                .build();

                        CronTrigger trigger = TriggerBuilder.newTrigger()
                                .withIdentity(triggerKey)
                                .forJob(jobDetail) // Truyền JobDetail vào đây
                                .withSchedule(CronScheduleBuilder.cronSchedule(cronFromConfig))
                                .build();

                        scheduler.scheduleJob(jobDetail, trigger);

                        LogUtil.info("✅ Monitoring job rescheduled with new cron: " + cronFromConfig);
                    } catch (SchedulerException e) {
                        LogUtil.error("❌ Failed to reschedule monitoring job: " + e.getMessage());
                        throw new RuntimeException("Failed to reschedule job", e);
                    }

                }

                fromRedis.setCron(configDTO.getCron());
            }
            if(configDTO.getKafkaInstances() != null) {
                fromRedis.setKafkaInstances(configDTO.getKafkaInstances());
            }
        }else {
            fromRedis = new ThresholdConfig();
            fromRedis.setCpuThreshold(configDTO.getCpuThreshold());
            fromRedis.setRamThreshold(configDTO.getRamThreshold());
            fromRedis.setKafkaInstances(configDTO.getKafkaInstances());
            fromRedis.setCron(configDTO.getCron());
        }

        redisService.set(MonitoringThresholdConfig.REDIS_KEY, fromRedis, ThresholdConfig.class);

    }

}
