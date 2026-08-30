package com.example.notefactory.web;

import com.example.notefactory.domain.Roadmap;
import com.example.notefactory.service.JobService;
import com.example.notefactory.service.RoadmapService;
import com.example.notefactory.web.dto.RoadmapRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/roadmaps")
@RequiredArgsConstructor
public class RoadmapController {

    private final RoadmapService roadmapService;
    private final JobService jobService;

    /** List roadmap .txt files available on disk. */
    @GetMapping("/files")
    public ResponseEntity<?> listRoadmapFiles() {
        try {
            return ResponseEntity.ok(jobService.listRoadmapFiles());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /** Generate a roadmap from a topic via prompt 0 (hard-coded planner). */
    @PostMapping("/generate")
    public ResponseEntity<?> generateRoadmap(@RequestBody RoadmapRequest request) {
        if (request.getTopic() == null || request.getTopic().isBlank()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Provide a topic"));
        }
        try {
            Roadmap rm = roadmapService.generateRoadmap(request.getTopic(), request.getRawText());
            return ResponseEntity.ok(rm);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /** Upload a roadmap file (multipart). */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadRoadmap(@RequestParam("roadmap") MultipartFile file) {
        try {
            String content = new String(file.getBytes());
            Roadmap rm = roadmapService.uploadRoadmap(content);
            return ResponseEntity.ok(rm);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Failed to read file"));
        }
    }

    /** Parse free-form roadmap text via the AI extraction agent. */
    @PostMapping("/parse")
    public ResponseEntity<?> parseRoadmap(@RequestBody RoadmapRequest request) {
        try {
            Roadmap rm = roadmapService.uploadRoadmap(request.getRawText());
            return ResponseEntity.ok(rm);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRoadmap(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(roadmapService.getRoadmap(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Roadmap>> listRoadmaps() {
        return ResponseEntity.ok(roadmapService.getAllRoadmaps());
    }
}
