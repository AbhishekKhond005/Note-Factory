import Navbar from "./components/Navbar";
import styles from "./page.module.css";
import Link from "next/link";

export default function Home() {
  return (
    <>
      <Navbar />

      {/* Background ambient effects */}
      <div className={styles.bgEffects}>
        <div className={styles.gradientOrb1} />
        <div className={styles.gradientOrb2} />
        <div className={styles.gridOverlay} />
      </div>

      {/* Hero Section */}
      <section className={styles.hero}>
        <div className={styles.heroContent}>
          <div className={styles.heroBadge}>
            <span className={styles.heroBadgeDot} />
            AI-Powered Study Notes
          </div>

          <h1 className={styles.heroTitle}>
            Turn Any Roadmap Into
            <br />
            <span className="gradient-text">Textbook-Quality Notes</span>
          </h1>

          <p className={styles.heroSubtitle}>
            Paste a learning roadmap, pick a chapter, and let AI generate
            comprehensive, structured study notes with code examples, diagrams,
            and real-world context — in minutes, not hours.
          </p>

          <div className={styles.heroCta}>
            <Link href="/generate" className="btn-primary" id="cta-generate">
              ⚡ Start Generating
            </Link>
            <Link href="#how-it-works" className="btn-secondary" id="cta-learn-more">
              How It Works →
            </Link>
          </div>

          <div className={styles.heroStats}>
            <div className={styles.stat}>
              <span className={styles.statNumber}>17</span>
              <span className={styles.statLabel}>Chapters Supported</span>
            </div>
            <div className={styles.statDivider} />
            <div className={styles.stat}>
              <span className={styles.statNumber}>4x</span>
              <span className={styles.statLabel}>Parallel Generation</span>
            </div>
            <div className={styles.statDivider} />
            <div className={styles.stat}>
              <span className={styles.statNumber}>∞</span>
              <span className={styles.statLabel}>Custom Roadmaps</span>
            </div>
          </div>
        </div>
      </section>

      {/* How It Works */}
      <section className={styles.howItWorks} id="how-it-works">
        <div className="container">
          <h2 className={styles.sectionTitle}>
            How It <span className="gradient-text">Works</span>
          </h2>
          <p className={styles.sectionSubtitle}>
            Three simple steps to comprehensive study material
          </p>

          <div className={styles.stepsGrid}>
            <div className={styles.step}>
              <div className={styles.stepNumber}>01</div>
              <div className={styles.stepIcon}>📄</div>
              <h3 className={styles.stepTitle}>Upload Roadmap</h3>
              <p className={styles.stepDesc}>
                Paste your learning roadmap in tree format or upload a text file.
                Our parser understands hierarchical chapter structures
                automatically.
              </p>
            </div>

            <div className={styles.stepConnector}>
              <div className={styles.connectorLine} />
              <div className={styles.connectorArrow}>→</div>
            </div>

            <div className={styles.step}>
              <div className={styles.stepNumber}>02</div>
              <div className={styles.stepIcon}>🎯</div>
              <h3 className={styles.stepTitle}>Pick a Chapter</h3>
              <p className={styles.stepDesc}>
                Browse the parsed roadmap tree, explore sub-chapters, and select
                exactly what you want notes for. Process one chapter or queue
                many.
              </p>
            </div>

            <div className={styles.stepConnector}>
              <div className={styles.connectorLine} />
              <div className={styles.connectorArrow}>→</div>
            </div>

            <div className={styles.step}>
              <div className={styles.stepNumber}>03</div>
              <div className={styles.stepIcon}>🚀</div>
              <h3 className={styles.stepTitle}>Generate & Download</h3>
              <p className={styles.stepDesc}>
                Watch real-time progress as AI generates notes for each
                sub-chapter in parallel. Download individual files or a merged
                chapter document.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className={styles.features}>
        <div className="container">
          <h2 className={styles.sectionTitle}>
            Built for <span className="gradient-text">Serious Learners</span>
          </h2>

          <div className={styles.featureGrid}>
            <div className={`glass-panel ${styles.featureCard}`}>
              <div className={styles.featureIcon}>🤖</div>
              <h3>Two-Pass AI Generation</h3>
              <p>
                First generates an expert prompt template, then uses it to create
                comprehensive notes — producing deeper, more structured content
                than a single prompt.
              </p>
            </div>

            <div className={`glass-panel ${styles.featureCard}`}>
              <div className={styles.featureIcon}>⚡</div>
              <h3>Parallel Processing</h3>
              <p>
                Generate up to 4 sub-chapters simultaneously. A built-in
                concurrency manager queues excess work automatically — no
                overload, no crashes.
              </p>
            </div>

            <div className={`glass-panel ${styles.featureCard}`}>
              <div className={styles.featureIcon}>📡</div>
              <h3>Real-Time Progress</h3>
              <p>
                WebSocket-powered live updates show exactly which sub-chapters
                are generating, queued, or complete. Never wonder &quot;is it
                stuck?&quot;
              </p>
            </div>

            <div className={`glass-panel ${styles.featureCard}`}>
              <div className={styles.featureIcon}>🔗</div>
              <h3>Smart Merging</h3>
              <p>
                Individual sub-chapter notes are automatically merged into a
                single chapter document, ordered by roadmap sequence with proper
                navigation.
              </p>
            </div>

            <div className={`glass-panel ${styles.featureCard}`}>
              <div className={styles.featureIcon}>📁</div>
              <h3>Any Roadmap Format</h3>
              <p>
                Supports tree-structured roadmaps in text format. Upload your own
                roadmap file or paste content directly — works with any subject.
              </p>
            </div>

            <div className={`glass-panel ${styles.featureCard}`}>
              <div className={styles.featureIcon}>🛡️</div>
              <h3>Error Recovery</h3>
              <p>
                If a sub-chapter fails, the rest keep going. Retry failed items
                individually. No more losing an entire run to one flaky API call.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className={styles.footer}>
        <div className="container">
          <div className={styles.footerContent}>
            <div className={styles.footerBrand}>
              <span className={styles.footerLogo}>📘 NoteFactory</span>
              <p className={styles.footerTagline}>
                AI-powered study notes for ambitious learners.
              </p>
            </div>
            <div className={styles.footerLinks}>
              <Link href="/generate">Generate Notes</Link>
              <Link href="/jobs">View Jobs</Link>
            </div>
          </div>
          <div className={styles.footerDivider} />
          <p className={styles.footerCopy}>
            © {new Date().getFullYear()} Note Factory. Built with ❤️ for
            students everywhere.
          </p>
        </div>
      </footer>
    </>
  );
}
