package com.vht.springbootdemo.api;

import com.vht.springbootdemo.config.AppProperties;
import com.vht.springbootdemo.dto.AlarmMessage;
import com.vht.springbootdemo.dto.AlarmType;
import com.vht.springbootdemo.service.FaultManagementService;
import com.vht.springbootdemo.util.AppUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaApiClient {
    private final RestTemplate restTemplate;
    private final AppProperties appProperties;
    private final AppUtil appUtil;
    private final FaultManagementService faultManagementService;

    public String fetchKafkaApiResponse(String url)  {

        String urlBuild = url + "/connectors?expand=status"; // assume call to Kafka API
        // assume reading JSON from File for testing purpose
     try {
         ResponseEntity<String> response = restTemplate.getForEntity(urlBuild, String.class);
         if(response.getStatusCode().is2xxSuccessful()) {
             return response.getBody();
         }
     }catch (RestClientException ex) {
        log.warn("Error when fetching Kafka API response from {}", urlBuild);
        // TODO: should raise alarm here
         Long eventTime = Instant.now().toEpochMilli();
         long initialTime = eventTime;
         long triggerTime = eventTime;
         boolean isChanged = false;
         boolean isAlarmRequired = true;
//         String hostUrl = appUtil.getApplicationUrl();
         AlarmMessage alarmMessage = AlarmMessage.builder()
                 .ne(appProperties.getFixedFields().getNe())
                 .alarmId(appProperties.getFixedFields().getAlarmId())
                 .internalService(appProperties.getFixedFields().getInternalService())
                 .neIp(appProperties.getFixedFields().getNeIp())
                 .eventType(isAlarmRequired ? AlarmType.RAISE_ALARM.getCode() : AlarmType.CLEAR_ALARM.getCode())
                 .location(url)
                 .initialTime(initialTime)
                 .triggerTime(triggerTime)
                 .additionInfo("Could not fetch Kafka API response URL: " + url)
                 .probableCause("Could not fetch Kafka API response URL: " + url)
                 .isChanged(isChanged)
                 .build();
         faultManagementService.sendAlarm(alarmMessage);

     }
     return null;
    }
}
