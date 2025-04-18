package com.vht.kafkamonitoring.api;

import com.vht.kafkamonitoring.config.MonitoringConfigProperties;
import com.vht.kafkamonitoring.dto.AlarmMessage;
import com.vht.kafkamonitoring.dto.AlarmType;
import com.vht.kafkamonitoring.exception.KafkaConnectApiException;
import com.vht.kafkamonitoring.service.FaultManagementService;
import com.vht.kafkamonitoring.util.AppUtil;
import com.vht.kafkamonitoring.util.LogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaApiClient {
    private final RestTemplate restTemplate;
    private final MonitoringConfigProperties monitoringConfigProperties;
    private final FaultManagementService faultManagementService;

    public String fetchKafkaApiResponse(String url)  {

        String urlBuild = url + "/connectors?expand=status"; // assume call to Kafka API
        // assume reading JSON from File for testing purpose
     try {
         ResponseEntity<String> response = restTemplate.getForEntity(urlBuild, String.class);
         if(response.getStatusCode().is2xxSuccessful()) {
             return response.getBody();
         }
         return testReadFromFile();

     }catch (RestClientException ex) {
//        LogUtil.warn("Error when fetching Kafka API response from " + urlBuild);
//        // TODO: should raise alarm here
//         long eventTime = Instant.now().toEpochMilli();
//         long initialTime = eventTime;
//         long triggerTime = eventTime;
//         boolean isChanged = false;
//         boolean isAlarmRequired = true;
////         String hostUrl = appUtil.getApplicationUrl();
//         AlarmMessage alarmMessage = AlarmMessage.builder()
//
//                 .eventType(isAlarmRequired ? AlarmType.RAISE_ALARM.getCode() : AlarmType.CLEAR_ALARM.getCode())
//                 .location(url)
//                 .initialTime(initialTime)
//                 .triggerTime(triggerTime)
//                 .additionInfo("Could not fetch Kafka API response URL: " + url)
//                 .probableCause("Could not fetch Kafka API response URL: " + url)
//                 .isChanged(isChanged)
//                 .build();
//         faultManagementService.sendAlarm(alarmMessage);
        throw new KafkaConnectApiException("Could Call to Kafka Connect for instance: " + url);
     }
//     return null;
    }

    private String testReadFromFile() {
        String filePath = "src/main/resources/kafka-api-response.json";
        try {
            return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
        } catch (java.io.IOException e) {
            log.error("Error reading file: " + e.getMessage());
            return null;
        }
    }


}
