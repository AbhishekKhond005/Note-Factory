package com.example.notefactory.repository;

import com.example.notefactory.domain.SubChapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SubChapterRepository extends JpaRepository<SubChapter, UUID> {
}
