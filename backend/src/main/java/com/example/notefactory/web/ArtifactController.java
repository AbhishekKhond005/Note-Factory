package com.example.notefactory.web;

import com.example.notefactory.domain.GenerationJob;
import com.example.notefactory.domain.GenerationTask;
import com.example.notefactory.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class ArtifactController {
    
    private final JobService jobService;

    @GetMapping("/{jobId}")
    public ResponseEntity<?> getNotes(@PathVariable UUID jobId) {
        // Return notes info for a given job.
        // For now, we'll just return the tasks of the job, which contain the status
        GenerationJob job = jobService.getJob(jobId);
        List<GenerationTask> tasks = job.getTasks();
        
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{jobId}/download")
    public ResponseEntity<byte[]> downloadArtifact(@PathVariable UUID jobId) {
        return ResponseEntity.ok(new byte[0]); // Stub
    }

    @GetMapping("/{jobId}/download-all")
    public ResponseEntity<byte[]> downloadAllArtifacts(@PathVariable UUID jobId) {
        return ResponseEntity.ok(new byte[0]); // Stub
    }
}
