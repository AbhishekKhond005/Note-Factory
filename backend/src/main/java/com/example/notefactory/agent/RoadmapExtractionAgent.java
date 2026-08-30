package com.example.notefactory.agent;

import com.example.notefactory.provider.GenerationProvider;
import com.example.notefactory.provider.GenerationRequest;
import com.example.notefactory.provider.GenerationResponse;
import org.springframework.stereotype.Component;

@Component
public class RoadmapExtractionAgent extends GenerationAgent {

    public RoadmapExtractionAgent(GenerationProvider provider) {
        super(provider);
    }

    /**
     * Extracts a structured unicode tree from a raw, generic text roadmap.
     * If the provided roadmap is too vague or lacks sufficient structure,
     * this agent will extract the domain and generate a comprehensive custom roadmap.
     */
    public GenerationResponse extractRoadmap(String rawText) {
        String prompt = """
                You are an expert curriculum designer and parser.
                You have been given a raw text file representing a user's study roadmap.
                Your goal is to parse this text and output it EXACTLY as a unicode directory tree.
                
                Rules for the output format:
                - Output ONLY a unicode directory tree. No prose before or after.
                - Exactly 3 levels: Chapter (├── 01-name/), SubChapter (│   ├── topic-name.md),
                  and optionally a 3rd level for sub-topics inside a sub-chapter.
                - Chapter names must be kebab-case prefixed with a zero-padded index (01-, 02-, …).
                - SubChapter filenames must be kebab-case .md files that name a specific concept.
                - Sequence matters: preserve the user's order as much as possible.
                
                VAGUENESS RULE:
                If the user's raw text is extremely vague, generic, or lacks any real structure (e.g., "I want to learn java" or just a single line topic), you MUST:
                1. Identify the main domain/topic they want to learn.
                2. Generate a comprehensive, custom roadmap tree for that domain yourself (following the format rules above).
                3. You MUST prefix your response with exactly: "[VAGUE_ROADMAP_REPLACED]" (on its own line) before the unicode tree.
                
                Here is the raw text to parse:
                
                %s
                """.formatted(rawText);

        return provider.generate(GenerationRequest.builder()
                .prompt(prompt)
                .maxTokens(1500)
                .build());
    }
}
