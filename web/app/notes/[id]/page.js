"use client";
import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import Navbar from "../../components/Navbar";
import MarkdownViewer from "../../components/MarkdownViewer";
import api from "../../lib/api";
import styles from "./page.module.css";
import Link from "next/link";

export default function NotesPage() {
  const { id } = useParams();
  const router = useRouter();
  const [job, setJob] = useState(null);
  const [notesData, setNotesData] = useState(null);
  const [activeTab, setActiveTab] = useState("merged"); // "merged" or subChapter name
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const jobData = await api.getJob(id);
        setJob(jobData);

        if (jobData.status === "complete") {
          const data = await api.getNotes(id);
          setNotesData(data);
          
          if (!data.merged && data.notes.length > 0) {
            setActiveTab(data.notes[0].name);
          }
        }
      } catch (err) {
        setError(err.message);
      }
    };

    fetchData();
  }, [id]);

  const handleDownload = () => {
    window.location.href = api.getDownloadUrl(id);
  };

  const handleDownloadAll = () => {
    window.location.href = api.getDownloadAllUrl(id);
  };

  if (error) {
    return (
      <>
        <Navbar />
        <main className="container" style={{ padding: "var(--space-2xl) var(--space-xl)" }}>
          <div className={styles.errorState}>
            <h2>Error Loading Notes</h2>
            <p>{error}</p>
            <button className="btn-secondary" onClick={() => router.push("/jobs")}>
              ← Back to Jobs
            </button>
          </div>
        </main>
      </>
    );
  }

  if (!job) {
    return (
      <>
        <Navbar />
        <main className="container" style={{ padding: "var(--space-2xl) var(--space-xl)" }}>
          <div className={styles.loadingState}>Loading...</div>
        </main>
      </>
    );
  }

  if (job.status !== "complete") {
    return (
      <>
        <Navbar />
        <main className="container" style={{ padding: "var(--space-2xl) var(--space-xl)" }}>
          <div className={styles.notReadyState}>
            <h2>Notes Not Ready</h2>
            <p>This generation job is currently: <strong>{job.status}</strong></p>
            {job.status === "running" ? (
              <button className="btn-primary" onClick={() => router.push("/generate")}>
                View Progress
              </button>
            ) : (
              <button className="btn-secondary" onClick={() => router.push("/jobs")}>
                ← Back to Jobs
              </button>
            )}
          </div>
        </main>
      </>
    );
  }

  const activeContent = activeTab === "merged" 
    ? notesData?.merged 
    : notesData?.notes.find(n => n.name === activeTab)?.content;

  return (
    <>
      <Navbar />
      
      <div className={styles.layout}>
        {/* Sidebar */}
        <aside className={styles.sidebar}>
          <div className={styles.sidebarHeader}>
            <Link href="/jobs" className={styles.backLink}>← Back</Link>
            <h3>{job.chapterName}</h3>
            <p className={styles.meta}>{job.roadmapTitle}</p>
          </div>

          <div className={styles.tabs}>
            {notesData?.merged && (
              <button 
                className={`${styles.tab} ${activeTab === "merged" ? styles.active : ""}`}
                onClick={() => setActiveTab("merged")}
              >
                <span>📘</span> Merged Chapter
              </button>
            )}
            
            <div className={styles.divider} />
            <div className={styles.tabSectionTitle}>Sections</div>
            
            {notesData?.notes.map((note, i) => (
              <button 
                key={i}
                className={`${styles.tab} ${activeTab === note.name ? styles.active : ""}`}
                onClick={() => setActiveTab(note.name)}
              >
                <span>📄</span> {note.name}
              </button>
            ))}
          </div>
        </aside>

        {/* Main Content */}
        <main className={styles.main}>
          <div className={styles.topbar}>
            <h2>{activeTab === "merged" ? "Complete Chapter Notes" : activeTab}</h2>
            {notesData?.merged && (
              <button className="btn-secondary" onClick={handleDownload}>
                ↓ Download Markdown
              </button>
            )}
            {(notesData?.notes.length > 0) && (
              <button className="btn-secondary" onClick={handleDownloadAll}>
                📦 Download All (ZIP)
              </button>
            )}
          </div>
          
          <div className={styles.contentArea}>
            <div className={`glass-panel ${styles.document}`}>
              {activeContent ? (
                <MarkdownViewer content={activeContent} />
              ) : (
                <div className={styles.emptyContent}>No content available.</div>
              )}
            </div>
          </div>
        </main>
      </div>
    </>
  );
}
