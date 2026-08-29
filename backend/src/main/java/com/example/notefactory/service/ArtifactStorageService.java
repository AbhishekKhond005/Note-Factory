package com.example.notefactory.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ArtifactStorageService {

    @Value("${notefactory.storage.notes-dir:notes}")
    private String notesBaseDir;

    /**
     * Saves a note file following the structure:
     * notes/<roadmapName>/<chapterName>/<fileName>
     */
    public Path saveNote(String roadmapName, String chapterName, String fileName, String content) throws IOException {
        String safeRoadmap = sanitizeFilename(roadmapName);
        String safeChapter = sanitizeFilename(chapterName);
        String safeFile = sanitizeFilename(fileName);

        Path dirPath = Paths.get(notesBaseDir, safeRoadmap, safeChapter);
        Files.createDirectories(dirPath);

        Path filePath = dirPath.resolve(safeFile);
        Files.writeString(filePath, content);
        
        return filePath;
    }

    private String sanitizeFilename(String input) {
        if (input == null) return "unknown";
        return input.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
