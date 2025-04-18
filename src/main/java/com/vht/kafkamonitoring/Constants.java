package com.vht.kafkamonitoring;

public class Constants {
    public static final String JOB_NAME = "MonitoringJob";
    public static final String JOB_TRIGGER_NAME = "monitoringTrigger";

    public static final String REGEX_CRON = "^[0-9*\\/,-]+ [0-9*\\/,-]+ [0-9*\\/,-]+ [0-9*\\/,-]+ [0-9*\\/,-]+ [?*\\/,-][0-9*\\/,-]*$";
}
