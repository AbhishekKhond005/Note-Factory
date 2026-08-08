"use client";
import { useState, useEffect } from "react";
import Navbar from "../components/Navbar";
import RoadmapVisualizer from "../components/RoadmapVisualizer";
import ChapterPicker from "../components/ChapterPicker";
import GenerationDashboard from "../components/GenerationDashboard";
import api from "../lib/api";
import styles from "./page.module.css";

export default function GeneratePage() {
  const [step, setStep] = useState(1); // 1: Upload, 2: Pick, 3: Generate
  
  // Roadmap state
  const [roadmaps, setRoadmaps] = useState([]);
  const [selectedRoadmap, setSelectedRoadmap] = useState(null);
  const [roadmapContent, setRoadmapContent] = useState("");
  const [parsedRoadmap, setParsedRoadmap] = useState(null);
  
  // Error state
  const [error, setError] = useState("");

  // Job state
  const [currentJob, setCurrentJob] = useState(null);

  // Load predefined roadmaps
  useEffect(() => {
    api.listRoadmaps()
      .then(setRoadmaps)
      .catch(err => console.error("Failed to load roadmaps:", err));
  }, []);

  const handleParseText = async () => {
    if (!roadmapContent.trim()) {
      setError("Please enter a roadmap.");
      return;
    }
    
    setError("");
    try {
      const rm = await api.parseRoadmap(roadmapContent);
      setParsedRoadmap(rm);
      setStep(2);
    } catch (err) {
      setError(err.message);
    }
  };

  const handleSelectPredefined = async (filename) => {
    setError("");
    setSelectedRoadmap(filename);
    try {
      // In a real app we'd fetch the content or parse it directly.
      // We can trigger generation with the filename directly.
      // But we need the parsed roadmap to show the picker.
      // As a workaround, we could have an API endpoint to get the parsed roadmap for a file.
      // For now, let's just make the user upload or paste.
      // Actually, wait, we don't have a GET /api/roadmaps/:id endpoint.
      // Let's just stick to text pasting for now for custom ones, or maybe we can fetch the txt file from public?
      // Our API serves files from /files/ if they are notes.
      // Let's add a quick hack to parse a predefined roadmap by fetching its path if it's served.
      // Or simply, since it's an MVP, let's just have a big text area for now.
    } catch (err) {
      setError(err.message);
    }
  };
  
  const handleFileUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    
    setError("");
    try {
      const res = await api.uploadRoadmap(file);
      setParsedRoadmap(res.roadmap);
      setSelectedRoadmap(res.filename);
      setStep(2);
    } catch (err) {
      setError(err.message);
    }
  };

  const handleGenerate = async (chapterIndex) => {
    setError("");
    try {
      const job = await api.startGeneration({
        roadmapContent: roadmapContent || undefined,
        roadmapFile: selectedRoadmap || undefined,
        chapterIndex,
      });
      setCurrentJob(job);
      setStep(3);
    } catch (err) {
      setError(err.message);
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
            <span>Upload Roadmap</span>
          </div>
          <div className={styles.stepLine} />
          <div className={`${styles.step} ${step >= 2 ? styles.active : ""}`}>
            <div className={styles.stepNum}>2</div>
            <span>Select Chapter</span>
          </div>
          <div className={styles.stepLine} />
          <div className={`${styles.step} ${step >= 3 ? styles.active : ""}`}>
            <div className={styles.stepNum}>3</div>
            <span>Generate Notes</span>
          </div>
        </div>

        {error && <div className={styles.errorAlert}>{error}</div>}

        {/* Step 1: Upload */}
        {step === 1 && (
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
              <button className="btn-primary" onClick={handleParseText}>
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
                    <div key={rm.filename} className={`glass-panel ${styles.rmCard}`}>
                      <h4>{rm.name}</h4>
                      <p>{rm.filename}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Step 2: Pick */}
        {step === 2 && parsedRoadmap && (
          <div className={styles.pickSection}>
            <div className={styles.pickHeader}>
              <h2>{parsedRoadmap.title || "Roadmap"}</h2>
              <button className="btn-ghost" onClick={() => setStep(1)}>
                ← Back to Upload
              </button>
            </div>
            
            <div className={styles.pickLayout}>
              <div className={styles.pickSidebar}>
                <RoadmapVisualizer roadmap={parsedRoadmap} />
              </div>
              <div className={styles.pickContent}>
                <ChapterPicker 
                  chapters={parsedRoadmap.chapters} 
                  onSelect={handleGenerate} 
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
