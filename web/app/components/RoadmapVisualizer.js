import styles from "./RoadmapVisualizer.module.css";
import { useState } from "react";

export default function RoadmapVisualizer({ roadmap }) {
  if (!roadmap || !roadmap.chapters) return null;

  return (
    <div className={`glass-panel ${styles.container}`}>
      <div className={styles.header}>
        <h3>Roadmap Structure</h3>
        <div className={styles.badge}>{roadmap.chapters.length} Chapters</div>
      </div>
      
      <div className={styles.tree}>
        {roadmap.chapters.map((ch, i) => (
          <ChapterNode key={i} chapter={ch} index={i} />
        ))}
      </div>
    </div>
  );
}

function ChapterNode({ chapter, index }) {
  const [expanded, setExpanded] = useState(false);
  const hasSub = chapter.subChapters && chapter.subChapters.length > 0;

  return (
    <div className={styles.node}>
      <div 
        className={`${styles.nodeHeader} ${expanded ? styles.expanded : ""}`}
        onClick={() => hasSub && setExpanded(!expanded)}
      >
        <span className={styles.toggle}>{hasSub ? (expanded ? "▼" : "▶") : "•"}</span>
        <span className={styles.title}>{index + 1}. {chapter.name}</span>
        {hasSub && <span className={styles.count}>{chapter.subChapters.length}</span>}
      </div>
      
      {expanded && hasSub && (
        <div className={styles.children}>
          {chapter.subChapters.map((sub, j) => (
            <div key={j} className={styles.subNode}>
              <span className={styles.subTitle}>{sub.name}</span>
              {sub.topics && sub.topics.length > 0 && (
                <div className={styles.topics}>
                  {sub.topics.map((t, k) => (
                    <span key={k} className={styles.topic}>{t}</span>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
