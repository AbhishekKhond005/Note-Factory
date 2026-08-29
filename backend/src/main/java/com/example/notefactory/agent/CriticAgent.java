package com.example.notefactory.agent;

import com.example.notefactory.provider.GenerationProvider;
import org.springframework.stereotype.Component;

@Component
public class CriticAgent extends GenerationAgent {
    public CriticAgent(GenerationProvider provider) {
        super(provider);
    }

    // Deterministic validation checks
    public CriticResult evaluate(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new CriticResult(false, "Content is empty.");
        }
        if (content.length() < 100) {
            return new CriticResult(false, "Content is suspiciously short (less than 100 chars).");
        }
        if (content.contains("{{TOPIC}}") || content.contains("{{")) {
            return new CriticResult(false, "Content contains unreplaced template placeholders.");
        }
        String lower = content.toLowerCase();
        if (lower.contains("saved to") || lower.contains("word count:") || lower.contains("here is the file you requested")) {
            return new CriticResult(false, "Content appears to be narration rather than the actual study notes.");
        }
        return new CriticResult(true, "Looks good.");
    }

    public record CriticResult(boolean accepted, String reason) {}
}
