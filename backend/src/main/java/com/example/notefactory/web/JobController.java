package com.example.notefactory.web;

import com.example.notefactory.web.dto.JobRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.Collections;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @PostMapping
    public ResponseEntity<?> createJob(@RequestBody JobRequest request) {
        // TODO: Call service
        return ResponseEntity.accepted().body(Collections.singletonMap("jobId", UUID.randomUUID()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getJobStatus(@PathVariable UUID id) {
        // TODO: Call service
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelJob(@PathVariable UUID id) {
        // TODO: Call service
        return ResponseEntity.ok().build();
    }
}
