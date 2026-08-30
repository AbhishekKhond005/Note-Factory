package com.example.notefactory.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ArtifactStorageService {

    @Value("${notefactory.storage.notes-dir:notes}")
    private String notesBaseDir;

    @Value("${notefactory.storage.roadmaps-dir:roadmaps}")
    private String roadmapsBaseDir;

    /** Ensure storage directories exist on startup. */
    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(notesBaseDir));
        Files.createDirectories(Paths.get(roadmapsBaseDir));
    }

    /**
     * Saves a note file following the structure:
     * <notesDir>/<roadmapName>/<chapterName>/<fileName>
     */
    public Path saveNote(String roadmapName, String chapterName, String fileName, String content) throws IOException {
        Path dirPath = Paths.get(notesBaseDir,
                sanitizeFilename(roadmapName),
                sanitizeFilename(chapterName));
        Files.createDirectories(dirPath);

        Path filePath = dirPath.resolve(sanitizeFilename(fileName));
        Files.writeString(filePath, content);
        return filePath;
    }

    /**
     * Returns the resolved path of a stored note file (does not require it to exist).
     */
    public Path getNotePath(String roadmapName, String chapterName, String fileName) {
        return Paths.get(notesBaseDir,
                sanitizeFilename(roadmapName),
                sanitizeFilename(chapterName),
                sanitizeFilename(fileName));
    }

    /**
     * Returns the absolute path to the roadmaps directory.
     */
    public Path getRoadmapsDir() {
        return Paths.get(roadmapsBaseDir);
    }

    /**
     * Sanitizes a path segment: keeps alphanumerics, hyphens, underscores,
     * and dots. Everything else becomes an underscore.
     * Preserves kebab-case names like "session-security".
     */
    public String sanitizeFilename(String input) {
        if (input == null) return "unknown";
        return input.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

