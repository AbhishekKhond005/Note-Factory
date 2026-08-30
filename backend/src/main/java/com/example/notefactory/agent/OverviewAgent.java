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

    /**
     * Generates a rich textbook-style overview/introduction for the roadmap topic.
     * This is the first note the user reads — it sets context for everything else.
     */
    public GenerationResponse generateOverview(String topic, String userPrompt) {
        String prompt = """
                You are writing the INTRODUCTION chapter of a high-quality textbook about "%s".

                Write a Markdown document that serves as the student's very first read. It must include:

                ## 1. Why This Matters
                2-3 paragraphs explaining the real-world importance of %s.
                Use a concrete story or analogy — not abstract statements like "it is important".

                ## 2. What Is %s? (The Big Picture)
                A plain-English explanation of what this subject is, what problem it solves,
                and where it fits in the broader technology landscape.
                Use an ASCII or text-art diagram if it helps visualise the big picture.

                ## 3. Prerequisites
                A bullet list of what the learner must already understand before diving in.
                For each prerequisite, add one sentence explaining *why* it matters here.

                ## 4. What You Will Learn
                A numbered list of the key skills and concepts the learner will have mastered
                by the end of this roadmap.

                ## 5. How to Use This Roadmap
                Brief guidance: suggested learning order, how to approach the hands-on parts,
                what to do when stuck.

                Tone: enthusiastic, friendly, expert — like a senior engineer who genuinely
                wants the reader to succeed. Plain English, no unnecessary jargon.
                Output ONLY the Markdown. No preamble, no narration.
                """.formatted(topic, topic, topic) +
                (userPrompt != null ? "\nAdditional instructions from the user: " + userPrompt : "");

        return provider.generate(GenerationRequest.builder()
                .prompt(prompt)
                .maxTokens(2000)
                .build());
    }
}
