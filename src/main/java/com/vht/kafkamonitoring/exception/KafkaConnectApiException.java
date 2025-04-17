package com.vht.kafkamonitoring.exception;

public class KafkaConnectApiException extends RuntimeException {
    public KafkaConnectApiException(String message) {
        super(message);
    }
}
