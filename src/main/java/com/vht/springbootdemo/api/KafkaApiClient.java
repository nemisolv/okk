package com.vht.springbootdemo.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vht.springbootdemo.dto.KafkaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KafkaApiClient {
    private final ObjectMapper objectMapper;

    public Map<String, KafkaStatus> fetchKafkaStatus(String kafkaApiResponse) {
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
                String trace = statusNode.get("connector").get("trace") != null ? statusNode.get("connector").get("trace").asText() : "";


                // Thêm trạng thái của Connector
                kafkaStatusMap.put(connectorName,
                        KafkaStatus.builder()
                                .name(connectorName)
                                .type("CONNECTOR")
                                .state(connectorState)
                                .workerId(workerId)
                                .trace(trace)
                                .build());

                // Thêm trạng thái của từng Task
                for (JsonNode task : statusNode.get("tasks")) {
                    int taskId = task.get("id").asInt();
                    String taskState = task.get("state").asText();
                    String taskWorkerId = task.get("worker_id").asText();
                    String taskTrace = task.get("trace") != null ? task.get("trace").asText() : "";

                    String taskKey = connectorName + ":" + taskId; // Tạo key riêng cho Task
                    kafkaStatusMap.put(taskKey,
                            KafkaStatus.builder()
                                    .name(taskKey)
                                    .type("TASK")
                                    .state(taskState)
                                    .workerId(taskWorkerId)
                                    .trace(taskTrace)
                                    .build());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing Kafka API response", e);
        }

        return kafkaStatusMap;
    }
}
