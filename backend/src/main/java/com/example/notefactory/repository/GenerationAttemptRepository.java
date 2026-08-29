package com.example.notefactory.repository;

import com.example.notefactory.domain.GenerationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface GenerationAttemptRepository extends JpaRepository<GenerationAttempt, UUID> {
}
