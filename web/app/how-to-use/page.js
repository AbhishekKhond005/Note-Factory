import Navbar from "../components/Navbar";
import Link from "next/link";
import styles from "./page.module.css";

const sections = [
  {
    icon: "🚀",
    title: "1. Create a Roadmap from a Topic (Recommended)",
    steps: [
      <>
        Go to the <Link href="/generate" className={styles.inlineLink}>Generate</Link> page.
      </>,
      "Enter a topic in the first box (e.g. \"Python for Data Science\").",
      "Optionally add a prompt in the second box. This prompt is treated as priority guidance — the AI follows it over its defaults when they conflict, and it is applied to the roadmap, the prompt templates, and every notes file.",
      "Click \"Generate Roadmap with AI\". The roadmap is created and saved inside the project (in the roadmaps folder), then shown in the chapter picker.",
    ],
  },
  {
    icon: "📄",
    title: "2. Use an Existing Roadmap",
    steps: [
      "Paste a tree-formatted roadmap (like the included Java Roadmap) and click \"Parse Roadmap\", or",
      "Upload a .txt / .md roadmap file.",
      "Already-saved roadmaps (including AI-generated ones) appear in the \"Available Roadmaps\" list.",
    ],
  },
  {
    icon: "🎯",
    title: "3. Pick a Chapter",
    steps: [
      "After the roadmap loads, you'll see its tree on the left and the chapter cards on the right.",
      "Click \"Generate Notes\" on any chapter. Sub-chapters are generated in parallel (up to the configured limit).",
      "The optional prompt from step 1 is automatically applied to every AI step here.",
    ],
  },
  {
    icon: "📡",
    title: "4. Watch Progress",
    steps: [
      "The dashboard updates live over WebSocket — you will see each section go from queued → generating prompt → generating notes → done.",
      "If something fails, the rest keep going. You can cancel a run from the dashboard.",
    ],
  },
  {
    icon: "📘",
    title: "5. View & Download Notes",
    steps: [
      "When done, click \"View Notes\" to read the merged chapter or individual sections.",
      "Download the merged chapter as Markdown, or download all files as a ZIP.",
      "You can also track every generation from the Jobs page.",
    ],
  },
];

export default function HowToUsePage() {
  return (
    <>
      <Navbar />

      <main className={`container ${styles.container}`}>
        <div className={styles.header}>
          <h1>
            How to Use <span className="gradient-text">Note Factory</span>
          </h1>
          <p className={styles.subtitle}>
            From a single topic to a full set of study notes — here is the whole flow.
          </p>
        </div>

        <div className={styles.sections}>
          {sections.map((section, i) => (
            <div key={i} className={`glass-panel ${styles.section}`}>
              <div className={styles.sectionHeader}>
                <span className={styles.sectionIcon}>{section.icon}</span>
                <h2>{section.title}</h2>
              </div>
              <ul className={styles.stepList}>
                {section.steps.map((step, j) => (
                  <li key={j}>{step}</li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className={`glass-panel ${styles.tipPanel}`}>
          <h2>💡 Tips</h2>
          <ul className={styles.stepList}>
            <li>
              <strong>Roadmap format:</strong> tree-style with box-drawing characters (├──, └──, │). The parser accepts 3 levels: chapters → sub-topics → learning points.
            </li>
            <li>
              <strong>Optional prompt:</strong> keep it short and specific. It is injected with highest priority into every AI call, so it shapes everything from the roadmap structure to the final notes.
            </li>
            <li>
              <strong>AI-generated roadmaps</strong> are saved to the project roadmaps folder and stay available for future sessions.
            </li>
          </ul>
        </div>

        <div className={styles.cta}>
          <Link href="/generate" className="btn-primary">⚡ Start Generating</Link>
          <Link href="/jobs" className="btn-secondary">📋 View Jobs</Link>
        </div>
      </main>
    </>
  );
}
