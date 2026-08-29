package com.example.notefactory.service;

import com.example.notefactory.agent.*;
import com.example.notefactory.domain.*;
import com.example.notefactory.provider.GenerationResponse;
import com.example.notefactory.repository.GenerationAttemptRepository;
import com.example.notefactory.repository.GenerationTaskRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationOrchestrator {
    private final GenerationTaskRepository taskRepository;
    private final GenerationAttemptRepository attemptRepository;
    
    private final OutlineAgent outlineAgent;
    private final NoteWriterAgent noteWriterAgent;
    private final CriticAgent criticAgent;
    private final RepairAgent repairAgent;
    private final OverviewAgent overviewAgent;
    
    private final ArtifactStorageService storageService;
    private final SimpMessagingTemplate messagingTemplate;

    private ExecutorService executor;
    private AtomicInteger activeTasks = new AtomicInteger(0);
    private final int MAX_CONCURRENCY = 2;

    @PostConstruct
    public void init() {
        executor = Executors.newFixedThreadPool(MAX_CONCURRENCY);
        // Resume any pending tasks
        schedulePendingTasks();
    }

    public synchronized void schedulePendingTasks() {
        if (activeTasks.get() >= MAX_CONCURRENCY) return;

        List<GenerationTask> queued = taskRepository.findByStatus(TaskStatus.QUEUED);
        for (GenerationTask task : queued) {
            if (activeTasks.get() >= MAX_CONCURRENCY) break;
            activeTasks.incrementAndGet();
            
            task.setStatus(TaskStatus.RUNNING);
            taskRepository.save(task);
            
            executor.submit(() -> executeTask(task.getId()));
        }
    }

    private void executeTask(java.util.UUID taskId) {
        try {
            GenerationTask task = taskRepository.findById(taskId).orElse(null);
            if (task == null || task.getStatus() == TaskStatus.CANCELLED) return;

            log.info("Executing task: {} for {}", taskId, task.getAgentRole());
            
            if ("OverviewAgent".equals(task.getAgentRole())) {
                executeOverviewTask(task);
            } else {
                executeSubChapterPipeline(task);
            }
            
        } catch (Exception e) {
            log.error("Task execution failed", e);
            GenerationTask task = taskRepository.findById(taskId).orElse(null);
            if (task != null) {
                task.setStatus(TaskStatus.FAILED);
                task.setErrorDetail(e.getMessage());
                taskRepository.save(task);
            }
        } finally {
            activeTasks.decrementAndGet();
            schedulePendingTasks(); // Check for more work
        }
    }

    private void executeOverviewTask(GenerationTask task) throws Exception {
        String topic = task.getGenerationJob().getRoadmap().getTitle();
        broadcast(task, "Generating overview for " + topic);
        
        GenerationResponse resp = overviewAgent.generateOverview(topic, null);
        recordAttempt(task, resp, "OverviewAgent");
        
        if (resp.isQuotaError()) {
            task.setStatus(TaskStatus.FAILED);
            task.setErrorDetail("Quota exceeded");
        } else {
            saveNotesAndComplete(task, resp.getText(), "overview.md", "Overview");
        }
        taskRepository.save(task);
    }

    private void executeSubChapterPipeline(GenerationTask task) throws Exception {
        SubChapter sc = task.getSubChapter();
        String roadmapName = task.getGenerationJob().getRoadmap().getTitle();
        String chapterName = sc.getChapter().getName();
        
        // Step 1: Outline
        task.setAgentRole("OutlineAgent");
        broadcast(task, "Generating outline...");
        GenerationResponse outlineResp = outlineAgent.generateOutline(sc.getName(), sc.getTopics());
        recordAttempt(task, outlineResp, "OutlineAgent");
        
        if (outlineResp.isQuotaError()) {
            failTask(task, "Quota exceeded during outline");
            return;
        }

        // Step 2: Write Notes
        task.setAgentRole("NoteWriterAgent");
        broadcast(task, "Writing notes...");
        GenerationResponse notesResp = noteWriterAgent.writeNotes(outlineResp.getText());
        recordAttempt(task, notesResp, "NoteWriterAgent");
        
        if (notesResp.isQuotaError()) {
            failTask(task, "Quota exceeded during writing");
            return;
        }
        
        String finalContent = notesResp.getText();

        // Step 3: Critic
        task.setAgentRole("CriticAgent");
        broadcast(task, "Validating output...");
        CriticAgent.CriticResult eval = criticAgent.evaluate(finalContent);
        
        if (!eval.accepted()) {
            // Step 4: Repair (only try once)
            task.setAgentRole("RepairAgent");
            broadcast(task, "Repairing issues: " + eval.reason());
            GenerationResponse repairResp = repairAgent.repair(finalContent, eval.reason());
            recordAttempt(task, repairResp, "RepairAgent");
            if (!repairResp.isQuotaError()) {
                finalContent = repairResp.getText();
            }
        }

        saveNotesAndComplete(task, finalContent, sc.getName() + ".md", chapterName);
    }

    private void saveNotesAndComplete(GenerationTask task, String content, String filename, String chapterName) throws Exception {
        String roadmapName = task.getGenerationJob().getRoadmap().getTitle();
        java.nio.file.Path savedPath = storageService.saveNote(roadmapName, chapterName, filename, content);
        
        task.setStatus(TaskStatus.COMPLETE);
        task.setStepDescription("Finished");
        // TODO: Save artifact record and link it to task
        broadcast(task, "Complete");
        taskRepository.save(task);
    }
    
    private void failTask(GenerationTask task, String reason) {
        task.setStatus(TaskStatus.FAILED);
        task.setErrorDetail(reason);
        taskRepository.save(task);
        broadcast(task, "Failed: " + reason);
    }

    private void recordAttempt(GenerationTask task, GenerationResponse resp, String role) {
        GenerationAttempt attempt = new GenerationAttempt();
        attempt.setGenerationTask(task);
        attempt.setProvider(resp.getProviderName());
        attempt.setDurationMs(resp.getLatencyMs());
        attemptRepository.save(attempt);
    }

    private void broadcast(GenerationTask task, String message) {
        task.setStepDescription(message);
        taskRepository.save(task);
        messagingTemplate.convertAndSend("/topic/jobs/" + task.getGenerationJob().getId(), 
                "{\"taskId\":\"" + task.getId() + "\", \"status\":\"" + task.getStatus() + "\", \"message\":\"" + message + "\"}");
    }
}
