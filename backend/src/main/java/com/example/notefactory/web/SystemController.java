package com.example.notefactory.web;

import com.example.notefactory.service.GenerationOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SystemController {

    private final GenerationOrchestrator orchestrator;

    @GetMapping("/api/status")
    public ResponseEntity<?> getStatus() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "running");
        body.put("activeJobs", orchestrator.getActiveTasks());
        body.put("maxParallel", orchestrator.getMaxConcurrency());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/api/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
