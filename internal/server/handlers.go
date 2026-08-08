package server

import (
	"archive/zip"
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"sync"

	"github.com/go-chi/chi/v5"

	"github.com/Note_Factory/internal/agent"
	"github.com/Note_Factory/internal/merger"
	"github.com/Note_Factory/internal/parser"
	"github.com/Note_Factory/internal/types"
)

// ── Request / Response types ────────────────────────────────────────

type parseRoadmapRequest struct {
	Content string `json:"content"` // Raw roadmap text
}

type generateRoadmapRequest struct {
	Topic  string `json:"topic"`
	Prompt string `json:"prompt,omitempty"`
}

type generateRequest struct {
	RoadmapContent string `json:"roadmapContent,omitempty"` // raw text if not using pre-loaded
	RoadmapFile    string `json:"roadmapFile,omitempty"`    // filename of a pre-loaded roadmap
	ChapterIndex   int    `json:"chapterIndex"`             // 0-based chapter index
	Prompt         string `json:"prompt,omitempty"`         // optional user priority guidance
}

type overviewRequest struct {
	Topic  string `json:"topic"`
	Prompt string `json:"prompt,omitempty"`
}

type errorResponse struct {
	Error   string `json:"error"`
	Details string `json:"details,omitempty"`
}

type systemStatus struct {
	ActiveJobs    int `json:"activeJobs"`
	MaxParallel   int `json:"maxParallel"`
	QueuedJobs    int `json:"queuedJobs"`
	WSClients     int `json:"wsClients"`
	TotalJobsRun  int `json:"totalJobsRun"`
}

// ── Helpers ─────────────────────────────────────────────────────────

func respondJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}

func respondError(w http.ResponseWriter, status int, msg string) {
	respondJSON(w, status, errorResponse{Error: msg})
}

// ── Handlers ────────────────────────────────────────────────────────

// GET /api/health
func (s *Server) handleHealth(w http.ResponseWriter, r *http.Request) {
	respondJSON(w, http.StatusOK, map[string]string{
		"status":  "ok",
		"service": "note-factory",
	})
}

// GET /api/status
func (s *Server) handleSystemStatus(w http.ResponseWriter, r *http.Request) {
	allJobs := s.jobManager.List()
	active := 0
	queued := 0
	for _, j := range allJobs {
		switch j.Status {
		case types.JobStatusRunning:
			active++
		case types.JobStatusPending, types.JobStatusQueued:
			queued++
		}
	}

	respondJSON(w, http.StatusOK, systemStatus{
		ActiveJobs:   active,
		MaxParallel:  s.agentConfig.MaxParallel,
		QueuedJobs:   queued,
		WSClients:    s.hub.ClientCount(),
		TotalJobsRun: len(allJobs),
	})
}

// GET /api/roadmaps — list pre-loaded roadmap files
func (s *Server) handleListRoadmaps(w http.ResponseWriter, r *http.Request) {
	// Scan for roadmap files in the roadmaps directory AND current directory
	type roadmapInfo struct {
		Name     string `json:"name"`
		Filename string `json:"filename"`
		Path     string `json:"path"`
	}

	var roadmaps []roadmapInfo
	seen := map[string]bool{}

	// Check roadmaps/ directory
	scanDir := func(dir string) {
		entries, err := os.ReadDir(dir)
		if err != nil {
			return
		}
		for _, entry := range entries {
			if entry.IsDir() {
				continue
			}
			name := entry.Name()
			ext := strings.ToLower(filepath.Ext(name))
			if ext == ".txt" || ext == ".md" {
				if !seen[name] {
					seen[name] = true
					roadmaps = append(roadmaps, roadmapInfo{
						Name:     strings.TrimSuffix(name, ext),
						Filename: name,
						Path:     filepath.Join(dir, name),
					})
				}
			}
		}
	}

	scanDir(s.roadmapDir)
	scanDir(".")

	if roadmaps == nil {
		roadmaps = []roadmapInfo{}
	}
	respondJSON(w, http.StatusOK, roadmaps)
}

// POST /api/roadmaps/parse — parse roadmap text
func (s *Server) handleParseRoadmap(w http.ResponseWriter, r *http.Request) {
	var req parseRoadmapRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "Invalid JSON body")
		return
	}

	if req.Content == "" {
		respondError(w, http.StatusBadRequest, "Content is required")
		return
	}

	rm, err := parser.Parse(req.Content)
	if err != nil {
		respondError(w, http.StatusBadRequest, fmt.Sprintf("Failed to parse roadmap: %v", err))
		return
	}

	respondJSON(w, http.StatusOK, rm)
}

