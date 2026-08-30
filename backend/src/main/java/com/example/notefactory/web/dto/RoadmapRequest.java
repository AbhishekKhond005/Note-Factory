package com.example.notefactory.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class RoadmapRequest {
    private String topic;

    /** Accepts both "rawText" (backend convention) and "content" (frontend convention). */
    @JsonAlias("content")
    private String rawText;
}
