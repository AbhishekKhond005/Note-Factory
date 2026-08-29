package com.example.notefactory.agent;

import com.example.notefactory.provider.GenerationProvider;
import com.example.notefactory.provider.GenerationRequest;
import com.example.notefactory.provider.GenerationResponse;
import org.springframework.stereotype.Component;

@Component
public class OverviewAgent extends GenerationAgent {
    public OverviewAgent(GenerationProvider provider) {
        super(provider);
    }

    public GenerationResponse generateOverview(String topic, String userPrompt) {
        String prompt = "Write a beginner-level, short overview (300-600 words) for the topic: " + topic + ". " +
                        (userPrompt != null ? "\nAdditional instructions: " + userPrompt : "") +
                        "\nFormat as Markdown.";
        
        return provider.generate(GenerationRequest.builder()
                .prompt(prompt)
                .maxTokens(1000)
                .build());
    }
}
