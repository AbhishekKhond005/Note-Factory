# ── Stage 1: Build ──────────────────────────────────────────────────
FROM golang:1.22-alpine AS builder

WORKDIR /app

# Install git for go modules
RUN apk add --no-cache git

# Copy go module files first for layer caching
COPY go.mod go.sum ./
RUN go mod download

# Copy source code
COPY . .

# Build
RUN CGO_ENABLED=0 GOOS=linux go build -o /note-factory .

# ── Stage 2: Runtime ────────────────────────────────────────────────
FROM alpine:3.19

WORKDIR /app

# Install opencode and other dependencies
RUN apk add --no-cache ca-certificates curl bash nodejs npm

# Install opencode globally
RUN npm install -g opencode@latest || echo "opencode install skipped"

# Copy the binary
COPY --from=builder /note-factory /app/note-factory

# Create directories
RUN mkdir -p /app/notes /app/roadmaps

# Copy any default roadmaps
COPY Roadmap.txt /app/roadmaps/ 2>/dev/null || true

# Environment
ENV PORT=8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

ENTRYPOINT ["/app/note-factory"]
CMD ["-port", "8080", "-parallel", "4"]
