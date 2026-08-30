package com.example.notefactory.web.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Request for POST /api/generate — starts a chapter-based generation job.
 * - roadmapId: a persisted roadmap (required, unless roadmapContent given)
 * - chapterIndexes: 0-based indexes of chapters to generate (empty = all chapters)
 */
@Data
public class GenerateRequest {
    private UUID roadmapId;
    private String roadmapContent;
    private String roadmapFile;
    private List<Integer> chapterIndexes;
    private String prompt;
}
