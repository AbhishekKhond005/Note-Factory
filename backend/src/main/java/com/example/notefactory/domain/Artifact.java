package com.example.notefactory.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "artifact")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artifact {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "logical_name", nullable = false)
    private String logicalName;

    @Column(name = "storage_location", nullable = false, columnDefinition = "TEXT")
    private String storageLocation;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "checksum")
    private String checksum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_chapter_artifact_id")
    private Artifact mergedChapterArtifact;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
