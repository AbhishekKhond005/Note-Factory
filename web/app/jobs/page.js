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
              const total = job.subChapters.length;
              const complete = job.subChapters.filter(sc => sc.status === "complete").length;
              const failed = job.subChapters.filter(sc => sc.status === "failed").length;
              const isDone = job.status === "complete" || job.status === "failed" || job.status === "cancelled";
              
              const date = new Date(job.createdAt).toLocaleString();
              
              return (
                <div key={job.id} className={`glass-panel ${styles.jobCard}`}>
                  <div className={styles.jobInfo}>
                    <div className={styles.jobTitle}>
                      {job.chapterName}
                      <span className={`badge badge-${job.status}`}>{job.status}</span>
                    </div>
                    <div className={styles.jobMeta}>
                      <span>{job.roadmapTitle}</span>
                      <span>•</span>
                      <span>{date}</span>
                    </div>
                    
                    <div className={styles.jobProgress}>
                      <span className={styles.progressText}>
                        {complete}/{total} sections completed
                      </span>
                      {failed > 0 && <span className={styles.errorText}>({failed} failed)</span>}
                    </div>
                  </div>
                  
                  <div className={styles.jobActions}>
                    {isDone && (job.mergedFile || complete > 0) ? (
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
