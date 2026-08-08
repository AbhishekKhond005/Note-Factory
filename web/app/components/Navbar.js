"use client";
import { usePathname } from "next/navigation";
import Link from "next/link";
import styles from "./Navbar.module.css";

export default function Navbar() {
  const pathname = usePathname();

  return (
    <nav className={styles.navbar}>
      <div className={styles.inner}>
        <Link href="/" className={styles.logo}>
          <span className={styles.logoIcon}>📘</span>
          <span className={styles.logoText}>
            Note<span className={styles.logoAccent}>Factory</span>
          </span>
        </Link>

        <div className={styles.links}>
          <Link
            href="/generate"
            className={`${styles.link} ${
              pathname === "/generate" ? styles.active : ""
            }`}
          >
            <span className={styles.linkIcon}>⚡</span>
            Generate
          </Link>
          <Link
            href="/jobs"
            className={`${styles.link} ${
              pathname === "/jobs" ? styles.active : ""
            }`}
          >
            <span className={styles.linkIcon}>📋</span>
            Jobs
          </Link>
        </div>

        <a
          href="https://github.com"
          target="_blank"
          rel="noopener noreferrer"
          className={styles.ghLink}
        >
          GitHub
        </a>
      </div>
    </nav>
  );
}