// POST /api/roadmaps/generate — create a roadmap for a topic using AI
func (s *Server) handleGenerateRoadmap(w http.ResponseWriter, r *http.Request) {
	var req generateRoadmapRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "Invalid JSON body")
		return
	}

	req.Topic = strings.TrimSpace(req.Topic)
	if req.Topic == "" {
		respondError(w, http.StatusBadRequest, "Topic is required")
		return
	}

	tree, err := agent.GenerateRoadmap(s.agentConfig, req.Topic, req.Prompt)
	if err != nil {
		respondError(w, http.StatusInternalServerError, fmt.Sprintf("Failed to generate roadmap: %v", err))
		return
	}

	// Save the generated roadmap inside the project's roadmaps directory
	os.MkdirAll(s.roadmapDir, 0755)
	filename := sanitizeFilename(req.Topic) + "-roadmap.txt"
	content := fmt.Sprintf("# %s Roadmap\n\n```text\n%s\n```\n", req.Topic, tree)
	if err := os.WriteFile(filepath.Join(s.roadmapDir, filename), []byte(content), 0644); err != nil {
		respondError(w, http.StatusInternalServerError, "Failed to save generated roadmap")
		return
	}

	rm, err := parser.Parse(content)
	if err != nil {
		respondError(w, http.StatusBadRequest, fmt.Sprintf("Generated roadmap could not be parsed: %v", err))
		return
	}

	respondJSON(w, http.StatusOK, map[string]interface{}{
		"message":  "Roadmap generated and saved",
		"filename": filename,
		"roadmap":  rm,
	})
}

// POST /api/roadmaps/upload — upload a roadmap file
func (s *Server) handleUploadRoadmap(w http.ResponseWriter, r *http.Request) {
	r.ParseMultipartForm(10 << 20) // 10 MB max

	file, header, err := r.FormFile("roadmap")
	if err != nil {
		respondError(w, http.StatusBadRequest, "No file uploaded")
		return
	}
	defer file.Close()

	// Read content
	data, err := io.ReadAll(file)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "Failed to read file")
		return
	}

	// Save to roadmaps directory
	os.MkdirAll(s.roadmapDir, 0755)
	destPath := filepath.Join(s.roadmapDir, header.Filename)
	if err := os.WriteFile(destPath, data, 0644); err != nil {
		respondError(w, http.StatusInternalServerError, "Failed to save file")
		return
	}

	// Parse it
	rm, err := parser.Parse(string(data))
	if err != nil {
		respondError(w, http.StatusBadRequest, fmt.Sprintf("File saved but failed to parse: %v", err))
		return
	}

	respondJSON(w, http.StatusOK, map[string]interface{}{
		"message":  "Roadmap uploaded and parsed successfully",
		"filename": header.Filename,
		"roadmap":  rm,
	})
}

// POST /api/generate — start note generation for a chapter
func (s *Server) handleGenerate(w http.ResponseWriter, r *http.Request) {
	var req generateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "Invalid JSON body")
		return
	}

	// Get roadmap content
	var content string
	if req.RoadmapContent != "" {
		content = req.RoadmapContent
	} else if req.RoadmapFile != "" {
		// Try roadmaps/ directory first, then current directory
		paths := []string{
			filepath.Join(s.roadmapDir, req.RoadmapFile),
			req.RoadmapFile,
		}
		for _, p := range paths {
			data, err := os.ReadFile(p)
			if err == nil {
				content = string(data)
				break
			}
		}
		if content == "" {
			respondError(w, http.StatusBadRequest, fmt.Sprintf("Roadmap file %q not found", req.RoadmapFile))
			return
		}
	} else {
		respondError(w, http.StatusBadRequest, "Either roadmapContent or roadmapFile is required")
		return
	}

	// Parse roadmap
	rm, err := parser.Parse(content)
	if err != nil {
		respondError(w, http.StatusBadRequest, fmt.Sprintf("Failed to parse roadmap: %v", err))
		return
	}

	if req.ChapterIndex < 0 || req.ChapterIndex >= len(rm.Chapters) {
		respondError(w, http.StatusBadRequest, fmt.Sprintf("Chapter index %d out of range (0-%d)", req.ChapterIndex, len(rm.Chapters)-1))
		return
	}

	chapter := rm.Chapters[req.ChapterIndex]

	// Create job
	job := s.jobManager.Create(rm.Title, chapter.Name, chapter.SubChapters)

	// Start generation in background
	go s.runGeneration(job.ID, rm.Title, chapter, req.Prompt)

	respondJSON(w, http.StatusAccepted, job)
}

