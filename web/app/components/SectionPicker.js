import { useState } from "react";
import styles from "./SectionPicker.module.css";

// SectionPicker lets the user choose individual sub-chapters (sections) of a
// chapter. Each selected section is generated as its own document.
export default function SectionPicker({ chapter, onGenerate, onBack }) {
  const [selected, setSelected] = useState([]);

  if (!chapter) return null;

  const subChapters = chapter.subChapters || [];
  const allSelected = subChapters.length > 0 && selected.length === subChapters.length;

  const toggle = (idx) => {
    setSelected((prev) =>
      prev.includes(idx) ? prev.filter((i) => i !== idx) : [...prev, idx]
    );
  };

  const toggleAll = () => {
    setSelected(allSelected ? [] : subChapters.map((_, i) => i));
  };

  const handleGenerate = () => {
    if (selected.length === 0) return;
    onGenerate(selected);
  };

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <div>
          <h2>{chapter.name}</h2>
          <p className={styles.subtitle}>
            Choose the sections you want notes for. Each selected section is
            generated as its own document with its own index.
          </p>
        </div>
        <button className="btn-ghost" onClick={onBack}>← Back to Chapters</button>
      </div>

      <div className={`glass-panel ${styles.controls}`}>
        <label className={styles.selectAll}>
          <input type="checkbox" checked={allSelected} onChange={toggleAll} />
          <span>{allSelected ? "Deselect all" : "Select all"}</span>
          <span className={styles.selectAllCount}>
            {selected.length}/{subChapters.length} selected
          </span>
        </label>
      </div>

      {subChapters.length === 0 && (
        <div className={`glass-panel ${styles.empty}`}>
          This chapter has no sections to generate.
        </div>
      )}

      <div className={styles.list}>
        {subChapters.map((sub, i) => {
          const isSelected = selected.includes(i);
          const hasTopics = sub.topics && sub.topics.length > 0;

          return (
            <div
              key={i}
              className={`glass-panel ${styles.item} ${isSelected ? styles.selected : ""}`}
              onClick={() => toggle(i)}
            >
              <input
                type="checkbox"
                checked={isSelected}
                onChange={() => toggle(i)}
                onClick={(e) => e.stopPropagation()}
              />

              <div className={styles.itemBody}>
                <div className={styles.itemHeader}>
                  <span className={styles.itemIndex}>{i + 1}</span>
                  <span className={styles.itemName}>{sub.name}</span>
                  {hasTopics && (
                    <span className={styles.topicCount}>
                      {sub.topics.length} {sub.topics.length === 1 ? "topic" : "topics"}
                    </span>
                  )}
                </div>

                {hasTopics && (
                  <div className={styles.topics}>
                    {sub.topics.slice(0, 4).map((t, k) => (
                      <span key={k} className={styles.topic}>{t}</span>
                    ))}
                    {sub.topics.length > 4 && (
                      <span className={styles.topicMore}>+{sub.topics.length - 4} more</span>
                    )}
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      <div className={styles.footer}>
        <button
          className="btn-primary"
          onClick={handleGenerate}
          disabled={selected.length === 0}
        >
          {selected.length === 0
            ? "Select at least one section"
            : `Generate Selected Sections (${selected.length})`}
        </button>
      </div>
    </div>
  );
}
