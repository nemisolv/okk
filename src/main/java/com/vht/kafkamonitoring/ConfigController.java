package com.vht.kafkamonitoring;

import com.vht.kafkamonitoring.dto.ConfigDTO;
import com.vht.kafkamonitoring.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/config")
public class ConfigController {
    private final ConfigService configService;

    @PutMapping("/update")
    public ResponseEntity<?> updateConfig(@RequestBody  ConfigDTO configDTO) {
        configService.updateConfig(configDTO);
        return ResponseEntity.ok("Config updated successfully");
    }
}
