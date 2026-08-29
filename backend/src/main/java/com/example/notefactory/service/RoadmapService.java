package com.example.notefactory.service;

import com.example.notefactory.domain.Roadmap;
import com.example.notefactory.domain.RoadmapSource;
import com.example.notefactory.repository.RoadmapRepository;
import com.example.notefactory.agent.RoadmapPlannerAgent;
import com.example.notefactory.provider.GenerationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoadmapService {
    private final RoadmapRepository roadmapRepository;
    private final RoadmapParser roadmapParser;
    private final RoadmapPlannerAgent roadmapPlannerAgent;

    public List<Roadmap> getAllRoadmaps() {
        return roadmapRepository.findAll();
    }

    public Roadmap getRoadmap(UUID id) {
        return roadmapRepository.findById(id).orElseThrow(() -> new RuntimeException("Roadmap not found"));
    }

    @Transactional
    public Roadmap uploadRoadmap(String content) {
        Roadmap rm = roadmapParser.parse(content, RoadmapSource.UPLOADED);
        return roadmapRepository.save(rm);
    }

    @Transactional
    public Roadmap generateRoadmap(String topic, String prompt) {
        GenerationResponse response = roadmapPlannerAgent.planRoadmap(topic, prompt);
        if (response.isQuotaError()) {
            throw new RuntimeException("Quota error while generating roadmap");
        }
        Roadmap rm = roadmapParser.parse(response.getText(), RoadmapSource.GENERATED);
        if (rm.getTitle() == null || rm.getTitle().equals("Untitled Roadmap")) {
            rm.setTitle(topic);
        }
        return roadmapRepository.save(rm);
    }
}
