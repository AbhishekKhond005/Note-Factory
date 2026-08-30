import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

class ApiClient {
  constructor() {
    this.baseUrl = API_BASE;
    this.ws = null;
    this.wsConnected = false;
    this.wsSubscriptions = new Map(); // jobId -> { onEvent, subscription, retry }
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

  /** List saved roadmaps (Roadmap entities with id + title + chapters). */
  async listRoadmaps() {
    return this.request("/api/roadmaps");
  }

  /** Load a roadmap by its id. */
  async getRoadmap(id) {
    return this.request(`/api/roadmaps/${encodeURIComponent(id)}`);
  }

  /** List roadmap .txt files available on disk. */
  async listRoadmapFiles() {
    return this.request("/api/roadmaps/files");
  }

  /** Parse free-form roadmap text via the AI extraction agent -> Roadmap entity. */
  async parseRoadmap(content) {
    return this.request("/api/roadmaps/parse", {
      method: "POST",
      body: JSON.stringify({ content }),
    });
  }

  /** Generate a roadmap from a topic via prompt 0 -> Roadmap entity. */
  async generateRoadmap(topic, prompt) {
    return this.request("/api/roadmaps/generate", {
      method: "POST",
      body: JSON.stringify({ topic, prompt }),
    });
  }

  /** Upload a roadmap file -> Roadmap entity. */
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

  /**
   * Start a chapter-based generation job against a roadmap.
   * Returns { jobId, warning? }.
   */
  async startGeneration({ roadmapId, roadmapContent, roadmapFile, chapterIndexes }) {
    const body = {
      roadmapId,
      roadmapContent,
      roadmapFile,
      chapterIndexes,
    };
    return this.request("/api/generate", {
      method: "POST",
      body: JSON.stringify(body),
    });
  }

  /** Start a quick overview job for a topic. Returns { jobId }. */
  async generateOverview(topic, prompt) {
    return this.request("/api/generate/overview", {
      method: "POST",
      body: JSON.stringify({ topic, prompt }),
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

  getDownloadAllUrl(jobId) {
    return `${this.baseUrl}/api/notes/${jobId}/download-all`;
  }

  // ── System ───────────────────────────────────────────────────────

  async getSystemStatus() {
    return this.request("/api/status");
  }

  async healthCheck() {
    return this.request("/api/health");
  }

  // ── WebSocket (STOMP over SockJS) ────────────────────────────────

  /** Ensure the single shared STOMP client is connected. */
  _ensureWS() {
    if (this.ws && this.wsConnected) return this.ws;

    if (!this.ws) {
      const wsUrl = this.baseUrl.replace(/^http/, "ws") + "/api/ws";
      this.ws = new Client({
        webSocketFactory: () => new SockJS(wsUrl),
        reconnectDelay: 3000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        onConnect: () => {
          this.wsConnected = true;
          console.log("[WS] STOMP connected");
          // (Re)subscribe all registered job handlers
          this.wsSubscriptions.forEach((entry, jobId) => this._subscribe(jobId, entry));
        },
        onStompError: (frame) => {
          console.warn("[WS] STOMP error:", frame.headers && frame.headers.message);
        },
      });
      this.ws.activate();
    }
    return this.ws;
  }

  _subscribe(jobId, entry) {
    if (!this.ws || !this.wsConnected) return;
    try {
      const sub = this.ws.subscribe(`/topic/jobs/${jobId}`, (message) => {
        try {
          entry.onEvent(JSON.parse(message.body));
        } catch (e) {
          console.warn("[WS] Failed to parse message:", e);
        }
      });
      entry.subscription = sub;
    } catch (e) {
      console.warn("[WS] Subscribe failed for", jobId, e);
    }
  }

  /**
   * Subscribe to live progress events for a specific job.
   * events: { type, jobId, taskId, chapter, status, message }
   */
  subscribeJob(jobId, onEvent) {
    this._ensureWS();
    if (this.wsSubscriptions.has(jobId)) {
      // ignore duplicate subscriptions for the same job
      return;
    }
    const entry = { onEvent, subscription: null, retry: 0 };
    this.wsSubscriptions.set(jobId, entry);
    if (this.wsConnected) this._subscribe(jobId, entry);
  }

  /** Unsubscribe from a specific job's events. */
  unsubscribeJob(jobId) {
    const entry = this.wsSubscriptions.get(jobId);
    if (entry && entry.subscription) {
      try {
        entry.subscription.unsubscribe();
      } catch (e) {
        console.warn("[WS] Unsubscribe failed for", jobId, e);
      }
    }
    this.wsSubscriptions.delete(jobId);
  }

  /** Close the shared STOMP connection. */
  disconnectWS() {
    if (this.ws) {
      try {
        this.ws.deactivate();
      } catch (e) {
        console.warn("[WS] Deactivate error:", e);
      }
      this.ws = null;
    }
    this.wsConnected = false;
    this.wsSubscriptions.clear();
  }
}

// Singleton
const api = new ApiClient();
export default api;
