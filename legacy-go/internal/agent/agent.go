package agent

import (
	"bytes"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"strings"
)

// Config holds the agent configuration.
type Config struct {
	// OutputDir is the directory where notes will be written.
	OutputDir string
	// Format is the output format ("md" or "pdf").
	Format string
	// OpencodePath is the path to the opencode binary.
	OpencodePath string
	// Model is the model to use (e.g., "anthropic/claude-sonnet-4-20250514").
	// Empty means use default.
	Model string
	// MaxParallel is the max concurrent opencode processes (default 4).
	MaxParallel int
	// UseDocker controls whether to execute opencode in a Docker container.
	UseDocker bool
	// Proxy is an optional HTTP proxy to pass to Docker containers.
	Proxy string
}

// GenerateRoadmap asks opencode to create a tree-structured study roadmap for a topic.
// The optional userPrompt is treated as priority guidance.
// Returns the roadmap tree text (code-fence stripped), ready to be saved & parsed.
func GenerateRoadmap(cfg *Config, topic, userPrompt string) (string, error) {
	workDir, err := os.MkdirTemp("", "note-factory-*")
	if err != nil {
		return "", fmt.Errorf("creating temp work dir: %w", err)
	}
	defer os.RemoveAll(workDir)

	prompt := fmt.Sprintf(`You are an expert curriculum designer. Create a detailed, well-organized study roadmap for the topic below.

The roadmap MUST use EXACTLY this tree format, with box-drawing characters and a root folder named after the topic:

    <topic>-roadmap/
    ├── 01-first-chapter/
    │   ├── first-subtopic/
    │   │   ├── specific point to learn
    │   │   └── another specific point
    │   └── second-subtopic/
    │       └── ...
    └── 02-second-chapter/
        └── ...

Strict format rules:
1. The root folder line is the topic name followed by "-roadmap/".
2. Top level (├── / └── at 0 indentation): 8 to 16 chapters, numbered 01, 02, 03, ... in a sensible learning order (foundations first).
3. Second level (4-space indent under each chapter): 3 to 6 sub-topics.
4. Third level (8-space indent): 3 to 5 concrete, specific learning points (short phrases, no trailing slashes).
5. Do NOT nest deeper than 3 levels.
6. Use "├── " for all but the last item at each level, and "└── " for the last item.
7. No commentary, no explanations, no extra text — ONLY the tree.

Topic: %s`, topic)

	prompt = appendUserPrompt(prompt, userPrompt)

	fmt.Printf("  Generating roadmap for %q...\n", topic)
	out, err := runOpencode(cfg, workDir, prompt)
	if err != nil {
		return "", fmt.Errorf("generating roadmap: %w", err)
	}

	out = cleanOutput(out)
	// The tree may be wrapped in a code block — strip it
	out = extractFromCodeBlock(out)
	return strings.TrimSpace(out), nil
}

// appendUserPrompt appends the user's priority guidance to a prompt when provided.
func appendUserPrompt(prompt, userPrompt string) string {
	userPrompt = strings.TrimSpace(userPrompt)
	if userPrompt == "" {
		return prompt
	}
	return prompt + fmt.Sprintf("\n\nPRIORITY USER GUIDANCE (this has the highest priority; follow it strictly wherever it conflicts with the instructions above):\n%s", userPrompt)
}

// overviewSystemPrompt is the hardcoded quick-overview prompt. It asks for
// SHORT, EXTREMELY SIMPLE notes covering what the domain is about — not the
// workings or in-depth concepts — so the result is typically a single file.
const overviewSystemPrompt = `You are an expert educator creating a quick overview of a domain for a complete beginner.

Write SHORT and EXTREMELY SIMPLE notes that cover what the domain "{{TOPIC}}" is about: what it is, why people use it, and the main high-level areas it touches. This is a bird's-eye overview, NOT a deep dive.

Rules:
- Keep the whole document short (about 300–600 words).
- Use plain, beginner-friendly language. If jargon is needed, explain it in one short phrase.
- Cover ONLY: what the domain is, what it is used for / why it matters, and the main areas or subfields it includes.
- Do NOT explain inner workings, internals, or in-depth concepts.
- Do NOT include code examples, exercises, or implementation details.
- Structure the notes with a title, a few short section headings, and short bullet points.

Return the COMPLETE markdown notes directly in your response. Do NOT write any files. Do NOT include any extra text, greetings, or commentary.`

