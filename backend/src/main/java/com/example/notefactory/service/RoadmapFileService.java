package com.example.notefactory.service;

import com.example.notefactory.agent.RoadmapExtractionAgent;
import com.example.notefactory.domain.Chapter;
import com.example.notefactory.domain.Roadmap;
import com.example.notefactory.domain.RoadmapSource;
import com.example.notefactory.provider.GenerationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Loads roadmap files from the configured roadmaps directory on disk,
 * routes them through the AI RoadmapExtractionAgent to normalise any
 * free-form text into a unicode tree, then parses and returns them.
 *
 * This service does NOT persist anything to the database — it is used
 * solely to load a roadmap for a generation request when the caller
 * supplies a filename instead of a DB-stored roadmap ID.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoadmapFileService {

    private static final String VAGUE_MARKER = "[VAGUE_ROADMAP_REPLACED]";

    private final ArtifactStorageService storageService;
    private final RoadmapExtractionAgent extractionAgent;
    private final RoadmapParser roadmapParser;

    /**
     * Lists all *.txt files in the configured roadmaps directory.
     */
    public List<String> listRoadmapFiles() throws IOException {
        Path dir = storageService.getRoadmapsDir();
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.getFileName().toString().endsWith(".txt"))
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    /**
     * Loads a roadmap from the given filename, runs it through the AI
     * extraction agent, and returns the parsed (transient) Roadmap.
     *
     * @param filename  name of the file inside the roadmaps directory
     * @return parsed Roadmap (not saved to DB)
     * @throws IllegalArgumentException if the file doesn't exist
     */
    public RoadmapLoadResult load(String filename) throws IOException {
        Path file = storageService.getRoadmapsDir().resolve(filename);
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("Roadmap file not found: " + filename);
        }

        String rawContent = Files.readString(file);

        // Route through AI extraction agent so any format is accepted
        GenerationResponse extractionResp = extractionAgent.extractRoadmap(rawContent);
        if (extractionResp.isQuotaError()) {
            throw new RuntimeException("Quota exceeded while extracting roadmap structure");
        }

        String extractedText = extractionResp.getText().trim();
        String warning = null;

        if (extractedText.contains(VAGUE_MARKER)) {
            warning = "Your roadmap file was too vague to parse directly. " +
                    "A comprehensive custom roadmap has been generated based on the domain identified in the file.";
            extractedText = extractedText
                    .substring(extractedText.indexOf(VAGUE_MARKER) + VAGUE_MARKER.length())
                    .trim();
        }

        Roadmap roadmap = roadmapParser.parse(extractedText, RoadmapSource.UPLOADED);
        roadmap.setRawText(rawContent);
        roadmap.setWarningMessage(warning);

        log.info("Loaded roadmap '{}': {} chapters, warning={}",
                filename, roadmap.getChapters().size(), warning != null);

        return new RoadmapLoadResult(roadmap, warning);
    }

    /**
     * Returns the chapter at {@code chapterIndex} from a roadmap file,
     * or all chapters if chapterIndex is -1.
     */
    public List<Chapter> loadChapters(String filename, int chapterIndex) throws IOException {
        Roadmap rm = load(filename).roadmap();
        if (chapterIndex < 0) return rm.getChapters();
        if (chapterIndex >= rm.getChapters().size()) {
            throw new IllegalArgumentException(
                    "Chapter index " + chapterIndex + " out of range (0-" + (rm.getChapters().size() - 1) + ")");
        }
        return List.of(rm.getChapters().get(chapterIndex));
    }

    public record RoadmapLoadResult(Roadmap roadmap, String warning) {}
}
