package main

import (
	"flag"
	"fmt"
	"log"
	"os"
	"os/exec"
	"strings"

	"github.com/Note_Factory/internal/server"
)

func main() {
	cfg := parseFlags()

	// Resolve opencode path
	opencodePath := cfg.opencode
	if opencodePath == "" {
		var err error
		opencodePath, err = exec.LookPath("opencode")
		if err != nil {
			log.Printf("⚠️  opencode not found in PATH. Note generation will fail until opencode is available.")
			log.Printf("   Install it or set -opencode flag.")
			opencodePath = "opencode" // Placeholder — will error on use
		}
	}

	if opencodePath != "opencode" {
		if err := checkOpencode(opencodePath); err != nil {
			log.Printf("⚠️  opencode check failed: %v", err)
		}
	}

	// Copy existing roadmaps into roadmaps/ directory if they exist in root
	copyExistingRoadmaps(cfg.roadmapDir)

	if cfg.useDocker {
		if err := initDockerImage(); err != nil {
			log.Fatalf("Failed to initialize Docker runner: %v", err)
		}
	} else {
		// Even if not forced, we check if docker is available to build the fallback image
		initDockerImage() // ignore error, it's optional fallback
	}

	// Start server
	srv := server.New(&server.Config{
		Port:         cfg.port,
		NotesDir:     cfg.outputDir,
		RoadmapDir:   cfg.roadmapDir,
		OpencodePath: opencodePath,
		Model:        cfg.model,
		MaxParallel:  cfg.parallel,
		UseDocker:    cfg.useDocker,
		Proxy:        cfg.proxy,
	})

	if err := srv.Run(); err != nil {
		log.Fatalf("Server error: %v", err)
	}
}

type config struct {
	port       string
	outputDir  string
	roadmapDir string
	model      string
	opencode   string
	parallel   int
	useDocker  bool
	proxy      string
}

func parseFlags() *config {
	cfg := &config{}

	flag.StringVar(&cfg.port, "port", "8080", "HTTP server port")
	flag.StringVar(&cfg.outputDir, "output", "notes", "Output directory for generated notes")
	flag.StringVar(&cfg.roadmapDir, "roadmaps", "roadmaps", "Directory for roadmap files")
	flag.StringVar(&cfg.model, "model", "", "OpenCode model (e.g. 'anthropic/claude-sonnet-4-20250514')")
	flag.StringVar(&cfg.opencode, "opencode", "", "Path to opencode binary (default: PATH lookup)")
	flag.IntVar(&cfg.parallel, "parallel", 1, "Max parallel opencode processes (default: 1)")
	flag.BoolVar(&cfg.useDocker, "use-docker", false, "Force opencode to run in Docker for all tasks (quota bypass)")
	flag.StringVar(&cfg.proxy, "proxy", "", "HTTP proxy for Docker containers (e.g., http://proxy:port)")

	flag.Usage = func() {
		fmt.Fprintf(os.Stderr, `Note Factory — AI-powered study note generator

Usage:
  note-factory [flags]

Starts an HTTP API server that the web frontend connects to.

Flags:
  -port <port>       Server port (default: 8080)
  -output <dir>      Output root for notes (default: "notes")
  -roadmaps <dir>    Roadmap files directory (default: "roadmaps")
  -model <model>     OpenCode model override
  -opencode <path>   Path to opencode binary
  -parallel <n>      Max parallel opencode processes (default: 4)
  -use-docker        Run opencode in Docker containers to bypass device quotas
  -proxy <url>       Optional HTTP proxy for Docker containers
`)
	}

	flag.Parse()
	return cfg
}

func initDockerImage() error {
	// Check if docker is available
	cmd := exec.Command("docker", "info")
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("docker daemon not found or not running")
	}

	fmt.Println("Initializing opencode-runner Docker image (this may take a moment)...")
	buildCmd := exec.Command("docker", "build", "-t", "opencode-runner", "-f", "runner.Dockerfile", ".")
	buildCmd.Stdout = os.Stdout
	buildCmd.Stderr = os.Stderr
	
	if err := buildCmd.Run(); err != nil {
		return fmt.Errorf("failed to build opencode-runner image: %w", err)
	}
	
	fmt.Println("opencode-runner image ready!")
	return nil
}

func checkOpencode(path string) error {
	cmd := exec.Command(path, "--version")
	output, err := cmd.Output()
	if err != nil {
		return fmt.Errorf("cannot execute opencode: %w", err)
	}
	fmt.Printf("🤖 OpenCode version: %s\n", strings.TrimSpace(string(output)))
	return nil
}

func copyExistingRoadmaps(roadmapDir string) {
	os.MkdirAll(roadmapDir, 0755)

	// Look for roadmap files in the current directory
	patterns := []string{"*roadmap*", "*Roadmap*", "*ROADMAP*"}
	for _, p := range patterns {
		matches, err := exec.Command("sh", "-c", fmt.Sprintf("ls %s 2>/dev/null", p)).Output()
		if err != nil {
			continue
		}
		for _, name := range strings.Split(strings.TrimSpace(string(matches)), "\n") {
			if name == "" {
				continue
			}
			info, err := os.Stat(name)
			if err != nil || info.IsDir() {
				continue
			}
			// Copy to roadmaps dir if not already there
			dest := fmt.Sprintf("%s/%s", roadmapDir, name)
			if _, err := os.Stat(dest); err != nil {
				data, err := os.ReadFile(name)
				if err == nil {
					os.WriteFile(dest, data, 0644)
					log.Printf("📄 Copied roadmap: %s → %s", name, dest)
				}
			}
		}
	}
}