// GenerateOverview runs a single opencode call with the hardcoded quick-overview
// prompt for a topic. The optional userPrompt is appended as priority guidance.
// Returns the markdown notes content (typically one short notes file).
func GenerateOverview(cfg *Config, topic, userPrompt string) (string, error) {
	workDir, err := os.MkdirTemp("", "note-factory-*")
	if err != nil {
		return "", fmt.Errorf("creating temp work dir: %w", err)
	}
	defer os.RemoveAll(workDir)

	prompt := strings.ReplaceAll(overviewSystemPrompt, "{{TOPIC}}", topic)
	prompt = appendUserPrompt(prompt, userPrompt)

	fmt.Printf("  Generating quick overview for %q...\n", topic)
	out, err := runOpencode(cfg, workDir, prompt)
	if err != nil {
		return "", fmt.Errorf("generating overview: %w", err)
	}

	content := cleanOutput(out)
	if isSummaryResponse(content) {
		fmt.Printf("  Detected summary response, looking for files in workdir...\n")
		if saved := findAndReadOutputFile(workDir); saved != "" {
			content = saved
		}
	}
	return content, nil
}

// GenerateNotesForSubChapter runs the agentic flow for a single sub-chapter.
// Step 1: Ask opencode to generate a prompt/outline for the topic.
// Step 2: Feed that prompt back to generate comprehensive notes.
// order is the 1-based position of this sub-chapter in the roadmap
// (sub-chapters are numbered in the order they appear in the roadmap),
// and total is the total number of sub-chapters in the chapter.
// userPrompt is optional priority guidance appended at every AI step.
// Returns the path to the generated file.
func GenerateNotesForSubChapter(cfg *Config, chapterName, subChapterName string, topics []string, order, total int, userPrompt string) (string, error) {
	// Create output directory for this chapter
	chapterDir := filepath.Join(cfg.OutputDir, sanitizeName(chapterName))
	if err := os.MkdirAll(chapterDir, 0755); err != nil {
		return "", fmt.Errorf("creating chapter directory: %w", err)
	}

	// Build topic description
	topicDesc := buildTopicDescription(subChapterName, topics)

	// Create a temporary working directory for opencode
	workDir, err := os.MkdirTemp("", "note-factory-*")
	if err != nil {
		return "", fmt.Errorf("creating temp work dir: %w", err)
	}
	defer os.RemoveAll(workDir)

	// Step 1: Generate a prompt template
	fmt.Printf("  [Step 1/2] Generating prompt template for %q...\n", subChapterName)
	promptTemplate, err := generatePromptTemplate(cfg, workDir, topicDesc, userPrompt)
	if err != nil {
		return "", fmt.Errorf("generating prompt template: %w", err)
	}

	promptTemplate = cleanOutput(promptTemplate)

	// Step 2: Use the prompt template to generate notes
	fmt.Printf("  [Step 2/2] Generating notes for %q...\n", subChapterName)

	finalPrompt := fillPromptTemplate(promptTemplate, topicDesc)
	notesContent, err := generateNotes(cfg, workDir, topicDesc, finalPrompt, userPrompt)
	if err != nil {
		return "", fmt.Errorf("generating notes: %w", err)
	}

	notesContent = cleanOutput(notesContent)

	// If the output is still a summary (too short, mentions "written to file"), 
	// look for files opencode may have written in the workdir
	if isSummaryResponse(notesContent) {
		fmt.Printf("  Detected summary response, looking for files in workdir...\n")
		savedContent := findAndReadOutputFile(workDir)
		if savedContent != "" {
			notesContent = savedContent
		}
	}

	// Write the output file, numbered by roadmap order so files sort
	// in the same order they appear in the roadmap
	filename := orderPrefix(order, total) + "-" + sanitizeName(subChapterName) + ".md"
	outputPath := filepath.Join(chapterDir, filename)
	if err := os.WriteFile(outputPath, []byte(notesContent), 0644); err != nil {
		return "", fmt.Errorf("writing notes file: %w", err)
	}

	fmt.Printf("  ✓ Saved to %s (%d bytes)\n", outputPath, len(notesContent))
	return outputPath, nil
}

