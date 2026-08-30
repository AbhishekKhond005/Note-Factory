"use client";
import { useState, useEffect } from "react";
import Link from "next/link";
import Navbar from "../components/Navbar";
import api from "../lib/api";
import styles from "./page.module.css";

export default function JobsPage() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchJobs = async () => {
      try {
        const data = await api.listJobs();
        setJobs(data);
      } catch (err) {
        console.error("Failed to list jobs:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchJobs();

    // Poll for updates every 5 seconds
    const interval = setInterval(fetchJobs, 5000);
    return () => clearInterval(interval);
  }, []);

  const doneStatuses = ["complete", "failed", "cancelled"];

  return (
    <>
      <Navbar />

      <main className="container" style={{ padding: "var(--space-2xl) var(--space-xl)" }}>
        <div className={styles.header}>
          <div>
            <h1>Generation Jobs</h1>
            <p className={styles.subtitle}>Track your past and active generation tasks.</p>
          </div>
          <Link href="/generate" className="btn-primary">
            + New Generation
          </Link>
        </div>

        {loading ? (
          <div className={styles.emptyState}>Loading jobs...</div>
        ) : jobs.length === 0 ? (
          <div className={`glass-panel ${styles.emptyState}`}>
            <h3>No jobs found</h3>
            <p>You haven't generated any study notes yet.</p>
          </div>
        ) : (
          <div className={styles.jobList}>
            {jobs.map(job => {
              const chapters = job.chapters || [];
              const total = chapters.length;
              const complete = chapters.filter(c => c.status === "complete").length;
              const failed = chapters.filter(c => c.status === "failed").length;
              const isDone = doneStatuses.includes(job.status);

              const date = new Date(job.createdAt).toLocaleString();

              return (
                <div key={job.id} className={`glass-panel ${styles.jobCard}`}>
                  <div className={styles.jobInfo}>
                    <div className={styles.jobTitle}>
                      {job.roadmapTitle || "Untitled Roadmap"}
                      <span className={`badge badge-${job.status}`}>{job.status}</span>
                    </div>
                    <div className={styles.jobMeta}>
                      <span>{job.scope || "chapter"}</span>
                      <span>•</span>
                      <span>{date}</span>
                    </div>

                    <div className={styles.jobProgress}>
                      <span className={styles.progressText}>
                        {complete}/{total} chapters completed
                      </span>
                      {failed > 0 && <span className={styles.errorText}>({failed} failed)</span>}
                    </div>
                  </div>

                  <div className={styles.jobActions}>
                    {isDone && complete > 0 ? (
                      <Link href={`/notes/${job.id}`} className="btn-secondary">
                        View Notes
                      </Link>
                    ) : (
                      <span className={styles.statusWait}>
                        {job.status === "running" ? "Generating..." : "Waiting..."}
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </main>
    </>
  );
}
