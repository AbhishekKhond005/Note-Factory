package parser

import (
	"fmt"
	"os"
	"strings"

	"github.com/Note_Factory/internal/types"
)

const maxDepth = 2 // depth 0=chapter, 1=sub-chapter, 2=topic (sub-sub-chapter)

// ParseFile reads a roadmap file and returns the parsed structure.
func ParseFile(path string) (*types.RoadMap, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("reading roadmap file: %w", err)
	}
	return Parse(string(data))
}

// Parse parses a tree-structured roadmap text into structured data.
func Parse(input string) (*types.RoadMap, error) {
	lines := strings.Split(input, "\n")

	rm := &types.RoadMap{}
	var currentChapter *types.Chapter
	var currentSubChapter *types.SubChapter

	for i, line := range lines {
		trimmed := strings.TrimRight(line, " \t\r")
		if trimmed == "" {
			continue
		}

		// Skip the fenced code block markers
		if strings.HasPrefix(strings.TrimSpace(trimmed), "```") {
			continue
		}

		depth, name := parseTreeLine(trimmed)
		if depth < 0 {
		// This is the root title line (no tree prefix)
		if rm.Title == "" {
			title := strings.TrimRight(name, "/")
			title = strings.TrimPrefix(title, "# ")
			title = strings.TrimPrefix(title, "#")
			rm.Title = strings.TrimSpace(title)
		}
			continue
		}

		// Strip trailing slash used for directories
		name = strings.TrimRight(name, "/")
		name = strings.TrimSpace(name)
		if name == "" {
			continue
		}

		switch depth {
		case 0: // Chapter
			currentChapter = &types.Chapter{
				Name: name,
			}
			rm.Chapters = append(rm.Chapters, *currentChapter)
			// Reset sub-chapter pointer since we're at a new chapter
			currentSubChapter = nil

		case 1: // Sub-chapter
			if currentChapter == nil {
				return nil, fmt.Errorf("line %d: sub-chapter %q found before any chapter", i+1, name)
			}
			sc := types.SubChapter{
				Name: name,
			}
			idx := len(rm.Chapters) - 1
			rm.Chapters[idx].SubChapters = append(rm.Chapters[idx].SubChapters, sc)
			// Point to the newly added element
			subs := rm.Chapters[idx].SubChapters
			currentSubChapter = &subs[len(subs)-1]

		case 2: // Topic (sub-sub-chapter)
			if currentSubChapter == nil {
				return nil, fmt.Errorf("line %d: topic %q found before any sub-chapter", i+1, name)
			}
			currentSubChapter.Topics = append(currentSubChapter.Topics, name)

		default:
			return nil, fmt.Errorf("line %d: nesting depth %d exceeds maximum depth of %d", i+1, depth, maxDepth)
		}
	}

	return rm, nil
}

// parseTreeLine extracts depth and name from a tree-format line.
// Returns depth (-1 if not a tree line) and the item name.
func parseTreeLine(line string) (int, string) {
	// The tree format uses these Unicode box-drawing characters:
	//   ├ (U+251C), ─ (U+2500), └ (U+2514), │ (U+2502)
	// Each indent level consists of either "│   " (│ + 3 spaces) or "    " (4 spaces)
	// Each item marker is either "├── " or "└── "
	// In terms of rune count: indent = 4 runes, marker = 4 runes

	runes := []rune(line)

	// Find where the tree marker starts — look for ├ or └
	markerStart := -1
	for j, r := range runes {
		if r == '├' || r == '└' {
			markerStart = j
			break
		}
	}

	if markerStart < 0 {
		// Not a tree line — could be the root title
		return -1, strings.TrimSpace(line)
	}

	// The marker should be followed by "── " (2 dashes + space)
	// Skip past the marker (├── ) — that's 4 runes total
	if markerStart+4 > len(runes) {
		return -1, strings.TrimSpace(line)
	}

	name := string(runes[markerStart+4:])

	// Depth is the number of 4-rune indent blocks before the marker
	// Each indent block is either "│   " or "    "
	if markerStart%4 != 0 {
		// Shouldn't happen with well-formed tree output, but handle gracefully
		depth := markerStart / 4
		return depth, name
	}

	depth := markerStart / 4
	return depth, name
}
