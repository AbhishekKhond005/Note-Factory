package com.example.notefactory.service;

import com.example.notefactory.domain.Chapter;
import com.example.notefactory.domain.Roadmap;
import com.example.notefactory.domain.RoadmapSource;
import com.example.notefactory.domain.SubChapter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class RoadmapParser {

    private static final int MAX_DEPTH = 2; // depth 0=chapter, 1=sub-chapter, 2=topic

    public Roadmap parse(String input, RoadmapSource source) {
        String[] lines = input.split("\\n");

        Roadmap rm = new Roadmap();
        rm.setSource(source);
        rm.setRawText(input);
        rm.setChapters(new ArrayList<>());
        
        Chapter currentChapter = null;
        SubChapter currentSubChapter = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.stripTrailing();
            if (trimmed.isEmpty()) {
                continue;
            }

            // Skip fenced code block markers
            if (trimmed.trim().startsWith("```")) {
                continue;
            }

            TreeLine parsedLine = parseTreeLine(trimmed);
            if (parsedLine.depth < 0) {
                // Root title line
                if (rm.getTitle() == null) {
                    String title = parsedLine.name;
                    if (title.endsWith("/")) title = title.substring(0, title.length() - 1);
                    if (title.startsWith("# ")) title = title.substring(2);
                    if (title.startsWith("#")) title = title.substring(1);
                    rm.setTitle(title.trim());
                }
                continue;
            }

            String name = cleanName(parsedLine.name);
            if (name.isEmpty()) {
                continue;
            }

            switch (parsedLine.depth) {
                case 0:
                    currentChapter = new Chapter();
                    currentChapter.setName(name);
                    currentChapter.setOrderIndex(rm.getChapters().size());
                    currentChapter.setSubChapters(new ArrayList<>());
                    currentChapter.setRoadmap(rm);
                    rm.getChapters().add(currentChapter);
                    currentSubChapter = null;
                    break;
                case 1:
                    if (currentChapter == null) {
                        throw new IllegalArgumentException("Line " + (i + 1) + ": sub-chapter '" + name + "' found before any chapter");
                    }
                    currentSubChapter = new SubChapter();
                    currentSubChapter.setName(name);
                    currentSubChapter.setOrderIndex(currentChapter.getSubChapters().size());
                    currentSubChapter.setTopics("");
                    currentSubChapter.setChapter(currentChapter);
                    currentChapter.getSubChapters().add(currentSubChapter);
                    break;
                case 2:
                    if (currentSubChapter == null) {
                        throw new IllegalArgumentException("Line " + (i + 1) + ": topic '" + name + "' found before any sub-chapter");
                    }
                    String existing = currentSubChapter.getTopics();
                    currentSubChapter.setTopics(existing == null || existing.isEmpty() ? name : existing + ", " + name);
                    break;
                default:
                    throw new IllegalArgumentException("Line " + (i + 1) + ": nesting depth " + parsedLine.depth + " exceeds maximum depth of " + MAX_DEPTH);
            }
        }
        
        if (rm.getTitle() == null || rm.getTitle().isEmpty()) {
            rm.setTitle("Untitled Roadmap");
        }

        return rm;
    }

    private TreeLine parseTreeLine(String line) {
        char[] chars = line.toCharArray();
        int markerStart = -1;
        for (int j = 0; j < chars.length; j++) {
            if (chars[j] == '├' || chars[j] == '└') {
                markerStart = j;
                break;
            }
        }

        if (markerStart < 0) {
            return new TreeLine(-1, line.trim());
        }

        if (markerStart + 4 > chars.length) {
            return new TreeLine(-1, line.trim());
        }

        String name = new String(chars, markerStart + 4, chars.length - (markerStart + 4));

        if (markerStart % 4 != 0) {
            return new TreeLine(markerStart / 4, name);
        }

        return new TreeLine(markerStart / 4, name);
    }

    private String cleanName(String name) {
        name = name.stripTrailing();
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1).trim();
        }
        if (name.isEmpty()) return "";

        int sepIndex = -1;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '-' || c == '_' || c == '.' || c == ' ') {
                sepIndex = i;
                break;
            }
        }

        if (sepIndex > 0 && isAllDigits(name.substring(0, sepIndex))) {
            name = name.substring(sepIndex + 1);
        }

        String lower = name.toLowerCase();
        for (String ext : new String[]{".markdown", ".md", ".txt"}) {
            if (lower.endsWith(ext)) {
                name = name.substring(0, name.length() - ext.length());
                break;
            }
        }

        return name.trim();
    }

    private boolean isAllDigits(String s) {
        if (s == null || s.isEmpty()) return false;
        for (char c : s.toCharArray()) {
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    private record TreeLine(int depth, String name) {}
}
