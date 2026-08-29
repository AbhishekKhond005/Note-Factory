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

    public GenerationResponse planRoadmap(String topic, String userPrompt) {
        String prompt = "Create a comprehensive learning roadmap for: " + topic + ". " +
                        (userPrompt != null ? "\nAdditional instructions: " + userPrompt : "") +
                        "\nFormat as a unicode tree (e.g. using ├── and └──). Maximum 3 levels deep (Chapter, SubChapter, Topic).";
        
        return provider.generate(GenerationRequest.builder()
                .prompt(prompt)
                .maxTokens(1000)
                .build());
    }
}
