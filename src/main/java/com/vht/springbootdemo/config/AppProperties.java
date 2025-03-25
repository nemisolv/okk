package com.vht.springbootdemo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

@Component
@ConfigurationProperties("app.kafka-connect")
@Getter
@Setter
public class AppProperties {
    private final Instance instance = new Instance();
    private final FixedFields fixedFields = new FixedFields();

    @Getter
    @Setter
    public static class Instance {
        private List<String> url;
    }
    @Getter
    @Setter
    public static class FixedFields {
        private String ne;
        private String neIp;
        private String internalService;
        private String alarmId;
    }

}
