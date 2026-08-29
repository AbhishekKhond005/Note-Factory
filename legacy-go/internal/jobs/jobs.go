package jobs

import (
	"sort"
	"sync"
	"time"

	"github.com/Note_Factory/internal/types"
	"github.com/google/uuid"
)

// maxStoredJobs bounds the in-memory job list. Jobs are tiny, but on a
// long-running low-memory instance the list should not grow without bound.
const maxStoredJobs = 100

// Manager manages generation jobs in memory
type Manager struct {
	mu   sync.RWMutex
	jobs map[string]*types.Job
}

// NewManager creates a new job manager
func NewManager() *Manager {
	return &Manager{
		jobs: make(map[string]*types.Job),
	}
}

// Create creates a new job and returns its ID
func (m *Manager) Create(roadmapTitle, chapterName string, subChapters []types.SubChapter) *types.Job {
	m.mu.Lock()
	defer m.mu.Unlock()

	id := uuid.New().String()[:8]
	now := time.Now()

	statuses := make([]types.SubChapterStatus, len(subChapters))
	for i, sc := range subChapters {
		statuses[i] = types.SubChapterStatus{
			Name:   sc.Name,
			Status: types.JobStatusPending,
		}
	}

	job := &types.Job{
		ID:           id,
		RoadmapTitle: roadmapTitle,
		ChapterName:  chapterName,
		Status:       types.JobStatusPending,
		SubChapters:  statuses,
		CreatedAt:    now,
		UpdatedAt:    now,
	}

	m.jobs[id] = job
	m.pruneLocked()
	return job
}

// pruneLocked removes the oldest finished jobs (complete/failed/cancelled)
// once the list exceeds maxStoredJobs. Running and queued jobs are kept.
// Caller must hold m.mu.
func (m *Manager) pruneLocked() {
	if len(m.jobs) <= maxStoredJobs {
		return
	}

	var terminal []*types.Job
	for _, j := range m.jobs {
		if j.Status == types.JobStatusComplete || j.Status == types.JobStatusFailed || j.Status == types.JobStatusCancelled {
			terminal = append(terminal, j)
		}
	}
	if len(terminal) == 0 {
		return
	}

	// Remove oldest terminal jobs until under the cap
	sort.Slice(terminal, func(i, j int) bool {
		return terminal[i].CreatedAt.Before(terminal[j].CreatedAt)
	})
	for _, j := range terminal {
		if len(m.jobs) <= maxStoredJobs {
			break
		}
		delete(m.jobs, j.ID)
	}
}

// Get returns a job by ID (nil if not found)
func (m *Manager) Get(id string) *types.Job {
	m.mu.RLock()
	defer m.mu.RUnlock()
	return m.jobs[id]
}

// List returns all jobs, newest first
func (m *Manager) List() []*types.Job {
	m.mu.RLock()
	defer m.mu.RUnlock()

	result := make([]*types.Job, 0, len(m.jobs))
	for _, j := range m.jobs {
		result = append(result, j)
	}
	return result
}

// UpdateJobStatus updates the overall job status
func (m *Manager) UpdateJobStatus(id string, status types.JobStatus) {
	m.mu.Lock()
	defer m.mu.Unlock()

	if j, ok := m.jobs[id]; ok {
		j.Status = status
		j.UpdatedAt = time.Now()
	}
}

// UpdateSubChapter updates a sub-chapter's status within a job
func (m *Manager) UpdateSubChapter(jobID, subName string, status types.JobStatus, step, errMsg, output string) {
	m.mu.Lock()
	defer m.mu.Unlock()

	j, ok := m.jobs[jobID]
	if !ok {
		return
	}

	for i := range j.SubChapters {
		if j.SubChapters[i].Name == subName {
			j.SubChapters[i].Status = status
			j.SubChapters[i].Step = step
			if errMsg != "" {
				j.SubChapters[i].Error = errMsg
			}
			if output != "" {
				j.SubChapters[i].Output = output
			}
			break
		}
	}
	j.UpdatedAt = time.Now()
}

// SetMergedFile records the merged output file path
func (m *Manager) SetMergedFile(id, path string) {
	m.mu.Lock()
	defer m.mu.Unlock()

	if j, ok := m.jobs[id]; ok {
		j.MergedFile = path
		j.UpdatedAt = time.Now()
	}
}

// SetError marks a job as failed with an error message
func (m *Manager) SetError(id, errMsg string) {
	m.mu.Lock()
	defer m.mu.Unlock()

	if j, ok := m.jobs[id]; ok {
		j.Status = types.JobStatusFailed
		j.Error = errMsg
		j.UpdatedAt = time.Now()
	}
}

// ActiveCount returns the number of currently running jobs
func (m *Manager) ActiveCount() int {
	m.mu.RLock()
	defer m.mu.RUnlock()

	count := 0
	for _, j := range m.jobs {
		if j.Status == types.JobStatusRunning {
			count++
		}
	}
	return count
}
