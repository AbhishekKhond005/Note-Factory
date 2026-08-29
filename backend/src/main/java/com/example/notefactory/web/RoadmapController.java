package com.example.notefactory.web;

import com.example.notefactory.domain.Roadmap;
import com.example.notefactory.service.RoadmapService;
import com.example.notefactory.web.dto.RoadmapRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.Collections;

@RestController
@RequestMapping("/api/roadmaps")
@RequiredArgsConstructor
public class RoadmapController {

    private final RoadmapService roadmapService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateRoadmap(@RequestBody RoadmapRequest request) {
        try {
            Roadmap rm = roadmapService.generateRoadmap(request.getTopic(), request.getRawText());
            return ResponseEntity.ok(rm);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadRoadmap(@RequestParam("roadmap") MultipartFile file) {
        try {
            String content = new String(file.getBytes());
            Roadmap rm = roadmapService.uploadRoadmap(content);
            return ResponseEntity.ok(Collections.singletonMap("id", rm.getId()));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Failed to read file"));
        }
    }

    @PostMapping("/parse")
    public ResponseEntity<?> parseRoadmap(@RequestBody RoadmapRequest request) {
        try {
            Roadmap rm = roadmapService.uploadRoadmap(request.getRawText());
            return ResponseEntity.ok(rm);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRoadmap(@PathVariable String id) {
        try {
            // Support searching by name or UUID
            try {
                UUID uuid = UUID.fromString(id);
                return ResponseEntity.ok(roadmapService.getRoadmap(uuid));
            } catch (IllegalArgumentException e) {
                // If it's a filename, we can try to find it by name, but for now just return error
                // In a real implementation we'd search by title
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Only UUID supported currently"));
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<Roadmap>> listRoadmaps() {
        return ResponseEntity.ok(roadmapService.getAllRoadmaps());
    }
}
