package com.example.notefactory.repository;

import com.example.notefactory.domain.GenerationTask;
import com.example.notefactory.domain.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface GenerationTaskRepository extends JpaRepository<GenerationTask, UUID> {
    List<GenerationTask> findByStatus(TaskStatus status);
}
