package com.vht.springbootdemo.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vht.springbootdemo.dto.KafkaStatus;
import com.vht.springbootdemo.util.TraceExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KafkaConnectResponseDeserializer {
    private final KafkaApiClient kafkaApiClient;
    private final ObjectMapper objectMapper;

    public Map<String, KafkaStatus> deserializeKafkaStatusApiResponse(String instanceUrl) {
        
        String kafkaApiResponse = kafkaApiClient.fetchKafkaApiResponse(instanceUrl);
        if(kafkaApiResponse == null) {
            return new HashMap<>();
        }
        Map<String, KafkaStatus> kafkaStatusMap = new HashMap<>();

        try {
            JsonNode rootNode = objectMapper.readTree(kafkaApiResponse);
            Iterator<Map.Entry<String, JsonNode>> connectors = rootNode.fields();

            while (connectors.hasNext()) {
                Map.Entry<String, JsonNode> connectorEntry = connectors.next();
                JsonNode statusNode = connectorEntry.getValue().get("status");

                String connectorName = statusNode.get("name").asText();
                String connectorState = statusNode.get("connector").get("state").asText();
                String workerId = statusNode.get("connector").get("worker_id").asText();
                String rawTrace = statusNode.get("connector").get("trace")!= null ? statusNode.get("connector").get("trace").asText(): "" ;
                String traceExtracted = TraceExtractor.buildTrace(rawTrace, connectorState);

                String additionalInfo = instanceUrl +"|" + connectorName +" is " + connectorState + " at worker id" + workerId;


               String connectorLocation =  buildLocation(instanceUrl, connectorName);
                // Thêm trạng thái của Connector
                kafkaStatusMap.put(connectorLocation,
                        KafkaStatus.builder()
                                .location( connectorLocation )
                                .type("CONNECTOR")
                                .state(connectorState)
                                .workerId(workerId)
                                .trace(traceExtracted)
                                .addtionalInfo(additionalInfo)
                                .build());

                // Thêm trạng thái của từng Task
                for (JsonNode task : statusNode.get("tasks")) {
                    int taskId = task.get("id").asInt();
                    String taskState = task.get("state").asText();
                    String taskWorkerId = task.get("worker_id").asText();
                    String taskTraceRaw = task.get("trace") != null ?task.get("trace").asText() : "";
                    String taskTraceExtracted = TraceExtractor.buildTrace(taskTraceRaw, taskState);
                    String additionalInfoTask = instanceUrl +"|" + connectorName +"-task-" + taskId + " is " + taskState + " at worker id " + taskWorkerId;

                    String taskKey = connectorName + "/" + taskId; // Tạo key riêng cho Task
                    String taskLocation = buildLocation(instanceUrl,"/"+ taskKey);
                    kafkaStatusMap.put(taskLocation,
                            KafkaStatus.builder()
                                    .location( taskLocation)
                                    .type("TASK")
                                    .state(taskState)
                                    .workerId(taskWorkerId)
                                    .trace(taskTraceExtracted)
                                    .addtionalInfo(additionalInfoTask)
                                    .build());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing Kafka API response", e);
        }

        return kafkaStatusMap;
    }

    private String buildLocation(String instanceUrl, String subLocation) {
        return instanceUrl + "/" + subLocation;
    }


}
