package com.example.notefactory.web;

import com.example.notefactory.service.NotesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class ArtifactController {

    private final NotesService notesService;

    /** Returns the generated notes for a job: { merged, notes: [{name, content}] }. */
    @GetMapping("/{jobId}")
    public ResponseEntity<?> getNotes(@PathVariable UUID jobId) {
        try {
            return ResponseEntity.ok(notesService.getNotes(jobId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** Download the merged chapter notes as a single Markdown file. */
    @GetMapping("/{jobId}/download")
    public ResponseEntity<byte[]> downloadArtifact(@PathVariable UUID jobId) {
        NotesService.NotesPayload payload = notesService.getNotes(jobId);
        String merged = payload.merged() != null ? payload.merged() : "";
        byte[] bytes = merged.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"notes-" + jobId + ".md\"")
                .contentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"))
                .body(bytes);
    }

    /** Download all generated chapter notes as a ZIP bundle. */
    @GetMapping("/{jobId}/download-all")
    public ResponseEntity<byte[]> downloadAllArtifacts(@PathVariable UUID jobId) {
        try {
            NotesService.NotesPayload payload = notesService.getNotes(jobId);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (NotesService.NoteItem note : payload.notes()) {
                    ZipEntry entry = new ZipEntry(note.name() + ".md");
                    zos.putNextEntry(entry);
                    zos.write(note.content().getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
                // Include the merged document too, when present.
                if (payload.merged() != null) {
                    ZipEntry merged = new ZipEntry("00-MERGED.md");
                    zos.putNextEntry(merged);
                    zos.write(payload.merged().getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"notes-" + jobId + ".zip\"")
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .body(baos.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
