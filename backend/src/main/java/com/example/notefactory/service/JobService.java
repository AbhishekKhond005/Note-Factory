package com.example.notefactory.service;

import com.example.notefactory.domain.*;
import com.example.notefactory.repository.GenerationJobRepository;
import com.example.notefactory.repository.RoadmapRepository;
import com.example.notefactory.web.dto.JobResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final GenerationJobRepository jobRepository;
    private final RoadmapRepository roadmapRepository;
    private final GenerationOrchestrator orchestrator;
    private final RoadmapFileService roadmapFileService;

    /**
     * Creates a chapter-based generation job for a DB-persisted roadmap.
     * One GenerationTask is created per selected chapter (0-based indexes);
     * an empty or null {@code chapterIndexes} means "all chapters".
     */
    @Transactional
    public GenerationJob createJobForRoadmap(UUID roadmapId, List<Integer> chapterIndexes) {
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new RuntimeException("Roadmap not found"));

        List<Chapter> chapters = roadmap.getChapters();
        List<Chapter> targets = new ArrayList<>();
        if (chapterIndexes == null || chapterIndexes.isEmpty()) {
            targets.addAll(chapters);
        } else {
            for (Integer i : chapterIndexes) {
                if (i == null || i < 0 || i >= chapters.size()) {
                    throw new IllegalArgumentException(
                            "Chapter index " + i + " out of range (0-" + (chapters.size() - 1) + ")");
                }
                targets.add(chapters.get(i));
            }
        }

        GenerationJob job = buildJob(roadmap, "chapter");
        for (Chapter chapter : targets) {
            job.getTasks().add(buildTask(job, chapter));
        }

        GenerationJob saved = jobRepository.save(job);
        orchestrator.schedulePendingTasks();
        log.info("Created chapter job {} for roadmap {} with {} tasks", saved.getId(), roadmapId, targets.size());
        return saved;
    }

    /**
     * Creates an overview job for a topic — a single cheap task that writes one
     * short introduction note.
     */
    @Transactional
    public GenerationJob createOverviewJob(String topic) {
        // Persist a minimal generated roadmap so the FK on generation_job resolves.
        Roadmap roadmap = new Roadmap();
        roadmap.setTitle(truncate(topic, 250));
        roadmap.setSource(RoadmapSource.GENERATED);
        Roadmap saved = roadmapRepository.save(roadmap);

        GenerationJob job = buildJob(saved, "overview");
        GenerationTask task = new GenerationTask();
        task.setGenerationJob(job);
        task.setStatus(TaskStatus.QUEUED);
        task.setAgentRole("OverviewAgent");
        task.setStepDescription("Pending");
        job.getTasks().add(task);

        GenerationJob savedJob = jobRepository.save(job);
        orchestrator.schedulePendingTasks();
        return savedJob;
    }

    /**
     * Loads a roadmap from a .txt file on disk, persists it, then queues a
     * chapter-based generation job for the given chapter indexes (empty = all).
     */
    @Transactional
    public FileJobResult createJobFromFile(String filename, List<Integer> chapterIndexes) throws IOException {
        RoadmapFileService.RoadmapLoadResult loaded = roadmapFileService.load(filename);
        Roadmap roadmap = loaded.roadmap();
        Roadmap saved = roadmapRepository.save(roadmap);

        List<Chapter> chapters = saved.getChapters();
        List<Chapter> targets = new ArrayList<>();
        if (chapterIndexes == null || chapterIndexes.isEmpty()) {
            targets.addAll(chapters);
        } else {
            for (Integer i : chapterIndexes) {
                if (i == null || i < 0 || i >= chapters.size()) {
                    throw new IllegalArgumentException(
                            "Chapter index " + i + " out of range (0-" + (chapters.size() - 1) + ")");
                }
                targets.add(chapters.get(i));
            }
        }

        GenerationJob job = buildJob(saved, "chapter");
        for (Chapter chapter : targets) {
            job.getTasks().add(buildTask(job, chapter));
        }

        GenerationJob savedJob = jobRepository.save(job);
        orchestrator.schedulePendingTasks();

        log.info("Created file-based job {} for '{}' with {} chapter tasks",
                savedJob.getId(), filename, targets.size());

        return new FileJobResult(savedJob, loaded.warning());
    }

    private GenerationJob buildJob(Roadmap roadmap, String scope) {
        GenerationJob job = new GenerationJob();
        job.setRoadmap(roadmap);
        job.setStatus(JobStatus.QUEUED);
        job.setRequestedScope(scope);
        job.setTasks(new ArrayList<>());
        return job;
    }

    private GenerationTask buildTask(GenerationJob job, Chapter chapter) {
        GenerationTask task = new GenerationTask();
        task.setGenerationJob(job);
        task.setChapter(chapter);
        task.setStatus(TaskStatus.QUEUED);
        task.setAgentRole("PromptCrafterAgent");
        task.setStepDescription("Pending");
        task.setRetryCount(0);
        return task;
    }

    public List<String> listRoadmapFiles() throws IOException {
        return roadmapFileService.listRoadmapFiles();
    }

    @Transactional(readOnly = true)
    public List<GenerationJob> listJobs() {
        // Simple listing (all jobs, newest-first approximation via findAll order).
        return jobRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<JobResponse> listJobViews() {
        return jobRepository.findAll().stream().map(this::toView).toList();
    }

    public GenerationJob getJob(UUID jobId) {
        return jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));
    }

    @Transactional(readOnly = true)
    public JobResponse getJobView(UUID jobId) {
        return toView(getJob(jobId));
    }

    private JobResponse toView(GenerationJob job) {
        String roadmapTitle = job.getRoadmap() != null && job.getRoadmap().getTitle() != null
                ? job.getRoadmap().getTitle() : "Untitled Roadmap";
        List<GenerationTask> tasks = job.getTasks();
        List<JobResponse.ChapterTask> chapters = (tasks == null ? List.<GenerationTask>of() : tasks).stream()
                .map(t -> {
                    String name = t.getChapter() != null ? t.getChapter().getName() : "Overview";
                    return new JobResponse.ChapterTask(name,
                            t.getStatus().name().toLowerCase(),
                            t.getStepDescription(),
                            t.getErrorDetail());
                })
                .toList();
        return new JobResponse(job.getId(), job.getStatus().name().toLowerCase(),
                job.getRequestedScope(), roadmapTitle, job.getCreatedAt(), job.getUpdatedAt(), chapters);
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

    private String truncate(String s, int max) {
        if (s == null) return "Untitled";
        return s.length() <= max ? s : s.substring(0, max);
    }

    public record FileJobResult(GenerationJob job, String warning) {}
}
