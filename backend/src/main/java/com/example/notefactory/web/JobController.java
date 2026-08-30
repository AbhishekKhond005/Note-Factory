package com.example.notefactory.web;

import com.example.notefactory.service.JobService;
import com.example.notefactory.web.dto.FileJobRequest;
import com.example.notefactory.web.dto.JobResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    /** List all generation jobs (frontend-friendly shape). */
    @GetMapping({"", "/"})
    public ResponseEntity<?> listJobs() {
        try {
            return ResponseEntity.ok(jobService.listJobViews());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /** Generate from a roadmap .txt file on disk (by filename), one task per chapter. */
    @PostMapping("/generate-from-file")
    public ResponseEntity<?> generateFromFile(@RequestBody FileJobRequest request) {
        try {
            List<Integer> chapterIndexes = request.getChapterIndexes();
            JobService.FileJobResult result = jobService.createJobFromFile(request.getFilename(), chapterIndexes);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("jobId", result.job().getId());
            if (result.warning() != null) {
                body.put("warning", result.warning());
            }
            return ResponseEntity.accepted().body(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Collections.singletonMap("error", "Failed to read roadmap file: " + e.getMessage()));
        }
    }

    /** Get a single job (frontend-friendly shape). */
    @GetMapping("/{id}")
    public ResponseEntity<?> getJobStatus(@PathVariable UUID id) {
        try {
            JobResponse view = jobService.getJobView(id);
            return ResponseEntity.ok(view);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelJob(@PathVariable UUID id) {
        try {
            jobService.cancelJob(id);
            return ResponseEntity.ok(Collections.singletonMap("message", "Cancelled"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
