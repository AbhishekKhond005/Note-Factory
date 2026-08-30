package com.example.notefactory.agent;

import com.example.notefactory.provider.GenerationProvider;
import com.example.notefactory.provider.GenerationRequest;
import com.example.notefactory.provider.GenerationResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Crafting a dynamic, chapter-wide notes-writing prompt (the "Prompt 2" in the
 * pipeline) from the user's selected chapter (the "Prompt 1" — a hard-coded
 * meta prompt). The produced prompt is fed verbatim to the NoteWriterAgent.
 */
@Component
public class PromptCrafterAgent extends GenerationAgent {

    private static final String META_PROMPT_TEMPLATE = """
            You are a senior curriculum designer and expert technical writer.

            Your task is to write a DETAILED NOTES-WRITING PROMPT for an entire chapter called
            "%s" within the subject "%s". The chapter contains these sections and topics:
            %s

            The prompt you write will be given verbatim to an LLM to produce the actual,
            complete study notes for this WHOLE chapter.

            The notes the chapter must contain:
            - A top-level title: "# %s"
            - A 2-3 sentence "Why This Matters" intro after the title
            - ONE "## Section" per section/topic group listed above, in order, each covering:
                • A plain-English definition
                • An analogy to everyday life
                • At least one concrete, realistic code example in a fenced block with a language tag
                • How it connects to the other sections
            - A "## Common Mistakes & Gotchas" section with at least 3 concrete pitfalls
            - A "## Key Takeaways" section with 5-7 bullet points
            - Written in PLAIN, FRIENDLY ENGLISH — explain every term the first time it appears,
              never assume the reader already knows, use everyday analogies for abstract concepts
            - COMPREHENSIVE but ruthlessly focused: every sentence must teach something concrete
            - Syntactically correct, realistic code examples (no `foo`, `bar`, `x`, `y`)
            - Tone: like a brilliant friend who is also an expert — enthusiastic, precise, warm

            Output ONLY the notes-writing prompt. No preamble, no explanation, no meta-commentary.
            """;

    public PromptCrafterAgent(GenerationProvider provider) {
        super(provider);
    }

    /**
     * Uses the meta-prompt to craft a tailored, chapter-wide notes-writing prompt.
     *
     * @param chapterName    the user-selected chapter
     * @param roadmapTitle   the parent roadmap title for context
     * @param sectionTopics  the topics aggregated from the chapter's sub-chapters
     * @return a GenerationResponse whose text is the crafted notes-writing prompt (Prompt 2)
     */
    public GenerationResponse craftNotesPrompt(String chapterName, String roadmapTitle, List<String> sectionTopics) {
        String topicList = sectionTopics == null || sectionTopics.isEmpty()
                ? chapterName
                : String.join("\n", sectionTopics);

        String metaPrompt = META_PROMPT_TEMPLATE.formatted(chapterName, roadmapTitle, topicList, chapterName);

        return provider.generate(GenerationRequest.builder()
                .prompt(metaPrompt)
                .maxTokens(1600)
                .build());
    }
}
