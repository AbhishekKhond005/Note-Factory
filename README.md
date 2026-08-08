# Note Factory — AI Study Notes Generator

Note Factory transforms learning roadmaps into comprehensive, textbook-quality study notes using AI.

It uses a dual-architecture:
1. **Go API Backend**: Manages `opencode` processes, tracks job state, limits concurrency, and provides a REST/WebSocket API.
2. **Next.js Frontend**: A modern web app for uploading roadmaps, visualizing the curriculum tree, and tracking real-time generation progress.

## Architecture & Deployment

Since the backend relies on executing CLI binaries (`opencode`), it cannot be hosted on serverless platforms like Vercel. 

**Recommended Deployment Strategy:**
- **Backend (Go)**: Deploy to a persistent VM or container platform like Fly.io, Railway, or Render. A Dockerfile is included.
- **Frontend (Next.js)**: Deploy to Vercel or Netlify.

## Environment Variables

### Frontend (`web/.env.local`)
- `NEXT_PUBLIC_API_URL`: The URL of your deployed Go backend (e.g., `https://my-api.fly.dev`). If left blank, it defaults to `http://localhost:8080`.

### Backend
The backend can be configured via CLI flags when executing `note-factory`:
- `-port`: HTTP server port (default `8080`)
- `-output`: Directory to store generated notes (default `notes`)
- `-roadmaps`: Directory to look for existing roadmap files (default `roadmaps`)
- `-model`: Override the `opencode` model
- `-parallel`: Maximum number of concurrent generation tasks to run at once (default `4`)

## Running Locally

1. Start the Go backend:
```bash
# In the project root
go run . -port 8080 -parallel 4
```

2. Start the Next.js frontend:
```bash
# In the web/ directory
npm run dev
```

Visit `http://localhost:3000` to access the application.
