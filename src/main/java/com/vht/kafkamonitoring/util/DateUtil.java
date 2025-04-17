package com.vht.kafkamonitoring.util;

import java.time.format.DateTimeFormatter;

public class DateUtil {
    private static  final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String formatDate(long timestamp) {
        return DATE_TIME_FORMATTER.format(java.time.Instant.ofEpochMilli(timestamp));
    }
}