// buildTopicDescription creates a formatted description of the topic.
func buildTopicDescription(subChapterName string, topics []string) string {
	readableName := strings.ReplaceAll(subChapterName, "-", " ")
	readableName = strings.ReplaceAll(readableName, "_", " ")

	var b strings.Builder
	b.WriteString(readableName)
	if len(topics) > 0 {
		b.WriteString("\n\nSpecific areas to cover:\n")
		for _, t := range topics {
			cleanTopic := strings.ReplaceAll(t, "-", " ")
			b.WriteString(fmt.Sprintf("  - %s\n", cleanTopic))
		}
	}
	return b.String()
}

// generatePromptTemplate asks opencode to create a detailed prompt template for the topic.
func generatePromptTemplate(cfg *Config, workDir string, topicDesc string, userPrompt string) (string, error) {
	prompt := fmt.Sprintf(`You are a curriculum designer creating a prompt template for a world-class textbook author.

For the topic below, generate a detailed, structured prompt template that would produce exceptional textbook-quality notes. The notes should use Java as the programming language for all examples.

The prompt template should:
1. Specify the exact structure (sections, subsections)
2. Request Java code examples with proper syntax
3. Ask for clear explanations with analogies
4. Request proper formatting with headings, code blocks, tables
5. Ask for real-world context and use cases
6. Specify depth appropriate for the topic

Use "{{TOPIC}}" as a placeholder for the actual topic name.

IMPORTANT: Return ONLY the prompt template itself, wrapped in a markdown code block. Do NOT write any files. Do NOT include any extra text, greetings, or commentary.

Topic:
%s`, topicDesc)

	prompt = appendUserPrompt(prompt, userPrompt)
	return runOpencode(cfg, workDir, prompt)
}

// generateNotes uses the prompt template to generate comprehensive notes.
func generateNotes(cfg *Config, workDir string, topicDesc string, finalPrompt string, userPrompt string) (string, error) {
	fullPrompt := fmt.Sprintf(`You are a world-class textbook author writing comprehensive Java study notes.

Follow the prompt below precisely to produce exceptional, textbook-quality notes. Use Java for all code examples.

Keep the notes focused and concise: dense, useful content with no filler, no repetition, and no padded introductions or conclusions. Every sentence should teach something.

IMPORTANT: Return the COMPLETE notes directly in your response. Do NOT write any files. Do NOT save to a file. Just respond with the full Markdown content.

%s

Topic to cover:
%s`, finalPrompt, topicDesc)

	fullPrompt = appendUserPrompt(fullPrompt, userPrompt)
	return runOpencode(cfg, workDir, fullPrompt)
}

// fillPromptTemplate replaces {{TOPIC}} placeholders with the actual topic.
func fillPromptTemplate(template string, topicDesc string) string {
	lines := strings.SplitN(topicDesc, "\n", 2)
	topicName := strings.TrimSpace(lines[0])

	result := strings.ReplaceAll(template, "{{TOPIC}}", topicName)
	result = strings.ReplaceAll(result, "{{topic}}", topicName)
	return result
}

// isSummaryResponse checks if the output looks like a summary rather than full notes.
func isSummaryResponse(output string) bool {
	lower := strings.ToLower(output)
	summaryIndicators := []string{
		"written successfully",
		"has been written",
		"saved to",
		"here's a summary",
		"word count:",
	}
	for _, indicator := range summaryIndicators {
		if strings.Contains(lower, indicator) {
			return true
		}
	}
	return len(output) < 500
}

