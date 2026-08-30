package com.example.notefactory.service;

import com.example.notefactory.domain.*;
import com.example.notefactory.repository.GenerationAttemptRepository;
import com.example.notefactory.repository.GenerationTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Transactional boundary around task state changes.
 *
 * The orchestrator runs external (slow, subprocess/container) calls on worker
 * threads outside any transaction. All database reads/writes for a task go
 * through this service so that JPA lazy associations are always accessed
 * inside an open session/transaction, avoiding LazyInitializationException.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskPersistenceService {

    private final GenerationTaskRepository taskRepository;
    private final GenerationAttemptRepository attemptRepository;

    /** Plain-value snapshot of everything a task execution needs. No JPA entities. */
    public record TaskContext(
            UUID taskId,
            UUID jobId,
            boolean overview,
            String roadmapName,
            String chapterName,
            String subChapterName,
            List<String> topics
    ) {}

    /**
     * Loads a task and eagerly materialises the surrounding graph
     * (chapter, roadmap, sub-chapters/topics) into a plain context record.
     * Safe to hand to a worker thread because it contains no lazy proxies.
     */
    @Transactional(readOnly = true)
    public TaskContext loadTaskContext(UUID taskId) {
        GenerationTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) return null;

        Roadmap roadmap = task.getGenerationJob().getRoadmap();
        String roadmapName = roadmap.getTitle();

        if ("OverviewAgent".equals(task.getAgentRole())) {
            return new TaskContext(task.getId(), task.getGenerationJob().getId(), true,
                    roadmapName, "Overview", null, List.of());
        }

        Chapter chapter = task.getChapter();
        if (chapter == null) {
            return null;
        }
        // Force-load sub-chapters so their topics (String) are available.
        String chapterName = chapter.getName();
        List<String> topics = chapter.getSubChapters().stream()
                .map(SubChapter::getTopics)
                .filter(t -> t != null && !t.isBlank())
                .toList();

        return new TaskContext(task.getId(), task.getGenerationJob().getId(), false,
                roadmapName, chapterName, null, topics);
    }

    @Transactional
    public void markRunning(UUID taskId) {
        GenerationTask task = taskRepository.findById(taskId).orElse(null);
        if (task != null && task.getStatus() != TaskStatus.CANCELLED) {
            task.setStatus(TaskStatus.RUNNING);
            task.setStepDescription("Starting...");
            taskRepository.save(task);
        }
    }

    @Transactional
    public void saveNoteAndComplete(UUID taskId, String content, String filename, String chapterName) {
        GenerationTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) return;
        task.setStatus(TaskStatus.COMPLETE);
        task.setStepDescription("Finished: " + chapterName);
        task.setErrorDetail(null);
        taskRepository.save(task);
    }

    @Transactional
    public void failTask(UUID taskId, String reason) {
        GenerationTask task = taskRepository.findById(taskId).orElse(null);
        if (task != null) {
            task.setStatus(TaskStatus.FAILED);
            task.setErrorDetail(reason);
            task.setStepDescription("Failed: " + reason);
            taskRepository.save(task);
        }
    }

    @Transactional
    public void recordAttempt(UUID taskId, String provider, Long latencyMs, String role) {
        GenerationTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) return;
        GenerationAttempt attempt = new GenerationAttempt();
        attempt.setGenerationTask(task);
        attempt.setProvider(provider);
        attempt.setDurationMs(latencyMs);
        attemptRepository.save(attempt);
        task.setCurrentAttempt(attempt);
        taskRepository.save(task);
    }

    @Transactional
    public void updateStep(UUID taskId, String step) {
        GenerationTask task = taskRepository.findById(taskId).orElse(null);
        if (task != null) {
            task.setStepDescription(step);
            taskRepository.save(task);
        }
    }
}
