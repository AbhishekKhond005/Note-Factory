import { useState, useEffect } from "react";
import Link from "next/link";
import api from "../lib/api";
import styles from "./GenerationDashboard.module.css";

export default function GenerationDashboard({ initialJob, onBack }) {
  const [job, setJob] = useState(initialJob);
  const [sysStatus, setSysStatus] = useState({ activeJobs: 0, maxParallel: 4 });

  useEffect(() => {
    // Initial fetch to get latest status
    api.getJob(job.id).then(setJob).catch(console.error);
    api.getSystemStatus().then(setSysStatus).catch(console.error);

    // Subscribe to WebSocket events
    api.connectWS((event) => {
      if (event.jobId === job.id) {
        setJob((prev) => {
          if (!prev) return prev;
          const next = { ...prev };
          
          if (event.type === "status" || event.type === "complete") {
            next.status = event.status;
          }
          
          if (event.subChapter) {
            const scIdx = next.subChapters.findIndex(sc => sc.name === event.subChapter);
            if (scIdx >= 0) {
              next.subChapters[scIdx] = {
                ...next.subChapters[scIdx],
                status: event.status,
                step: event.step || next.subChapters[scIdx].step,
                error: event.message || next.subChapters[scIdx].error,
              };
            }
          }
          return next;
        });
      }
      
      // Update system status periodically on any event
      api.getSystemStatus().then(setSysStatus).catch(console.error);
    });

    return () => {
      api.disconnectWS();
    };
  }, [job.id]);

  const handleCancel = async () => {
    if (confirm("Are you sure you want to cancel this generation?")) {
      await api.cancelJob(job.id);
      setJob({ ...job, status: "cancelled" });
    }
  };

  if (!job) return null;

  const total = job.subChapters.length;
  const complete = job.subChapters.filter(sc => sc.status === "complete").length;
  const failed = job.subChapters.filter(sc => sc.status === "failed").length;
  const running = job.subChapters.filter(sc => sc.status === "running").length;
  
  const progressPct = total === 0 ? 0 : Math.round(((complete + failed) / total) * 100);
  const isDone = job.status === "complete" || job.status === "failed" || job.status === "cancelled";

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <div>
          <h2>Generating Notes</h2>
          <p className={styles.subtitle}>{job.chapterName}</p>
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
            System is at capacity. Remaining items are queued and will start automatically.
          </div>
        )}
      </div>

      <div className={styles.list}>
        <h3>Sections ({total})</h3>
        
        <div className={styles.items}>
          {job.subChapters.map((sc, i) => (
            <div key={i} className={`glass-panel ${styles.item}`}>
              <div className={styles.itemHeader}>
                <span className={styles.itemName}>{sc.name}</span>
                <StatusBadge status={sc.status} />
              </div>
              
              <div className={styles.itemDetails}>
                {sc.status === "running" && (
                  <div className={styles.stepInfo}>
                    <div className={`status-dot running`} />
                    {sc.step || "Initializing..."}
                  </div>
                )}
                {sc.status === "failed" && (
                  <div className={styles.errorText}>
                    {sc.error || "Generation failed"}
                  </div>
                )}
                {sc.status === "complete" && (
                  <div className={styles.successText}>
                    ✓ Generated successfully
                  </div>
                )}
                {sc.status === "pending" && (
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
