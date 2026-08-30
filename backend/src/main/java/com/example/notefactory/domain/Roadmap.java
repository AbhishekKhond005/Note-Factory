package com.example.notefactory.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "roadmap")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Roadmap {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoadmapSource source;

    @JsonIgnore
    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @JsonIgnore
    @Column(name = "parsed_structure", columnDefinition = "TEXT")
    private String parsedStructure;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Chapter> chapters;

    @Column(name = "warning_message", columnDefinition = "TEXT")
    private String warningMessage;
}
