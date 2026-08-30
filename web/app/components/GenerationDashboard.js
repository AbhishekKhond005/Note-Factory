import { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import api from "../lib/api";
import styles from "./GenerationDashboard.module.css";

export default function GenerationDashboard({ initialJob, onBack }) {
  const [job, setJob] = useState(initialJob || {});
  const [sysStatus, setSysStatus] = useState({ activeJobs: 0, maxParallel: 4 });
  const jobId = job.id;

  const refresh = useCallback(() => {
    if (!jobId) return;
    api.getJob(jobId).then(setJob).catch(console.error);
    api.getSystemStatus().then(setSysStatus).catch(console.error);
  }, [jobId]);

  useEffect(() => {
    refresh();

    // Subscribe to live STOMP events for this job.
    api.subscribeJob(jobId, (event) => {
      // Progress events de-normalize surprisingly often for a single job
      // (PromptCrafter, NoteWriter, Critic, Repair), so the simplest correct
      // approach is to re-fetch the authoritative job view on each event.
      refresh();

      // Reflect the latest status immediately for snappier UI.
      if (event.type === "complete") {
        setJob((prev) => ({ ...prev, status: "complete" }));
      } else if (event.type === "error") {
        setJob((prev) => ({ ...prev, status: "failed" }));
      }
    });

    return () => {
      api.unsubscribeJob(jobId);
    };
  }, [jobId, refresh]);

  const handleCancel = async () => {
    if (confirm("Are you sure you want to cancel this generation?")) {
      await api.cancelJob(job.id);
      setJob({ ...job, status: "cancelled" });
    }
  };

  if (!job || !job.id) return null;

  const chapters = job.chapters || [];
  const total = chapters.length;
  const complete = chapters.filter((c) => c.status === "complete").length;
  const failed = chapters.filter((c) => c.status === "failed").length;
  const running = chapters.filter((c) => c.status === "running").length;

  const progressPct = total === 0 ? 0 : Math.round(((complete + failed) / total) * 100);
  const isDone = job.status === "complete" || job.status === "failed" || job.status === "cancelled";

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <div>
          <h2>Generating Notes</h2>
          <p className={styles.subtitle}>{job.roadmapTitle || "Untitled Roadmap"}</p>
        </div>
        <div className={styles.actions}>
          {isDone ? (
            <Link href={`/notes/${job.id}`} className="btn-primary">
              View Notes →
            </Link>
          ) : (
            <button className="btn-ghost" onClick={handleCancel}>
              Cancel Generation
            </button>
          )}
        </div>
      </div>

      <div className={`glass-panel ${styles.overview}`}>
        <div className={styles.statsRow}>
          <div className={styles.statBox}>
            <span className={styles.statVal}>{progressPct}%</span>
            <span className={styles.statLabel}>Overall Progress</span>
          </div>
          <div className={styles.statBox}>
            <span className={styles.statVal}>{complete}/{total}</span>
            <span className={styles.statLabel}>Completed</span>
          </div>
          <div className={styles.statBox}>
            <span className={styles.statVal}>{running}</span>
            <span className={styles.statLabel}>Generating Now</span>
          </div>
          <div className={styles.statBox}>
            <span className={styles.statVal}>{sysStatus.activeJobs}/{sysStatus.maxParallel}</span>
            <span className={styles.statLabel}>System Capacity</span>
          </div>
        </div>

        <div className="progress-bar" style={{ marginTop: "var(--space-md)" }}>
          <div className="progress-bar-fill" style={{ width: `${progressPct}%` }} />
        </div>

        {job.status === "running" && sysStatus.activeJobs >= sysStatus.maxParallel && running < sysStatus.maxParallel && (
          <div className={styles.queueWarning}>
            System is at capacity. Remaining chapters are queued and will start automatically.
          </div>
        )}
      </div>

      <div className={styles.list}>
        <h3>Chapters ({total})</h3>

        {total === 0 && (
          <div className={`glass-panel ${styles.item}`}>
            <div className={styles.itemDetails}>No chapters in this job.</div>
          </div>
        )}

        <div className={styles.items}>
          {chapters.map((ch, i) => (
            <div key={i} className={`glass-panel ${styles.item}`}>
              <div className={styles.itemHeader}>
                <span className={styles.itemName}>{ch.name || `Chapter ${i + 1}`}</span>
                <StatusBadge status={ch.status} />
              </div>

              <div className={styles.itemDetails}>
                {ch.status === "running" && (
                  <div className={styles.stepInfo}>
                    <div className={`status-dot running`} />
                    {ch.step || "Initializing..."}
                  </div>
                )}
                {ch.status === "failed" && (
                  <div className={styles.errorText}>
                    {ch.error || "Generation failed"}
                  </div>
                )}
                {ch.status === "complete" && (
                  <div className={styles.successText}>
                    ✓ Generated successfully
                  </div>
                )}
                {(ch.status === "pending" || ch.status === "queued") && (
                  <div className={styles.pendingText}>Waiting in queue...</div>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function StatusBadge({ status }) {
  return <span className={`badge badge-${status}`}>{status}</span>;
}
