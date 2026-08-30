package com.example.notefactory.service;

import com.example.notefactory.domain.GenerationJob;
import com.example.notefactory.domain.GenerationTask;
import com.example.notefactory.domain.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Reads generated note files from disk and presents them in the shape the
 * frontend "notes" page expects: { merged, notes: [{name, content}] }.
 */
@Service
@RequiredArgsConstructor
public class NotesService {

    private final JobService jobService;
    private final ArtifactStorageService storageService;

    /** Returns whether the given job has at least one fully generated task. */
    @Transactional(readOnly = true)
    public boolean isComplete(UUID jobId) {
        GenerationJob job = jobService.getJob(jobId);
        List<GenerationTask> tasks = job.getTasks();
        return tasks == null || tasks.isEmpty()
                || tasks.stream().allMatch(t -> t.getStatus() == TaskStatus.COMPLETE);
    }

    @Transactional(readOnly = true)
    public NotesPayload getNotes(UUID jobId) {
        GenerationJob job = jobService.getJob(jobId);
        String roadmapName = job.getRoadmap() != null ? job.getRoadmap().getTitle() : "Untitled";

        List<NoteItem> notes = new ArrayList<>();
        List<GenerationTask> tasks = job.getTasks();
        if (tasks != null) {
            for (GenerationTask task : tasks) {
                if (task.getStatus() != TaskStatus.COMPLETE || task.getChapter() == null) continue;
                String chapterName = task.getChapter().getName();
                Path file = storageService.getNotePath(roadmapName, chapterName, chapterName + ".md");
                if (Files.exists(file)) {
                    try {
                        notes.add(new NoteItem(chapterName, Files.readString(file)));
                    } catch (Exception ignored) {
                        // skip unreadable file
                    }
                }
            }
        }

        String merged = notes.isEmpty() ? null : notes.stream()
                .map(n -> "# " + n.name() + "\n\n" + n.content())
                .reduce((a, b) -> a + "\n\n---\n\n" + b)
                .orElse(null);

        return new NotesPayload(merged, notes);
    }

    public record NoteItem(String name, String content) {}
    public record NotesPayload(String merged, List<NoteItem> notes) {}
}
