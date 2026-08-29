package com.example.notefactory.agent;

import com.example.notefactory.provider.GenerationProvider;
import com.example.notefactory.provider.GenerationRequest;
import com.example.notefactory.provider.GenerationResponse;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class OutlineAgent extends GenerationAgent {
    public OutlineAgent(GenerationProvider provider) {
        super(provider);
    }

    public GenerationResponse generateOutline(String subChapterName, List<String> topics) {
        String prompt = "Create a detailed outline for a chapter titled '" + subChapterName + "'.\n" +
                        "It must cover the following topics: " + String.join(", ", topics) + ".\n" +
                        "Provide a prompt template that can be used to generate the actual notes.";
        
        return provider.generate(GenerationRequest.builder()
                .prompt(prompt)
                .maxTokens(1500)
                .build());
    }
}
