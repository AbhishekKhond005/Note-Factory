# Note Factory — AI Study Notes Generator

Note Factory transforms learning roadmaps into comprehensive, textbook-quality study notes using AI.

It uses a dual-architecture:
1. **Go API Backend**: Manages `opencode` processes, tracks job state, limits concurrency, and provides a REST/WebSocket API.
2. **Next.js Frontend**: A modern web app for uploading roadmaps, visualizing the curriculum tree, and tracking real-time generation progress.

## Running Locally

Everything runs on your own machine — no deployed services required. The frontend talks to the
backend at `http://localhost:8080` (this is the default; override with `NEXT_PUBLIC_API_URL` in
`web/.env.local` if you change the backend port).

### Prerequisites

- **Go** 1.22+
- **Node.js** 20.9+ with npm (Next.js 16 requirement — a `.nvmrc` in `web/` pins v20; run `nvm use` after `cd web`)
- **opencode CLI** — install globally (e.g. `npm install -g opencode-ai`, or follow https://opencode.ai)
- An LLM provider/API key configured for opencode (e.g. via `opencode auth`)

### 1. Start the Go backend

```bash
# In the project root
go run . -port 8080 -parallel 4
```

The server will print its URL (`http://localhost:8080`) once it's ready.

### 2. Start the Next.js frontend

```bash
# In the web/ directory
npm install   # first time only
npm run dev
```

### 3. Use the app

Open [http://localhost:3000](http://localhost:3000), enter a topic, and generate your roadmap and notes.

## Configuration

### Backend flags (`note-factory` / `go run .`)

| Flag | Description | Default |
|------|-------------|---------|
| `-port` | HTTP server port | `8080` |
| `-output` | Directory to store generated notes | `notes` |
| `-roadmaps` | Directory for roadmap files | `roadmaps` |
| `-model` | Override the `opencode` model (e.g. `anthropic/claude-sonnet-4-20250514`) | (opencode default) |
| `-opencode` | Path to the `opencode` binary (defaults to PATH lookup) | — |
| `-parallel` | Max concurrent generation tasks. Use `4` on a desktop; lower on small machines | `1` |
| `-use-docker` | Run opencode inside a Docker container (optional workaround for quota/rate limits) | off |
| `-proxy` | Optional HTTP proxy for Docker containers | — |

### Frontend

- `web/.env.local` → `NEXT_PUBLIC_API_URL`: URL of the Go backend. If blank/unset, the frontend
  defaults to `http://localhost:8080`.

## Deployment (optional)

Note Factory is designed to run locally: the backend executes the `opencode` CLI on your machine,
so it cannot run on serverless platforms. The `fly.toml`, `Dockerfile`, and `runner.Dockerfile`
files are kept for optional VM/container hosting and are not required for local use.
