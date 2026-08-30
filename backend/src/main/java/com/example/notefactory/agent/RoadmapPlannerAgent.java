package com.example.notefactory.agent;

import com.example.notefactory.provider.GenerationProvider;
import com.example.notefactory.provider.GenerationRequest;
import com.example.notefactory.provider.GenerationResponse;
import org.springframework.stereotype.Component;

@Component
public class RoadmapPlannerAgent extends GenerationAgent {

    public RoadmapPlannerAgent(GenerationProvider provider) {
        super(provider);
    }

    /**
     * Generates a structured, realistic learning roadmap for the given topic as a unicode tree.
     * The tree is parsed by {@link com.example.notefactory.service.RoadmapParser} into
     * Chapter → SubChapter → topics entities.
     */
    public GenerationResponse planRoadmap(String topic, String userPrompt) {
        String prompt = """
                You are an expert educator and curriculum designer.

                Create a comprehensive, sequenced learning roadmap for: "%s"

                Rules:
                - Output ONLY a unicode directory tree. No prose before or after.
                - Exactly 3 levels: Chapter (├── 00-name/), SubChapter (│   ├── topic-name.md),
                  and optionally a 3rd level for sub-topics inside a sub-chapter.
                - Chapter names must be kebab-case prefixed with a zero-padded index (00-, 01-, …).
                - SubChapter filenames must be kebab-case .md files that name a specific, real concept
                  (e.g. "filter-chain.md", not "topic1.md").
                - Sequence matters: order chapters and sub-chapters from foundational → advanced.
                - Include 4-10 chapters, each with 3-8 sub-chapters.
                - Cover prerequisites, core concepts, practical usage, edge cases, and best practices.
                - Do NOT include chapters on "Introduction" or "Conclusion" — dive straight into content.

                %s
                """.formatted(topic, userPrompt != null ? "Additional instructions: " + userPrompt : "");

        return provider.generate(GenerationRequest.builder()
                .prompt(prompt)
                .maxTokens(1500)
                .build());
    }
}
