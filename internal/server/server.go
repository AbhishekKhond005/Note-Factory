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
		cfg.MaxParallel = 4
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
	s.router.Use(middleware.Timeout(60 * time.Second))

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
		r.Post("/roadmaps/parse", s.handleParseRoadmap)
		r.Post("/roadmaps/upload", s.handleUploadRoadmap)

		// Job operations
		r.Post("/generate", s.handleGenerate)
		r.Get("/jobs", s.handleListJobs)
		r.Get("/jobs/{jobID}", s.handleGetJob)
		r.Post("/jobs/{jobID}/cancel", s.handleCancelJob)

		// Notes
		r.Get("/notes/{jobID}", s.handleGetNotes)
		r.Get("/notes/{jobID}/download", s.handleDownloadNotes)

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
	fmt.Printf("   📂 Roadmaps: %s\n\n", s.roadmapDir)

	<-done
	log.Println("Server shutting down...")

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	return srv.Shutdown(ctx)
}
