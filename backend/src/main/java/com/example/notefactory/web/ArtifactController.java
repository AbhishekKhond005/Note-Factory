package com.example.notefactory.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/artifacts")
public class ArtifactController {

    @GetMapping("/{id}/content")
    public ResponseEntity<String> getArtifactContent(@PathVariable UUID id) {
        // TODO: Call service
        return ResponseEntity.ok("TODO content");
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadArtifact(@PathVariable UUID id) {
        // TODO: Call service
        return ResponseEntity.ok(new byte[0]);
    }
}
