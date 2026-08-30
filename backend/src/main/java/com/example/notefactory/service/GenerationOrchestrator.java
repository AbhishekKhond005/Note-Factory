package com.example.notefactory.service;

import com.example.notefactory.agent.*;
import com.example.notefactory.domain.*;
import com.example.notefactory.provider.GenerationResponse;
import com.example.notefactory.repository.GenerationTaskRepository;
import com.example.notefactory.service.TaskPersistenceService.TaskContext;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chapter-based generation orchestrator.
 *
 * Flow per job:
 *   user selects a chapter (or topic -> roadmap -> chapters)
 *   -> one task per selected chapter
 *   -> PromptCrafterAgent (hardcoded Prompt 1) produces a dynamic Prompt 2
 *      tailored to the whole chapter
 *   -> NoteWriterAgent feeds Prompt 2 to the LLM to produce the chapter notes
 *   -> CriticAgent validates; RepairAgent fixes once if needed
 *   -> notes saved under <notesDir>/<roadmap>/<chapter>/<chapter>.md
 *
 * Each task runs on a bounded worker pool, and every external generation goes
 * through the Docker-wrapped OpenCode CLI so per-session limits are bypassed
 * with a fresh container per chapter invocation. All DB access happens through
 * {@link TaskPersistenceService} so lazy associations are always loaded inside
 * a transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationOrchestrator {

    private static final int MAX_CONCURRENCY = 4;

    private final GenerationTaskRepository taskRepository;
    private final TaskPersistenceService persistence;

    private final PromptCrafterAgent promptCrafterAgent;
    private final NoteWriterAgent noteWriterAgent;
    private final CriticAgent criticAgent;
    private final RepairAgent repairAgent;
    private final OverviewAgent overviewAgent;

    private final ArtifactStorageService storageService;
    private final SimpMessagingTemplate messagingTemplate;

    private ExecutorService executor;
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        executor = Executors.newFixedThreadPool(MAX_CONCURRENCY);
        schedulePendingTasks();
    }

    public int getActiveTasks() {
        return activeTasks.get();
    }

    public int getMaxConcurrency() {
        return MAX_CONCURRENCY;
    }

    public synchronized void schedulePendingTasks() {
        if (activeTasks.get() >= MAX_CONCURRENCY) return;

        List<GenerationTask> queued = taskRepository.findByStatus(TaskStatus.QUEUED);
        for (GenerationTask task : queued) {
            if (activeTasks.get() >= MAX_CONCURRENCY) break;
            activeTasks.incrementAndGet();
            UUID taskId = task.getId();
            executor.submit(() -> executeTask(taskId));
        }
    }

    private void executeTask(UUID taskId) {
        try {
            TaskContext ctx = persistence.loadTaskContext(taskId);
            if (ctx == null) return;
            persistence.markRunning(taskId);

            log.info("Executing task {} for {}", taskId, ctx.overview() ? "overview" : ctx.chapterName());

            if (ctx.overview()) {
                executeOverviewTask(ctx);
            } else {
                executeChapterTask(ctx);
            }
        } catch (Exception e) {
            log.error("Task execution failed for {}", taskId, e);
            persistence.failTask(taskId, e.getMessage());
        } finally {
            activeTasks.decrementAndGet();
            schedulePendingTasks();
        }
    }

    private void executeOverviewTask(TaskContext ctx) throws Exception {
        String topic = ctx.roadmapName();
        broadcast(ctx, "status", "Generating overview for " + topic);

        GenerationResponse resp = overviewAgent.generateOverview(topic, null);
        persistence.recordAttempt(ctx.taskId(), resp.getProviderName(), resp.getLatencyMs(), "OverviewAgent");

        if (resp.isQuotaError()) {
            persistence.failTask(ctx.taskId(), "Quota exceeded");
            broadcast(ctx, "error", "Quota exceeded");
            return;
        }

        String roadmapName = ctx.roadmapName();
        java.nio.file.Path saved = storageService.saveNote(roadmapName, "Overview", "overview.md", resp.getText());
        persistence.saveNoteAndComplete(ctx.taskId(), resp.getText(), "overview.md", "Overview");
        broadcast(ctx, "complete", "Complete: " + saved.getFileName());
    }

    private void executeChapterTask(TaskContext ctx) throws Exception {
        String chapterName = ctx.chapterName();
        String roadmapName = ctx.roadmapName();

        // Step 1: hardcoded Prompt 1 -> dynamic Prompt 2 tailored to the whole chapter
        broadcast(ctx, "status", "Crafting notes prompt for " + chapterName + "...");
        GenerationResponse craftedPromptResp = promptCrafterAgent.craftNotesPrompt(chapterName, roadmapName, ctx.topics());
        persistence.recordAttempt(ctx.taskId(), craftedPromptResp.getProviderName(), craftedPromptResp.getLatencyMs(), "PromptCrafterAgent");

        if (craftedPromptResp.isQuotaError()) {
            persistence.failTask(ctx.taskId(), "Quota exceeded during prompt crafting");
            broadcast(ctx, "error", "Quota exceeded during prompt crafting");
            return;
        }
        String prompt2 = craftedPromptResp.getText();

        // Step 2: feed Prompt 2 to the LLM to produce chapter notes
        broadcast(ctx, "status", "Writing notes for chapter " + chapterName + "...");
        GenerationResponse notesResp = noteWriterAgent.writeNotes(prompt2);
        persistence.recordAttempt(ctx.taskId(), notesResp.getProviderName(), notesResp.getLatencyMs(), "NoteWriterAgent");

        if (notesResp.isQuotaError()) {
            persistence.failTask(ctx.taskId(), "Quota exceeded during writing");
            broadcast(ctx, "error", "Quota exceeded during writing");
            return;
        }

        String finalContent = notesResp.getText();

        // Step 3: critic
        broadcast(ctx, "status", "Validating output...");
        CriticAgent.CriticResult eval = criticAgent.evaluate(finalContent);

        // Step 4: repair once if rejected
        if (!eval.accepted()) {
            broadcast(ctx, "status", "Repairing issues: " + eval.reason());
            GenerationResponse repairResp = repairAgent.repair(finalContent, eval.reason());
            persistence.recordAttempt(ctx.taskId(), repairResp.getProviderName(), repairResp.getLatencyMs(), "RepairAgent");
            if (!repairResp.isQuotaError()) {
                finalContent = repairResp.getText();
            }
        }

        java.nio.file.Path saved = storageService.saveNote(roadmapName, chapterName, chapterName + ".md", finalContent);
        persistence.saveNoteAndComplete(ctx.taskId(), finalContent, chapterName + ".md", chapterName);
        log.info("Chapter '{}' saved to {}", chapterName, saved);
        broadcast(ctx, "complete", "Complete: " + chapterName);
    }

    /**
     * Broadcasts a structured progress event to subscribers of /topic/jobs/{jobId}.
     * Matches the event vocabulary the frontend consumes (type, jobId, chapter,
     * status, step/message).
     */
    private void broadcast(TaskContext ctx, String type, String message) {
        persistence.updateStep(ctx.taskId(), message);
        var event = new java.util.LinkedHashMap<String, Object>();
        event.put("type", type);
        event.put("jobId", ctx.jobId().toString());
        event.put("taskId", ctx.taskId().toString());
        event.put("chapter", ctx.chapterName());
        event.put("status", type);
        event.put("message", message);
        messagingTemplate.convertAndSend("/topic/jobs/" + ctx.jobId(), (Object) event);
    }
}
