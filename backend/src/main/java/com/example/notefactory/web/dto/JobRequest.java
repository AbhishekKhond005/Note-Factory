package com.example.notefactory.web.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class JobRequest {
    private String type; // e.g. "chapter", "overview", "selected"
    private UUID roadmapId;
    private List<UUID> subChapterIds; 
}
