package com.vht.kafkamonitoring.service;

import com.vht.kafkamonitoring.Constants;
import com.vht.kafkamonitoring.config.MonitoringThresholdConfig;
import com.vht.kafkamonitoring.dto.*;
import com.vht.kafkamonitoring.util.LogUtil;
import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConfigService {
    private final RedisService redisService;
    //    private final QuartzConfig quartzConfig;
    private final Scheduler scheduler;
    private final FaultManagementService faultManagementService;
    private final KafkaMonitorService kafkaMonitorService;


    public void updateConfig(ConfigDTO configDTO) {
        ThresholdConfig fromRedis = redisService.get(MonitoringThresholdConfig.REDIS_KEY, ThresholdConfig.class);
        if (fromRedis != null) {
            if (configDTO.getCpuThreshold() != null) {
                fromRedis.setCpuThreshold(configDTO.getCpuThreshold());
            }
            if (configDTO.getRamThreshold() != null) {
                fromRedis.setRamThreshold(configDTO.getRamThreshold());
            }
            if (configDTO.getKafkaInstances() != null) {
                List<String> newInstances = configDTO.getKafkaInstances();
                List<String> staleKafkaInstanceList = getStaleKafkaInstance(newInstances, fromRedis.getKafkaInstances());

                // Xoá các instance cũ
                staleKafkaInstanceList.forEach(kafkaInstance -> {
                    LogUtil.info("Deleting stale Kafka instance: " + kafkaInstance);
                    // Xoá instance cũ, mọi thông tin về instance,connector, task sẽ bị xoá
                    long now = Instant.now().toEpochMilli();

                    Map<String, MonitoredStatus> staleMap = redisService.fetchSingleKafkaInstanceStateFromRedis(kafkaInstance);
                    List<AlarmMessage> batchSendAlarm = new ArrayList<>();
                    staleMap.forEach((key, stale) -> {
                        AlarmMessage alarmMessage = kafkaMonitorService.buildAlarmMessage(stale, AlarmType.CLEAR_ALARM.getCode(),stale.getInitialTime(), now, true );
                        batchSendAlarm.add(alarmMessage);
                    });
                    int code = faultManagementService.sendAlarm(batchSendAlarm);
                    if (code == 200) {
                        LogUtil.info("Alarm sent batched: " + batchSendAlarm);
                        LogUtil.info("Send alarm success, do clear stale instance and whole things of it. : " + kafkaInstance);
                        redisService.set(kafkaInstance, null);
                    }

                });

                fromRedis.setKafkaInstances(newInstances);


            }
            if (configDTO.getCron() != null) {
                String cronFromRedis = fromRedis.getCron();
                String cronFromConfig = configDTO.getCron();
                if (!cronFromRedis.equals(cronFromConfig)) {
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
            if (configDTO.getKafkaInstances() != null) {
                fromRedis.setKafkaInstances(configDTO.getKafkaInstances());
            }
        } else {
            fromRedis = new ThresholdConfig();
            fromRedis.setCpuThreshold(configDTO.getCpuThreshold());
            fromRedis.setRamThreshold(configDTO.getRamThreshold());
            fromRedis.setKafkaInstances(configDTO.getKafkaInstances());
            fromRedis.setCron(configDTO.getCron());
        }

        redisService.set(MonitoringThresholdConfig.REDIS_KEY, fromRedis);

    }

    private List<String> getStaleKafkaInstance(List<String> newInstances, List<String> oldInstances) {
        List<String> staleKafkaInstanceList = new ArrayList<>();
        for (String oldInstance : oldInstances) {
            if (!newInstances.contains(oldInstance)) {
                staleKafkaInstanceList.add(oldInstance);
                LogUtil.info("Stale Kafka instance: " + oldInstance);
            }
        }
        return staleKafkaInstanceList;

    }

}
