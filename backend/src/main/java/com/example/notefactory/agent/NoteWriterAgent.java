package com.example.notefactory.agent;

import com.example.notefactory.provider.GenerationProvider;
import com.example.notefactory.provider.GenerationRequest;
import com.example.notefactory.provider.GenerationResponse;
import org.springframework.stereotype.Component;

@Component
public class NoteWriterAgent extends GenerationAgent {
    public NoteWriterAgent(GenerationProvider provider) {
        super(provider);
    }

    public GenerationResponse writeNotes(String outline) {
        String prompt = "Using the following outline and instructions, write comprehensive study notes in Markdown format.\n\n" +
                        "OUTLINE:\n" + outline;
        
        return provider.generate(GenerationRequest.builder()
                .prompt(prompt)
                .maxTokens(3000)
                .build());
    }
}
