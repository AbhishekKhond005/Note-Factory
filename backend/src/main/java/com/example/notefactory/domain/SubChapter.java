package com.example.notefactory.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sub_chapter")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubChapter {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    @ToString.Exclude
    private Chapter chapter;

    @Column(nullable = false)
    private String name;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(columnDefinition = "TEXT")
    @JsonIgnore
    private String topics;

    /** Exposes topics as a JSON array for the frontend (roadmap visualizer/picker). */
    @JsonProperty("topics")
    public List<String> getTopicsList() {
        if (topics == null || topics.isBlank()) return List.of();
        return Arrays.stream(topics.split(",\\s*"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
