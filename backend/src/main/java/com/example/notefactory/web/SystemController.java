package com.example.notefactory.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Collections;

@RestController
public class SystemController {

    @GetMapping("/api/status")
    public ResponseEntity<?> getStatus() {
        return ResponseEntity.ok(Collections.singletonMap("status", "running"));
    }

    @GetMapping("/api/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Collections.singletonMap("status", "ok"));
    }
}
