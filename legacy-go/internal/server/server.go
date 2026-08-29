package server

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"syscall"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/rs/cors"

	"github.com/Note_Factory/internal/agent"
	"github.com/Note_Factory/internal/jobs"
)

// Server is the main HTTP server
type Server struct {
	router      *chi.Mux
	hub         *Hub
	jobManager  *jobs.Manager
	agentConfig *agent.Config
	port        string
	notesDir    string
	roadmapDir  string
	// jobSem is a server-wide semaphore bounding the total number of
	// concurrently running opencode processes across ALL jobs. This is the
	// critical guard against OOM on low-memory deployments (Render 512MB):
	// without it, each queued chapter job spawns its own opencode process
	// and they stack up. With it, at most MaxParallel opencode processes
	// exist system-wide, and extra jobs queue.
	jobSem chan struct{}
}

// Config holds server configuration
type Config struct {
	Port         string
	NotesDir     string
	RoadmapDir   string
	OpencodePath string
	Model        string
	MaxParallel  int
	UseDocker    bool
	Proxy        string
}

// New creates a new server instance
func New(cfg *Config) *Server {
	if cfg.Port == "" {
		cfg.Port = "8080"
	}
	if cfg.NotesDir == "" {
		cfg.NotesDir = "notes"
	}
	if cfg.RoadmapDir == "" {
		cfg.RoadmapDir = "roadmaps"
	}
	if cfg.MaxParallel <= 0 {
		// Keep the default conservative: each opencode process can use
		// hundreds of MB, and small deployments (Render 0.1 CPU / 512MB)
		// can only safely host a single one at a time.
		cfg.MaxParallel = 1
	}

	s := &Server{
		router:     chi.NewRouter(),
		hub:        NewHub(),
		jobManager: jobs.NewManager(),
		agentConfig: &agent.Config{
			OutputDir:    cfg.NotesDir,
			Format:       "md",
			OpencodePath: cfg.OpencodePath,
			Model:        cfg.Model,
			MaxParallel:  cfg.MaxParallel,
			UseDocker:    cfg.UseDocker,
			Proxy:        cfg.Proxy,
		},
		port:       cfg.Port,
		notesDir:   cfg.NotesDir,
		roadmapDir: cfg.RoadmapDir,
		jobSem:     make(chan struct{}, cfg.MaxParallel),
	}

	s.setupRoutes()
	return s
}

func (s *Server) setupRoutes() {
	// Middleware
	s.router.Use(middleware.Logger)
	s.router.Use(middleware.Recoverer)
	s.router.Use(middleware.RequestID)
	s.router.Use(middleware.RealIP)
	// Generous timeout: on 0.1 CPU instances a single AI roadmap generation
	// can take minutes and must not be killed mid-request.
	s.router.Use(middleware.Timeout(10 * time.Minute))

	// CORS
	corsHandler := cors.New(cors.Options{
		AllowedOrigins:   []string{"*"},
		AllowedMethods:   []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowedHeaders:   []string{"*"},
		AllowCredentials: false,
	})
	s.router.Use(corsHandler.Handler)

	// Health check
	s.router.Get("/api/health", s.handleHealth)

	// API routes
	s.router.Route("/api", func(r chi.Router) {
		// Roadmap operations
		r.Get("/roadmaps", s.handleListRoadmaps)
		r.Get("/roadmaps/{filename}", s.handleGetRoadmap)
		r.Post("/roadmaps/parse", s.handleParseRoadmap)
		r.Post("/roadmaps/upload", s.handleUploadRoadmap)
		r.Post("/roadmaps/generate", s.handleGenerateRoadmap)

		// Job operations
		r.Post("/generate", s.handleGenerate)
		r.Post("/generate/overview", s.handleGenerateOverview)
		r.Get("/jobs", s.handleListJobs)
		r.Get("/jobs/{jobID}", s.handleGetJob)
		r.Post("/jobs/{jobID}/cancel", s.handleCancelJob)

		// Notes
		r.Get("/notes/{jobID}", s.handleGetNotes)
		r.Get("/notes/{jobID}/download", s.handleDownloadNotes)
		r.Get("/notes/{jobID}/download-all", s.handleDownloadAllNotes)

		// System
		r.Get("/status", s.handleSystemStatus)

		// WebSocket
		r.Get("/ws", s.hub.HandleWS)
	})

	// Serve the notes directory as static files
	notesAbsDir, _ := filepath.Abs(s.notesDir)
	fs := http.StripPrefix("/files/", http.FileServer(http.Dir(notesAbsDir)))
	s.router.Handle("/files/*", fs)
}

// Run starts the HTTP server with graceful shutdown
func (s *Server) Run() error {
	// Ensure directories exist
	os.MkdirAll(s.notesDir, 0755)
	os.MkdirAll(s.roadmapDir, 0755)

	srv := &http.Server{
		Addr:         ":" + s.port,
		Handler:      s.router,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 120 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	// Graceful shutdown
	done := make(chan os.Signal, 1)
	signal.Notify(done, os.Interrupt, syscall.SIGINT, syscall.SIGTERM)

	go func() {
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Server failed: %v", err)
		}
	}()

	fmt.Printf("\n🚀 Note Factory API server running on http://localhost:%s\n", s.port)
	fmt.Printf("   📡 WebSocket: ws://localhost:%s/api/ws\n", s.port)
	fmt.Printf("   📁 Notes dir: %s\n", s.notesDir)
	fmt.Printf("   📂 Roadmaps: %s\n", s.roadmapDir)
	fmt.Printf("   ⚙️  Max parallel opencode processes: %d\n\n", s.agentConfig.MaxParallel)

	<-done
	log.Println("Server shutting down...")

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	return srv.Shutdown(ctx)
}
