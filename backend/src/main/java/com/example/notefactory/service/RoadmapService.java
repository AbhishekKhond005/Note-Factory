package com.example.notefactory.service;

import com.example.notefactory.domain.Roadmap;
import com.example.notefactory.domain.RoadmapSource;
import com.example.notefactory.repository.RoadmapRepository;
import com.example.notefactory.agent.RoadmapExtractionAgent;
import com.example.notefactory.agent.RoadmapPlannerAgent;
import com.example.notefactory.provider.GenerationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoadmapService {
    private static final String VAGUE_MARKER = "[VAGUE_ROADMAP_REPLACED]";

    private final RoadmapRepository roadmapRepository;
    private final RoadmapParser roadmapParser;
    private final RoadmapPlannerAgent roadmapPlannerAgent;
    private final RoadmapExtractionAgent roadmapExtractionAgent;

    @Transactional(readOnly = true)
    public List<Roadmap> getAllRoadmaps() {
        return roadmapRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Roadmap getRoadmap(UUID id) {
        return roadmapRepository.findById(id).orElseThrow(() -> new RuntimeException("Roadmap not found"));
    }

    /**
     * Accepts any free-form roadmap text and routes it through the AI extraction agent.
     * The agent normalizes it into the unicode tree format the parser expects.
     * If the input was too vague, the agent generates a custom roadmap and signals it
     * with a [VAGUE_ROADMAP_REPLACED] prefix — we strip the prefix and attach a
     * warning_message to the returned entity so callers can inform the user.
     */
    @Transactional
    public Roadmap uploadRoadmap(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Roadmap content is empty");
        }
        GenerationResponse response = roadmapExtractionAgent.extractRoadmap(content);
        if (response.isQuotaError()) {
            throw new RuntimeException("Quota error while extracting roadmap structure");
        }

        String extractedText = response.getText().trim();
        String warningMessage = null;

        if (extractedText.contains(VAGUE_MARKER)) {
            warningMessage = "Your roadmap was too vague to parse directly. " +
                    "A comprehensive custom roadmap has been generated for you based on the domain identified in your text.";
            extractedText = extractedText.substring(extractedText.indexOf(VAGUE_MARKER) + VAGUE_MARKER.length()).trim();
        }

        Roadmap rm = roadmapParser.parse(extractedText, RoadmapSource.UPLOADED);
        rm.setTitle(truncate(rm.getTitle(), 250));
        rm.setWarningMessage(warningMessage);
        return roadmapRepository.save(rm);
    }

    @Transactional
    public Roadmap generateRoadmap(String topic, String prompt) {
        GenerationResponse response = roadmapPlannerAgent.planRoadmap(topic, prompt);
        if (response.isQuotaError()) {
            throw new RuntimeException("Quota error while generating roadmap");
        }
        Roadmap rm = roadmapParser.parse(response.getText(), RoadmapSource.GENERATED);
        // Always use the user-supplied topic as the title (the AI may output
        // a long description before the tree which the parser picks up)
        rm.setTitle(truncate(topic, 250));
        return roadmapRepository.save(rm);
    }

    private String truncate(String s, int max) {
        if (s == null) return "Untitled";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