// POST /api/generate/overview — quick overview notes for a topic (single file)
func (s *Server) handleGenerateOverview(w http.ResponseWriter, r *http.Request) {
	var req overviewRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "Invalid JSON body")
		return
	}

	req.Topic = strings.TrimSpace(req.Topic)
	if req.Topic == "" {
		respondError(w, http.StatusBadRequest, "Topic is required")
		return
	}

	// Job with a single "overview" section so the dashboard works as usual
	job := s.jobManager.Create(req.Topic, req.Topic, []types.SubChapter{{Name: "overview"}})

	go s.runOverviewGeneration(job.ID, req.Topic, req.Prompt)

	respondJSON(w, http.StatusAccepted, job)
}

// runOverviewGeneration generates a single quick-overview notes file for a topic.
func (s *Server) runOverviewGeneration(jobID, topic, userPrompt string) {
	s.jobManager.UpdateJobStatus(jobID, types.JobStatusRunning)
	s.hub.Broadcast(types.ProgressEvent{
		JobID:  jobID,
		Type:   "status",
		Status: types.JobStatusRunning,
	})

	outDir := filepath.Join(s.notesDir, sanitizeFilename(topic))
	if err := os.MkdirAll(outDir, 0755); err != nil {
		s.jobManager.SetError(jobID, fmt.Sprintf("creating output directory: %v", err))
		s.hub.Broadcast(types.ProgressEvent{
			JobID:   jobID,
			Type:    "complete",
			Status:  types.JobStatusFailed,
			Message: err.Error(),
		})
		return
	}

	cfg := &agent.Config{
		OutputDir:    outDir,
		Format:       "md",
		OpencodePath: s.agentConfig.OpencodePath,
		Model:        s.agentConfig.Model,
		MaxParallel:  s.agentConfig.MaxParallel,
	}

	s.jobManager.UpdateSubChapter(jobID, "overview", types.JobStatusRunning, "generating overview", "", "")
	s.hub.Broadcast(types.ProgressEvent{
		JobID:      jobID,
		Type:       "progress",
		SubChapter: "overview",
		Status:     types.JobStatusRunning,
		Step:       "generating overview",
	})

	content, err := agent.GenerateOverview(cfg, topic, userPrompt)
	if err != nil {
		s.jobManager.UpdateSubChapter(jobID, "overview", types.JobStatusFailed, "", err.Error(), "")
		s.jobManager.SetError(jobID, err.Error())
		s.hub.Broadcast(types.ProgressEvent{
			JobID:      jobID,
			Type:       "error",
			SubChapter: "overview",
			Status:     types.JobStatusFailed,
			Message:    err.Error(),
		})
		return
	}

	// Write the single notes file
	outputPath := filepath.Join(outDir, "01-overview.md")
	if err := os.WriteFile(outputPath, []byte(content), 0644); err != nil {
		s.jobManager.SetError(jobID, fmt.Sprintf("writing overview file: %v", err))
		return
	}

	s.jobManager.UpdateSubChapter(jobID, "overview", types.JobStatusComplete, "done", "", outputPath)
	s.jobManager.SetMergedFile(jobID, outputPath)
	s.jobManager.UpdateJobStatus(jobID, types.JobStatusComplete)
	s.hub.Broadcast(types.ProgressEvent{
		JobID:      jobID,
		Type:       "complete",
		SubChapter: "overview",
		Status:     types.JobStatusComplete,
		Step:       "done",
	})
	s.hub.Broadcast(types.ProgressEvent{
		JobID:   jobID,
		Type:    "complete",
		Status:  types.JobStatusComplete,
		Message: "Quick overview generated",
	})
}

// GET /api/jobs — list all jobs
func (s *Server) handleListJobs(w http.ResponseWriter, r *http.Request) {
	allJobs := s.jobManager.List()
	if allJobs == nil {
		allJobs = []*types.Job{}
	}
	respondJSON(w, http.StatusOK, allJobs)
}

// GET /api/jobs/{jobID} — get job details
func (s *Server) handleGetJob(w http.ResponseWriter, r *http.Request) {
	jobID := chi.URLParam(r, "jobID")
	job := s.jobManager.Get(jobID)
	if job == nil {
		respondError(w, http.StatusNotFound, "Job not found")
		return
	}
	respondJSON(w, http.StatusOK, job)
}

