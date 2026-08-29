package types

import "time"

// RoadMap represents the full parsed roadmap
type RoadMap struct {
	Title    string    `json:"title"`
	Chapters []Chapter `json:"chapters"`
}

// Chapter represents a top-level chapter (level 1)
type Chapter struct {
	Name        string       `json:"name"`
	SubChapters []SubChapter `json:"subChapters"`
}

// SubChapter represents a sub-chapter (level 2)
type SubChapter struct {
	Name   string   `json:"name"`
	Topics []string `json:"topics"` // level 3 items (topics within sub-chapter)
}

// FullName returns the dotted hierarchical name (e.g., "00-start-here.how-java-runs")
func (s SubChapter) FullName(parent string) string {
	return parent + "." + s.Name
}

// ── Job tracking types ──────────────────────────────────────────────

// JobStatus represents the state of a generation job
type JobStatus string

const (
	JobStatusPending  JobStatus = "pending"
	JobStatusQueued   JobStatus = "queued"
	JobStatusRunning  JobStatus = "running"
	JobStatusComplete JobStatus = "complete"
	JobStatusFailed   JobStatus = "failed"
	JobStatusCancelled JobStatus = "cancelled"
)

// SubChapterStatus tracks generation progress for a single sub-chapter
type SubChapterStatus struct {
	Name    string    `json:"name"`
	Status  JobStatus `json:"status"`
	Step    string    `json:"step"`    // e.g., "generating prompt", "generating notes"
	Error   string    `json:"error,omitempty"`
	Output  string    `json:"output,omitempty"` // path to generated file
}

// Job represents a note generation job
type Job struct {
	ID           string              `json:"id"`
	RoadmapTitle string              `json:"roadmapTitle"`
	ChapterName  string              `json:"chapterName"`
	Status       JobStatus           `json:"status"`
	SubChapters  []SubChapterStatus  `json:"subChapters"`
	MergedFile   string              `json:"mergedFile,omitempty"`
	CreatedAt    time.Time           `json:"createdAt"`
	UpdatedAt    time.Time           `json:"updatedAt"`
	Error        string              `json:"error,omitempty"`
}

// ProgressEvent is sent over WebSocket to update the frontend
type ProgressEvent struct {
	JobID       string    `json:"jobId"`
	Type        string    `json:"type"` // "status", "progress", "complete", "error"
	SubChapter  string    `json:"subChapter,omitempty"`
	Status      JobStatus `json:"status"`
	Step        string    `json:"step,omitempty"`
	Message     string    `json:"message,omitempty"`
	QueuePos    int       `json:"queuePosition,omitempty"`
}