// findAndReadOutputFile looks for markdown files in the workdir that opencode may have written.
func findAndReadOutputFile(workDir string) string {
	entries, err := os.ReadDir(workDir)
	if err != nil {
		return ""
	}
	for _, entry := range entries {
		if !entry.IsDir() && strings.HasSuffix(entry.Name(), ".md") {
			data, err := os.ReadFile(filepath.Join(workDir, entry.Name()))
			if err == nil && len(data) > 500 {
				return string(data)
			}
		}
	}
	return ""
}

// runOpencode executes opencode with the given prompt and returns the output.
func runOpencode(cfg *Config, workDir string, prompt string) (string, error) {
	// Drop a lean config into the workdir: auto-compaction keeps the agent's
	// in-memory context (and therefore its RSS) bounded on long generations,
	// and autoupdate off avoids a version-check network call on startup.
	prepareWorkDir(workDir)

	// If UseDocker is strictly enforced, only use Docker
	if cfg.UseDocker {
		fmt.Println("  [Docker Mode] Executing in container...")
		return executeDocker(cfg, workDir, prompt)
	}

	// Try native first
	out, err := executeNative(cfg, workDir, prompt)
	if err != nil {
		// Check for common quota/rate limit error strings in the error message
		errMsg := strings.ToLower(err.Error())
		isQuotaError := strings.Contains(errMsg, "quota") || 
			strings.Contains(errMsg, "rate limit") || 
			strings.Contains(errMsg, "429") ||
			strings.Contains(errMsg, "too many requests") ||
			strings.Contains(errMsg, "payment required") ||
			strings.Contains(errMsg, "exhausted")

		if isQuotaError {
			fmt.Println("  ⚠️ Quota limit detected! Falling back to Docker container bypass...")
			return executeDocker(cfg, workDir, prompt)
		}
		
		return "", err
	}

	return out, nil
}

// workDirConfig is written into every opencode workdir. Compaction bounds the
// session context (and process memory) on long generations; autoupdate off
// skips the startup version check that costs time on slow instances.
const workDirConfig = `{
  "$schema": "https://opencode.ai/config.json",
  "autoupdate": false,
  "compaction": { "auto": true, "tail_turns": 5 }
}`

func prepareWorkDir(workDir string) {
	if workDir == "" {
		return
	}
	_ = os.WriteFile(filepath.Join(workDir, "opencode.jsonc"), []byte(workDirConfig), 0644)
}

// maxOutputBytes caps how much opencode stdout/stderr we buffer in memory.
// A runaway or misbehaving agent dump can otherwise blow up the Go heap.
const maxOutputBytes = 16 << 20 // 16 MiB

// limitedBuffer is an io.Writer that keeps at most maxOutputBytes and
// silently drops the rest (recording whether truncation happened).
type limitedBuffer struct {
	buf       bytes.Buffer
	limit     int
	truncated bool
}

func (b *limitedBuffer) Write(p []byte) (int, error) {
	remaining := b.limit - b.buf.Len()
	if remaining > 0 {
		if len(p) > remaining {
			b.buf.Write(p[:remaining])
			b.truncated = true
		} else {
			b.buf.Write(p)
		}
	} else {
		b.truncated = true
	}
	return len(p), nil
}

func (b *limitedBuffer) String() string { return b.buf.String() }

func executeNative(cfg *Config, workDir string, prompt string) (string, error) {
	args := []string{"run", "--pure", "--dir", workDir}
	if cfg.Model != "" {
		args = append(args, "--model", cfg.Model)
	}
	args = append(args, prompt)

	cmd := exec.Command(cfg.OpencodePath, args...)

	// NO_COLOR keeps the output free of ANSI escape noise (smaller buffers,
	// less cleanup work); the default env is inherited otherwise.
	cmd.Env = append(os.Environ(), "NO_COLOR=1")

	stdout := &limitedBuffer{limit: maxOutputBytes}
	stderr := &limitedBuffer{limit: maxOutputBytes}
	cmd.Stdout = stdout
	cmd.Stderr = stderr

	if err := cmd.Run(); err != nil {
		return "", fmt.Errorf("opencode execution failed: %w\nstderr: %s", err, truncate(stderr.String(), 4000))
	}

	return stdout.String(), nil
}

