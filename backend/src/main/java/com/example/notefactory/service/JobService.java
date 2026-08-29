package com.example.notefactory.service;

import com.example.notefactory.domain.*;
import com.example.notefactory.repository.GenerationJobRepository;
import com.example.notefactory.repository.RoadmapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {
    private final GenerationJobRepository jobRepository;
    private final RoadmapRepository roadmapRepository;
    private final GenerationOrchestrator orchestrator;

    @Transactional
    public GenerationJob createJob(String type, UUID roadmapId, List<UUID> subChapterIds) {
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new RuntimeException("Roadmap not found"));

        GenerationJob job = new GenerationJob();
        job.setRoadmap(roadmap);
        job.setStatus(JobStatus.QUEUED);
        job.setRequestedScope(type);
        job.setTasks(new ArrayList<>());

        List<SubChapter> targets = new ArrayList<>();
        if ("overview".equals(type)) {
            // Overview doesn't map to a specific subchapter
            GenerationTask task = new GenerationTask();
            task.setGenerationJob(job);
            task.setStatus(TaskStatus.QUEUED);
            task.setAgentRole("OverviewAgent");
            task.setStepDescription("Pending");
            job.getTasks().add(task);
        } else {
            for (Chapter chapter : roadmap.getChapters()) {
                for (SubChapter sc : chapter.getSubChapters()) {
                    if (subChapterIds == null || subChapterIds.isEmpty() || subChapterIds.contains(sc.getId())) {
                        targets.add(sc);
                    }
                }
            }

            for (SubChapter sc : targets) {
                GenerationTask task = new GenerationTask();
                task.setGenerationJob(job);
                task.setSubChapter(sc);
                task.setStatus(TaskStatus.QUEUED);
                task.setAgentRole("OutlineAgent"); // Starts with outline
                task.setStepDescription("Pending");
                job.getTasks().add(task);
            }
        }

        GenerationJob saved = jobRepository.save(job);
        
        // Notify orchestrator
        orchestrator.schedulePendingTasks();

        return saved;
    }

    public GenerationJob getJob(UUID jobId) {
        return jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));
    }

    @Transactional
    public void cancelJob(UUID jobId) {
        GenerationJob job = getJob(jobId);
        if (job.getStatus() == JobStatus.QUEUED || job.getStatus() == JobStatus.RUNNING) {
            job.setStatus(JobStatus.CANCELLED);
            for (GenerationTask task : job.getTasks()) {
                if (task.getStatus() == TaskStatus.QUEUED || task.getStatus() == TaskStatus.PENDING) {
                    task.setStatus(TaskStatus.CANCELLED);
                }
            }
            jobRepository.save(job);
        }
    }
}
