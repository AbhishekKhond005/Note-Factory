package com.example.notefactory.web;

import com.example.notefactory.domain.GenerationJob;
import com.example.notefactory.service.JobService;
import com.example.notefactory.web.dto.JobRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<?> createJob(@RequestBody JobRequest request) {
        GenerationJob job = jobService.createJob(request.getType(), request.getRoadmapId(), request.getSubChapterIds());
        return ResponseEntity.accepted().body(Collections.singletonMap("jobId", job.getId()));
    }
    
    @PostMapping("/api/generate") // Legacy Next.js route support
    public ResponseEntity<?> startGenerationLegacy(@RequestBody JobRequest request) {
        GenerationJob job = jobService.createJob(request.getType(), request.getRoadmapId(), request.getSubChapterIds());
        return ResponseEntity.accepted().body(Collections.singletonMap("jobId", job.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getJobStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.getJob(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelJob(@PathVariable UUID id) {
        jobService.cancelJob(id);
        return ResponseEntity.ok(Collections.singletonMap("message", "Cancelled"));
    }
}