func executeDocker(cfg *Config, workDir string, prompt string) (string, error) {
	// Resolve absolute path for workDir to mount it
	absWorkDir, err := filepath.Abs(workDir)
	if err != nil {
		return "", fmt.Errorf("resolving absolute workDir for docker mount: %w", err)
	}

	args := []string{
		"run", "--rm",
		"-v", fmt.Sprintf("%s:/work", absWorkDir),
		"-w", "/work",
	}

	if cfg.Proxy != "" {
		args = append(args, "-e", fmt.Sprintf("HTTP_PROXY=%s", cfg.Proxy))
		args = append(args, "-e", fmt.Sprintf("HTTPS_PROXY=%s", cfg.Proxy))
		args = append(args, "-e", fmt.Sprintf("http_proxy=%s", cfg.Proxy))
		args = append(args, "-e", fmt.Sprintf("https_proxy=%s", cfg.Proxy))
	}

	args = append(args, "opencode-runner", "opencode", "run", "--pure", "--dir", "/work")

	if cfg.Model != "" {
		args = append(args, "--model", cfg.Model)
	}
	args = append(args, prompt)

	cmd := exec.Command("docker", args...)
	cmd.Env = append(os.Environ(), "NO_COLOR=1")

	stdout := &limitedBuffer{limit: maxOutputBytes}
	stderr := &limitedBuffer{limit: maxOutputBytes}
	cmd.Stdout = stdout
	cmd.Stderr = stderr

	if err := cmd.Run(); err != nil {
		return "", fmt.Errorf("docker opencode execution failed: %w\nstderr: %s", err, truncate(stderr.String(), 4000))
	}

	return stdout.String(), nil
}

// truncate caps a string's length (used for error messages from subprocesses).
func truncate(s string, maxLen int) string {
	if len(s) <= maxLen {
		return s
	}
	return s[:maxLen] + "...(truncated)"
}

// cleanOutput removes ANSI escape codes and extracts content from code blocks.
func cleanOutput(s string) string {
	s = stripANSIEscapeCodes(s)
	s = extractFromCodeBlock(s)
	return strings.TrimSpace(s)
}

// stripANSIEscapeCodes removes ANSI escape sequences from the output.
func stripANSIEscapeCodes(s string) string {
	var result bytes.Buffer
	i := 0
	for i < len(s) {
		if s[i] == '\033' {
			i++
			for i < len(s) && s[i] != 'm' {
				i++
			}
			i++
		} else {
			result.WriteByte(s[i])
			i++
		}
	}
	return result.String()
}

// extractFromCodeBlock extracts content from a markdown code block if the output is wrapped.
func extractFromCodeBlock(s string) string {
	s = strings.TrimSpace(s)
	if strings.HasPrefix(s, "```") {
		firstNewline := strings.Index(s, "\n")
		if firstNewline > 0 {
			content := s[firstNewline+1:]
			lastIdx := strings.LastIndex(content, "```")
			if lastIdx >= 0 {
				return strings.TrimSpace(content[:lastIdx])
			}
			return strings.TrimSpace(content)
		}
	}
	return s
}

// sanitizeName makes a string safe for use as a filename.
func sanitizeName(name string) string {
	name = strings.ReplaceAll(name, " ", "_")
	name = strings.ReplaceAll(name, "/", "-")
	name = strings.ReplaceAll(name, "\\", "-")
	return name
}

// orderPrefix returns a zero-padded numeric prefix reflecting the sub-chapter's
// position in the roadmap (at least 2 digits, more if the chapter has 100+).
func orderPrefix(order, total int) string {
	width := 2
	if digits := len(strconv.Itoa(total)); digits > width {
		width = digits
	}
	return fmt.Sprintf("%0*d", width, order)
}
