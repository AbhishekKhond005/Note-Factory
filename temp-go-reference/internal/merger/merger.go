package merger

import (
	"fmt"
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
)

// MergeChapterNotes concatenates all markdown note files in a chapter directory
// into a single chapter-level file.
func MergeChapterNotes(outputDir, chapterName string) (string, error) {
	chapterDir := filepath.Join(outputDir, sanitizeName(chapterName))

	// Read all files in the chapter directory
	entries, err := os.ReadDir(chapterDir)
	if err != nil {
		return "", fmt.Errorf("reading chapter directory %q: %w", chapterDir, err)
	}

	// Collect markdown files, excluding any previously-merged chapter file
	// so a re-run doesn't merge the old merge into itself
	var mdFiles []string
	for _, entry := range entries {
		if entry.IsDir() {
			continue
		}
		if strings.HasSuffix(entry.Name(), ".md") && !strings.Contains(entry.Name(), "-merged.md") {
			mdFiles = append(mdFiles, filepath.Join(chapterDir, entry.Name()))
		}
	}

	if len(mdFiles) == 0 {
		return "", fmt.Errorf("no markdown files found in %q", chapterDir)
	}

	// Sort files for consistent ordering. Files are numbered in roadmap
	// order (NN-name.md), so a plain sort preserves the roadmap order.
	sort.Strings(mdFiles)

	// Build the merged content
	var merged strings.Builder
	merged.WriteString(fmt.Sprintf("# %s\n\n", chapterName))
	merged.WriteString(fmt.Sprintf("> Merged study notes for **%s**\n\n", chapterName))
	merged.WriteString("---\n\n")

	for i, filePath := range mdFiles {
		data, err := os.ReadFile(filePath)
		if err != nil {
			return "", fmt.Errorf("reading %q: %w", filePath, err)
		}

		content := string(data)
		// Add separator between files (except the first)
		if i > 0 {
			merged.WriteString("\n\n---\n\n")
		}
		merged.WriteString(content)
	}

	// Name the merged file with a number after all subtopic files, so it
	// always sorts at the bottom of the folder. The number is one greater
	// than the largest subtopic file number found.
	mergedNum := 0
	for _, filePath := range mdFiles {
		base := filepath.Base(filePath)
		var num int
		if _, err := fmt.Sscanf(base, "%d", &num); err == nil && num > mergedNum {
			mergedNum = num
		}
	}
	mergedNum++

	width := 2
	if digits := len(strconv.Itoa(mergedNum)); digits > width {
		width = digits
	}
	outputPath := filepath.Join(chapterDir, fmt.Sprintf("%0*d-%s-merged.md", width, mergedNum, sanitizeName(chapterName)))
	if err := os.WriteFile(outputPath, []byte(merged.String()), 0644); err != nil {
		return "", fmt.Errorf("writing merged file: %w", err)
	}

	return outputPath, nil
}

// sanitizeName makes a string safe for use as a filename.
func sanitizeName(name string) string {
	name = strings.ReplaceAll(name, " ", "_")
	name = strings.ReplaceAll(name, "/", "-")
	name = strings.ReplaceAll(name, "\\", "-")
	return name
}
