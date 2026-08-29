import styles from "./ChapterPicker.module.css";

export default function ChapterPicker({ chapters, onSelect }) {
  if (!chapters || chapters.length === 0) return null;

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h3>Select a Chapter to Generate</h3>
        <p>Pick a chapter to start generating study notes.</p>
      </div>
      
      <div className={styles.grid}>
        {chapters.map((ch, index) => {
          const subCount = ch.subChapters ? ch.subChapters.length : 0;
          
          return (
            <div key={index} className={`glass-panel ${styles.card}`}>
              <div className={styles.cardHeader}>
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
              
              <button 
                className={`btn-primary ${styles.btn}`}
                onClick={() => onSelect(index)}
              >
                Select Sections →
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
}
