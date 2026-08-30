package com.example.notefactory.web;

import com.example.notefactory.domain.GenerationJob;
import com.example.notefactory.domain.Roadmap;
import com.example.notefactory.service.JobService;
import com.example.notefactory.service.RoadmapService;
import com.example.notefactory.web.dto.GenerateRequest;
import com.example.notefactory.web.dto.OverviewRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/generate")
@RequiredArgsConstructor
public class GenerationController {

    private final JobService jobService;
    private final RoadmapService roadmapService;

    /**
     * Starts a chapter-based generation job.
     * Accepts either an existing roadmapId, an uploaded roadmap file name, or
     * free-form roadmap text (parsed via the AI extraction agent on the fly).
     */
    @PostMapping
    public ResponseEntity<?> generate(@RequestBody GenerateRequest request) {
        try {
            // Case 1: roadmap file on disk -> parse via AI extraction, persist, queue job
            if (request.getRoadmapFile() != null && !request.getRoadmapFile().isBlank()) {
                JobService.FileJobResult result = jobService.createJobFromFile(request.getRoadmapFile(), request.getChapterIndexes());
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("jobId", result.job().getId());
                if (result.warning() != null) body.put("warning", result.warning());
                return ResponseEntity.accepted().body(body);
            }

            // Case 2: free-form roadmap text -> parse via AI extraction agent, persist
            UUID roadmapId = request.getRoadmapId();
            String warning = null;
            if (roadmapId == null && request.getRoadmapContent() != null && !request.getRoadmapContent().isBlank()) {
                Roadmap rm = roadmapService.uploadRoadmap(request.getRoadmapContent());
                roadmapId = rm.getId();
                warning = rm.getWarningMessage();
            }

            if (roadmapId == null) {
                return ResponseEntity.badRequest()
                        .body(Collections.singletonMap("error", "Provide an existing roadmapId, or roadmapFile/roadmapContent to create one"));
            }

            // Case 3: persisted roadmap -> queue a chapter-based job
            GenerationJob job = jobService.createJobForRoadmap(roadmapId, request.getChapterIndexes());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("jobId", job.getId());
            if (warning != null) body.put("warning", warning);
            return ResponseEntity.accepted().body(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/overview")
    public ResponseEntity<?> generateOverview(@RequestBody OverviewRequest request) {
        if (request.getTopic() == null || request.getTopic().isBlank()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Provide a topic"));
        }
        try {
            GenerationJob job = jobService.createOverviewJob(request.getTopic());
            return ResponseEntity.accepted().body(Collections.singletonMap("jobId", job.getId()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
