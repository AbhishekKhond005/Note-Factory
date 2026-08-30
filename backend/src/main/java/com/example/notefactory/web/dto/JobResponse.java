package com.example.notefactory.web.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Frontend-friendly representation of a GenerationJob.
 * Statuses are lower-cased to match the UI conventions.
 */
public record JobResponse(
        java.util.UUID id,
        String status,
        String scope,
        String roadmapTitle,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<ChapterTask> chapters
) {
    public record ChapterTask(String name, String status, String step, String error) {}
}
