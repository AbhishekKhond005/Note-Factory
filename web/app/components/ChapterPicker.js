"use client";
import { useState } from "react";
import styles from "./ChapterPicker.module.css";

// Multi-select chapter picker. The backend generates one notes document per
// selected chapter, so the user can pick one or more chapters at once.
export default function ChapterPicker({ chapters, onGenerate, onBack }) {
  const [selected, setSelected] = useState([]);

  if (!chapters || chapters.length === 0) return null;

  const toggle = (index) => {
    setSelected((prev) =>
      prev.includes(index) ? prev.filter((i) => i !== index) : [...prev, index]
    );
  };

  const toggleAll = () => {
    setSelected(selected.length === chapters.length ? [] : chapters.map((_, i) => i));
  };

  const allSelected = selected.length === chapters.length;

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <div>
          <h3>Select Chapters to Generate</h3>
          <p>
            Each selected chapter gets its own complete set of notes. You can pick
            multiple chapters — they will be generated in parallel.
          </p>
        </div>
        {onBack && (
          <button className="btn-ghost" onClick={onBack}>← Back</button>
        )}
      </div>

      <div className={`glass-panel ${styles.controls}`}>
        <label className={styles.selectAll}>
          <input type="checkbox" checked={allSelected} onChange={toggleAll} />
          <span>{allSelected ? "Deselect all" : "Select all"}</span>
          <span className={styles.selectAllCount}>
            {selected.length}/{chapters.length} selected
          </span>
        </label>
      </div>

      <div className={styles.grid}>
        {chapters.map((ch, index) => {
          const isSelected = selected.includes(index);
          const subCount = ch.subChapters ? ch.subChapters.length : 0;

          return (
            <div
              key={index}
              className={`glass-panel ${styles.card} ${isSelected ? styles.selected : ""}`}
              onClick={() => toggle(index)}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => e.key === "Enter" && toggle(index)}
            >
              <div className={styles.cardHeader}>
                <input
                  type="checkbox"
                  checked={isSelected}
                  onChange={() => toggle(index)}
                  onClick={(e) => e.stopPropagation()}
                />
                <span className={styles.number}>{index + 1}</span>
                <span className={styles.badge}>{subCount} sections</span>
              </div>
              <h4 className={styles.title}>{ch.name}</h4>

              <div className={styles.preview}>
                {ch.subChapters && ch.subChapters.slice(0, 3).map((sub, i) => (
                  <div key={i} className={styles.previewItem}>• {sub.name}</div>
                ))}
                {subCount > 3 && (
                  <div className={styles.previewMore}>+ {subCount - 3} more...</div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      <div className={styles.footer}>
        <button
          className="btn-primary"
          onClick={() => onGenerate(selected)}
          disabled={selected.length === 0}
        >
          {selected.length === 0
            ? "Select at least one chapter"
            : `Generate Selected Chapters (${selected.length})`}
        </button>
      </div>
    </div>
  );
}