// POST /api/jobs/{jobID}/cancel — cancel a job
func (s *Server) handleCancelJob(w http.ResponseWriter, r *http.Request) {
	jobID := chi.URLParam(r, "jobID")
	job := s.jobManager.Get(jobID)
	if job == nil {
		respondError(w, http.StatusNotFound, "Job not found")
		return
	}
	s.jobManager.UpdateJobStatus(jobID, types.JobStatusCancelled)
	respondJSON(w, http.StatusOK, map[string]string{"message": "Job cancelled"})
}

// GET /api/notes/{jobID} — get generated notes content
func (s *Server) handleGetNotes(w http.ResponseWriter, r *http.Request) {
	jobID := chi.URLParam(r, "jobID")
	job := s.jobManager.Get(jobID)
	if job == nil {
		respondError(w, http.StatusNotFound, "Job not found")
		return
	}

	if job.Status != types.JobStatusComplete {
		respondError(w, http.StatusBadRequest, fmt.Sprintf("Job is %s, not complete", job.Status))
		return
	}

	type noteFile struct {
		Name    string `json:"name"`
		Content string `json:"content"`
	}

	var notes []noteFile
	for _, sc := range job.SubChapters {
		if sc.Output != "" {
			data, err := os.ReadFile(sc.Output)
			if err == nil {
				notes = append(notes, noteFile{
					Name:    sc.Name,
					Content: string(data),
				})
			}
		}
	}

	// Read merged file if available
	mergedContent := ""
	if job.MergedFile != "" {
		data, err := os.ReadFile(job.MergedFile)
		if err == nil {
			mergedContent = string(data)
		}
	}

	respondJSON(w, http.StatusOK, map[string]interface{}{
		"notes":  notes,
		"merged": mergedContent,
	})
}

// GET /api/notes/{jobID}/download — download merged notes as markdown
func (s *Server) handleDownloadNotes(w http.ResponseWriter, r *http.Request) {
	jobID := chi.URLParam(r, "jobID")
	job := s.jobManager.Get(jobID)
	if job == nil {
		respondError(w, http.StatusNotFound, "Job not found")
		return
	}

	if job.MergedFile == "" {
		respondError(w, http.StatusBadRequest, "No merged file available")
		return
	}

	data, err := os.ReadFile(job.MergedFile)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "Failed to read merged file")
		return
	}

	filename := sanitizeFilename(job.ChapterName) + ".md"
	w.Header().Set("Content-Type", "text/markdown; charset=utf-8")
	w.Header().Set("Content-Disposition", fmt.Sprintf("attachment; filename=%q", filename))
	w.Write(data)
}

// GET /api/notes/{jobID}/download-all — download all generated notes as a ZIP
func (s *Server) handleDownloadAllNotes(w http.ResponseWriter, r *http.Request) {
	jobID := chi.URLParam(r, "jobID")
	job := s.jobManager.Get(jobID)
	if job == nil {
		respondError(w, http.StatusNotFound, "Job not found")
		return
	}

	if job.Status != types.JobStatusComplete {
		respondError(w, http.StatusBadRequest, fmt.Sprintf("Job is %s, not complete", job.Status))
		return
	}

	// Collect all note files (deduped) plus the merged file if present
	var files []string
	seen := map[string]bool{}
	add := func(p string) {
		if p != "" && !seen[p] {
			seen[p] = true
			files = append(files, p)
		}
	}
	for _, sc := range job.SubChapters {
		add(sc.Output)
	}
	add(job.MergedFile)

	if len(files) == 0 {
		respondError(w, http.StatusBadRequest, "No note files available for this job")
		return
	}

	// Build the ZIP in memory
	zipBuf := new(bytes.Buffer)
	zw := zip.NewWriter(zipBuf)

	notesAbs, _ := filepath.Abs(s.notesDir)
	for _, f := range files {
		data, err := os.ReadFile(f)
		if err != nil {
			continue
		}

		// Use the path relative to the notes dir for clean archive entries,
		// falling back to the file base name.
		entryName := filepath.Base(f)
		absFile := f
		if !filepath.IsAbs(absFile) {
			if abs, err := filepath.Abs(f); err == nil {
				absFile = abs
			}
		}
		if rel, err := filepath.Rel(notesAbs, absFile); err == nil && !strings.HasPrefix(rel, "..") {
			entryName = rel
		}
		entryName = filepath.ToSlash(entryName)

		fw, err := zw.Create(entryName)
		if err != nil {
			continue
		}
		fw.Write(data)
	}

	if err := zw.Close(); err != nil {
		respondError(w, http.StatusInternalServerError, "Failed to create ZIP archive")
		return
	}

	zipName := sanitizeFilename(job.ChapterName) + "-notes.zip"
	w.Header().Set("Content-Type", "application/zip")
	w.Header().Set("Content-Disposition", fmt.Sprintf("attachment; filename=%q", zipName))
	w.Write(zipBuf.Bytes())
}

