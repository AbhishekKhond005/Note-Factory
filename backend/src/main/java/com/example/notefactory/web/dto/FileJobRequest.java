package com.example.notefactory.web.dto;

import lombok.Data;

import java.util.List;

@Data
public class FileJobRequest {
    private String filename;
    /** 0-based chapter indexes to generate; empty/null = all chapters. */
    private List<Integer> chapterIndexes;
}
