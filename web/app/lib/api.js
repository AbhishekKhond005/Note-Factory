const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

class ApiClient {
  constructor() {
    this.baseUrl = API_BASE;
    this.ws = null;
    this.wsListeners = new Map();
  }

  async request(path, options = {}) {
    const url = `${this.baseUrl}${path}`;
    const config = {
      headers: {
        "Content-Type": "application/json",
        ...options.headers,
      },
      ...options,
    };

    try {
      const res = await fetch(url, config);
      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.error || `Request failed: ${res.status}`);
      }

      return data;
    } catch (err) {
      if (err.name === "TypeError" && err.message.includes("fetch")) {
        throw new Error(
          "Cannot connect to Note Factory API. Make sure the backend is running."
        );
      }
      throw err;
    }
  }

  // ── Roadmap endpoints ────────────────────────────────────────────

  async listRoadmaps() {
    return this.request("/api/roadmaps");
  }

  async parseRoadmap(content) {
    return this.request("/api/roadmaps/parse", {
      method: "POST",
      body: JSON.stringify({ content }),
    });
  }

  async uploadRoadmap(file) {
    const formData = new FormData();
    formData.append("roadmap", file);

    const url = `${this.baseUrl}/api/roadmaps/upload`;
    const res = await fetch(url, { method: "POST", body: formData });
    const data = await res.json();

    if (!res.ok) throw new Error(data.error || "Upload failed");
    return data;
  }

  // ── Generation endpoints ─────────────────────────────────────────

  async startGeneration({ roadmapContent, roadmapFile, chapterIndex }) {
    return this.request("/api/generate", {
      method: "POST",
      body: JSON.stringify({ roadmapContent, roadmapFile, chapterIndex }),
    });
  }

  async getJob(jobId) {
    return this.request(`/api/jobs/${jobId}`);
  }

  async listJobs() {
    return this.request("/api/jobs");
  }

  async cancelJob(jobId) {
    return this.request(`/api/jobs/${jobId}/cancel`, { method: "POST" });
  }

  // ── Notes endpoints ──────────────────────────────────────────────

  async getNotes(jobId) {
    return this.request(`/api/notes/${jobId}`);
  }

  getDownloadUrl(jobId) {
    return `${this.baseUrl}/api/notes/${jobId}/download`;
  }

  // ── System ───────────────────────────────────────────────────────

  async getSystemStatus() {
    return this.request("/api/status");
  }

  async healthCheck() {
    return this.request("/api/health");
  }

  // ── WebSocket ────────────────────────────────────────────────────

  connectWS(onEvent) {
    const wsUrl = this.baseUrl.replace(/^http/, "ws") + "/api/ws";

    try {
      this.ws = new WebSocket(wsUrl);

      this.ws.onopen = () => {
        console.log("[WS] Connected");
      };

      this.ws.onmessage = (evt) => {
        try {
          const event = JSON.parse(evt.data);
          onEvent(event);
        } catch (e) {
          console.warn("[WS] Failed to parse message:", e);
        }
      };

      this.ws.onerror = (err) => {
        console.warn("[WS] Error:", err);
      };

      this.ws.onclose = () => {
        console.log("[WS] Disconnected, reconnecting in 3s...");
        setTimeout(() => this.connectWS(onEvent), 3000);
      };
    } catch (err) {
      console.warn("[WS] Connection failed:", err);
      setTimeout(() => this.connectWS(onEvent), 5000);
    }
  }

  disconnectWS() {
    if (this.ws) {
      this.ws.onclose = null; // prevent reconnection
      this.ws.close();
      this.ws = null;
    }
  }
}

// Singleton
const api = new ApiClient();
export default api;