// ── Background generation ───────────────────────────────────────────

func (s *Server) runGeneration(jobID, roadmapTitle string, chapter types.Chapter, userPrompt string) {
	s.jobManager.UpdateJobStatus(jobID, types.JobStatusRunning)
	s.hub.Broadcast(types.ProgressEvent{
		JobID:  jobID,
		Type:   "status",
		Status: types.JobStatusRunning,
	})

	outDir := filepath.Join(s.notesDir, sanitizeFilename(roadmapTitle))
	os.MkdirAll(outDir, 0755)

	cfg := &agent.Config{
		OutputDir:    outDir,
		Format:       "md",
		OpencodePath: s.agentConfig.OpencodePath,
		Model:        s.agentConfig.Model,
		MaxParallel:  s.agentConfig.MaxParallel,
	}

	// Process sub-chapters with concurrency limit
	type result struct {
		subName string
		path    string
		err     error
	}

	results := make(chan result, len(chapter.SubChapters))
	var wg sync.WaitGroup
	sem := make(chan struct{}, cfg.MaxParallel)

	for i, sub := range chapter.SubChapters {
		wg.Add(1)
		go func(i int, sub types.SubChapter) {
			defer wg.Done()

			// Check if job was cancelled
			if j := s.jobManager.Get(jobID); j != nil && j.Status == types.JobStatusCancelled {
				return
			}

			sem <- struct{}{}
			defer func() { <-sem }()

			s.jobManager.UpdateSubChapter(jobID, sub.Name, types.JobStatusRunning, "generating prompt", "", "")
			s.hub.Broadcast(types.ProgressEvent{
				JobID:      jobID,
				Type:       "progress",
				SubChapter: sub.Name,
				Status:     types.JobStatusRunning,
				Step:       "generating prompt",
			})

			path, err := agent.GenerateNotesForSubChapter(cfg, chapter.Name, sub.Name, sub.Topics, i+1, len(chapter.SubChapters), userPrompt)

			if err != nil {
				s.jobManager.UpdateSubChapter(jobID, sub.Name, types.JobStatusFailed, "", err.Error(), "")
				s.hub.Broadcast(types.ProgressEvent{
					JobID:      jobID,
					Type:       "error",
					SubChapter: sub.Name,
					Status:     types.JobStatusFailed,
					Message:    err.Error(),
				})
			} else {
				s.jobManager.UpdateSubChapter(jobID, sub.Name, types.JobStatusComplete, "done", "", path)
				s.hub.Broadcast(types.ProgressEvent{
					JobID:      jobID,
					Type:       "progress",
					SubChapter: sub.Name,
					Status:     types.JobStatusComplete,
					Step:       "done",
				})
			}

			results <- result{subName: sub.Name, path: path, err: err}
		}(i, sub)
	}

	wg.Wait()
	close(results)

	// Collect and merge
	generated := []string{}
	hadError := false
	for r := range results {
		if r.err != nil {
			hadError = true
		} else if r.path != "" {
			generated = append(generated, r.path)
		}
	}

	// Merge
	if len(generated) > 0 {
		mergedPath, err := merger.MergeChapterNotes(outDir, chapter.Name)
		if err == nil {
			s.jobManager.SetMergedFile(jobID, mergedPath)
		}
	}

	// Final status
	if hadError && len(generated) == 0 {
		s.jobManager.UpdateJobStatus(jobID, types.JobStatusFailed)
		s.hub.Broadcast(types.ProgressEvent{
			JobID:   jobID,
			Type:    "complete",
			Status:  types.JobStatusFailed,
			Message: "All sub-chapters failed",
		})
	} else {
		s.jobManager.UpdateJobStatus(jobID, types.JobStatusComplete)
		s.hub.Broadcast(types.ProgressEvent{
			JobID:   jobID,
			Type:    "complete",
			Status:  types.JobStatusComplete,
			Message: fmt.Sprintf("%d/%d sub-chapters generated successfully", len(generated), len(chapter.SubChapters)),
		})
	}
}

func sanitizeFilename(name string) string {
	name = strings.ReplaceAll(name, " ", "_")
	name = strings.ReplaceAll(name, "/", "-")
	name = strings.ReplaceAll(name, "\\", "-")
	return name
}
