package com.example.notefactory.web;

import com.example.notefactory.domain.Roadmap;
import com.example.notefactory.web.dto.RoadmapRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.List;
import java.util.Collections;

@RestController
@RequestMapping("/api/roadmaps")
public class RoadmapController {

    @PostMapping("/generate")
    public ResponseEntity<?> generateRoadmap(@RequestBody RoadmapRequest request) {
        // TODO: Call service
        return ResponseEntity.accepted().body(Collections.singletonMap("message", "Accepted"));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadRoadmap(@RequestBody RoadmapRequest request) {
        // TODO: Call service
        return ResponseEntity.ok(Collections.singletonMap("id", UUID.randomUUID()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRoadmap(@PathVariable UUID id) {
        // TODO: Call service
        return ResponseEntity.ok().build();
    }
    
    @GetMapping
    public ResponseEntity<List<Roadmap>> listRoadmaps() {
        return ResponseEntity.ok(Collections.emptyList());
    }
}
