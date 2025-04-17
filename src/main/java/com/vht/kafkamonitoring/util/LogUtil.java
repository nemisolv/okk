package com.vht.kafkamonitoring.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogUtil {
    private static final Logger logger = LogManager.getLogger(LogUtil.class);

    public static void info(String message) {
        logger.info(message);
    }

    public static void warn(String message) {
        logger.warn(message);
    }

    public static void error(String message, Throwable ex) {
        logger.error(message, ex);
    }

    public static void error(Object ...message) {
        logger.error(message);
    }




    public static void debug(String message) {
        logger.debug(message);
    }
}
