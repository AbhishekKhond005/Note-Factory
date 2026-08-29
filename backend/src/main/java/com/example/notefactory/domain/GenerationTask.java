package com.example.notefactory.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "generation_task")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerationTask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_job_id", nullable = false)
    @ToString.Exclude
    private GenerationJob generationJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_chapter_id")
    private SubChapter subChapter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(name = "agent_role", nullable = false)
    private String agentRole;

    @Column(name = "step_description")
    private String stepDescription;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_attempt_id")
    private GenerationAttempt currentAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "output_artifact_id")
    private Artifact outputArtifact;

    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    @OneToMany(mappedBy = "generationTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GenerationAttempt> attempts;
}
