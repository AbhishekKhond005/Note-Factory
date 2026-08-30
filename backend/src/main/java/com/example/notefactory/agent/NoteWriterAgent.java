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

    /**
     * Executes the notes-writing prompt produced by {@link PromptCrafterAgent}
     * and appends a strict output-format directive so the LLM returns only
     * the Markdown notes — no preamble, no narration, no apology.
     *
     * @param craftedPrompt the detailed notes-writing prompt from PromptCrafterAgent
     * @return the generated study notes as Markdown
     */
    public GenerationResponse writeNotes(String craftedPrompt) {
        String fullPrompt = craftedPrompt +
                "\n\n---\n" +
                "IMPORTANT: Return ONLY the final Markdown study notes. " +
                "Do NOT include any preamble, commentary, apology, word count, " +
                "or narration about what you are doing. Start directly with the note title.";

        return provider.generate(GenerationRequest.builder()
                .prompt(fullPrompt)
                .maxTokens(4000)
                .build());
    }
}

