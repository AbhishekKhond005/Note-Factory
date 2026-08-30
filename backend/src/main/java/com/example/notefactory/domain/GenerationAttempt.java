package com.example.notefactory.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "generation_attempt")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerationAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_task_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private GenerationTask generationTask;

    @Column(name = "provider")
    private String provider;

    @Column(name = "model")
    private String model;

    @Column(name = "prompt_reference", columnDefinition = "TEXT")
    private String promptReference;

    @Column(name = "response_reference", columnDefinition = "TEXT")
    private String responseReference;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome")
    private AttemptOutcome outcome;

    @Column(name = "validation_notes", columnDefinition = "TEXT")
    private String validationNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
