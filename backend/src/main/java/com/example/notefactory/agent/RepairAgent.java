package com.example.notefactory.agent;

import com.example.notefactory.provider.GenerationProvider;
import com.example.notefactory.provider.GenerationRequest;
import com.example.notefactory.provider.GenerationResponse;
import org.springframework.stereotype.Component;

@Component
public class RepairAgent extends GenerationAgent {
    public RepairAgent(GenerationProvider provider) {
        super(provider);
    }

    public GenerationResponse repair(String badContent, String rejectionReason) {
        String prompt = "The following content was rejected for the reason: " + rejectionReason + "\n" +
                        "Please fix the content and return only the fixed Markdown, without any narration or apologies.\n\n" +
                        "CONTENT:\n" + badContent;
        
        return provider.generate(GenerationRequest.builder()
                .prompt(prompt)
                .maxTokens(3000)
                .build());
    }
}
