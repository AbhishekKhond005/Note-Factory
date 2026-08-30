"use client";
import { useState, useEffect } from "react";
import Navbar from "../components/Navbar";
import RoadmapVisualizer from "../components/RoadmapVisualizer";
import ChapterPicker from "../components/ChapterPicker";
import GenerationDashboard from "../components/GenerationDashboard";
import api from "../lib/api";
import styles from "./page.module.css";

export default function GeneratePage() {
  const [step, setStep] = useState(1); // 1: Create/Upload, 2: Pick chapters, 3: Generate

  // Roadmap state
  const [roadmaps, setRoadmaps] = useState([]);
  const [parsedRoadmap, setParsedRoadmap] = useState(null);

  // Error state
  const [error, setError] = useState("");

  // Job state
  const [currentJob, setCurrentJob] = useState(null);

  // AI roadmap state (topic + optional prompt)
  const [topic, setTopic] = useState("");
  const [topicPrompt, setTopicPrompt] = useState("");
  const [roadmapContent, setRoadmapContent] = useState("");
  const [busy, setBusy] = useState(false);

  // Load predefined roadmaps
  useEffect(() => {
    api.listRoadmaps()
      .then(setRoadmaps)
      .catch(err => console.error("Failed to load roadmaps:", err));
  }, []);

  // Move the dashboard into view
  const showDashboard = (jobId) => {
    setCurrentJob({ id: jobId });
    setStep(3);
  };

  const handleGenerateRoadmap = async () => {
    if (!topic.trim()) {
      setError("Please enter a topic for the AI roadmap.");
      return;
    }
    setError("");
    setBusy(true);
    try {
      const rm = await api.generateRoadmap(topic.trim(), topicPrompt);
      setParsedRoadmap(rm);
      setRoadmapContent("");
      setStep(2);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const handleGenerateOverview = async () => {
    if (!topic.trim()) {
      setError("Please enter a topic for the quick overview.");
      return;
    }
    setError("");
    setBusy(true);
    try {
      const { jobId } = await api.generateOverview(topic.trim(), topicPrompt);
      showDashboard(jobId);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const handleParseText = async () => {
    if (!roadmapContent.trim()) {
      setError("Please enter a roadmap.");
      return;
    }
    setError("");
    setBusy(true);
    try {
      const rm = await api.parseRoadmap(roadmapContent);
      setParsedRoadmap(rm);
      setStep(2);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const handleSelectPredefined = async (id) => {
    setError("");
    setBusy(true);
    try {
      const rm = await api.getRoadmap(id);
      setParsedRoadmap(rm);
      setRoadmapContent("");
      setStep(2);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const handleFileUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setError("");
    setBusy(true);
    try {
      const rm = await api.uploadRoadmap(file);
      setParsedRoadmap(rm);
      setStep(2);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const handleGenerateChapters = async (chapterIndexes) => {
    if (!chapterIndexes || chapterIndexes.length === 0) {
      setError("Select at least one chapter.");
      return;
    }
    setError("");
    setBusy(true);
    try {
      const { jobId, warning } = await api.startGeneration({
        roadmapId: parsedRoadmap.id,
        chapterIndexes,
      });
      if (warning) setError(warning);
      showDashboard(jobId);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <Navbar />

      <main className="container" style={{ padding: "var(--space-2xl) var(--space-xl)" }}>

        {/* Step Indicator */}
        <div className={styles.stepper}>
          <div className={`${styles.step} ${step >= 1 ? styles.active : ""}`}>
            <div className={styles.stepNum}>1</div>
            <span>Create Roadmap</span>
          </div>
          <div className={styles.stepLine} />
          <div className={`${styles.step} ${step >= 2 ? styles.active : ""}`}>
            <div className={styles.stepNum}>2</div>
            <span>Select Chapters</span>
          </div>
          <div className={styles.stepLine} />
          <div className={`${styles.step} ${step >= 3 ? styles.active : ""}`}>
            <div className={styles.stepNum}>3</div>
            <span>Generate Notes</span>
          </div>
        </div>

        {error && <div className={styles.errorAlert}>{error}</div>}

        {/* Step 1: Create / Upload */}
        {step === 1 && (
          <>
            <div className={`glass-panel ${styles.uploadSection}`}>
              <h2>✨ Create a Roadmap from a Topic</h2>
              <p>Enter a topic and optionally a prompt. AI builds a roadmap for you, saved in the project.</p>

              <input
                className="input-field"
                placeholder="Topic, e.g. 'Python for Data Science'"
                value={topic}
                onChange={(e) => setTopic(e.target.value)}
                style={{ marginBottom: "var(--space-md)" }}
              />

              <textarea
                className="input-field"
                placeholder="Optional prompt (priority guidance for the AI — applies to roadmap and notes)..."
                value={topicPrompt}
                onChange={(e) => setTopicPrompt(e.target.value)}
                style={{ minHeight: "100px" }}
              />

              <div className={styles.actions}>
                <button className="btn-primary" onClick={handleGenerateRoadmap} disabled={busy}>
                  {busy ? "Generating roadmap..." : "🚀 Generate Roadmap with AI"}
                </button>

                <div className={styles.divider}>OR</div>

                <button className="btn-secondary" onClick={handleGenerateOverview} disabled={busy}>
                  ⚡ Quick Overview
                </button>
              </div>
              <p className={styles.overviewHint}>
                Quick Overview skips the roadmap and directly writes one short, simple
                notes file explaining what the domain is about.
              </p>
            </div>

            <div className={styles.orDivider}>OR paste / upload an existing roadmap</div>

            <div className={`glass-panel ${styles.uploadSection}`}>
              <h2>Paste your Roadmap</h2>
              <p>Paste a tree-formatted roadmap to get started.</p>

              <textarea
                className="input-field"
                placeholder="Paste roadmap here..."
                value={roadmapContent}
                onChange={(e) => setRoadmapContent(e.target.value)}
              />

              <div className={styles.actions}>
                <button className="btn-primary" onClick={handleParseText} disabled={busy}>
                  Parse Roadmap
                </button>

                <div className={styles.divider}>OR</div>

                <label className="btn-secondary">
                  Upload .txt file
                  <input type="file" accept=".txt,.md" hidden onChange={handleFileUpload} />
                </label>
              </div>

              {roadmaps.length > 0 && (
                <div className={styles.predefinedList}>
                  <h3>Available Roadmaps:</h3>
                  <div className="card-grid">
                    {roadmaps.map(rm => (
                      <div
                        key={rm.id}
                        className={`glass-panel ${styles.rmCard}`}
                        onClick={() => handleSelectPredefined(rm.id)}
                        title={`Load ${rm.title} and generate notes from it`}
                      >
                        <h4>{rm.title}</h4>
                        <p>{rm.id}</p>
                        <span className={styles.rmHint}>Click to load →</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </>
        )}

        {/* Step 2: Pick chapters (multi-select) */}
        {step === 2 && parsedRoadmap && (
          <div className={styles.pickSection}>
            <div className={styles.pickHeader}>
              <h2>{parsedRoadmap.title || "Roadmap"}</h2>
              <button className="btn-ghost" onClick={() => setStep(1)}>
                ← Back to Create
              </button>
            </div>

            <div className={styles.pickLayout}>
              <div className={styles.pickSidebar}>
                <RoadmapVisualizer roadmap={parsedRoadmap} />
              </div>
              <div className={styles.pickContent}>
                <ChapterPicker
                  chapters={parsedRoadmap.chapters}
                  onGenerate={handleGenerateChapters}
                  onBack={() => setStep(1)}
                />
              </div>
            </div>
          </div>
        )}

        {/* Step 3: Generate */}
        {step === 3 && currentJob && (
          <GenerationDashboard
            initialJob={currentJob}
            onBack={() => setStep(2)}
          />
        )}

      </main>
    </>
  );
}
