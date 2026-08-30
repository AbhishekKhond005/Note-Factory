# Java Backend: Full Setup + Notes Path Fix

## Problems to Solve

### 1. Backend won't start — PostgreSQL required
The app is wired to PostgreSQL (Flyway + JPA). There's no local Postgres running.
**Fix:** Add an `application-dev.properties` profile that swaps Postgres for an **H2 in-memory database** with a Flyway-compatible dialect, so `mvn spring-boot:run -Dspring-boot.run.profiles=dev` starts with zero external dependencies.

> [!IMPORTANT]
> H2 doesn't support Postgres-specific SQL (`uuid-ossp`, `JSONB`). The dev profile will use a separate `V1__init-h2.sql` migration that replaces those types with H2-compatible equivalents (`UUID`, `CLOB`).

### 2. Notes saved in wrong folder
`ArtifactStorageService` uses `${notefactory.storage.notes-dir:notes}` — a **relative path** that resolves inside `backend/`, not the project root.
**Fix:** Wire `notefactory.storage.notes-dir` in `application.properties` to the absolute path:
```
/home/abhishek/Coding/RESUME PROJECTS/Note-Factory/notes
```
And fix the `sanitizeFilename` bug that replaces `-` with `_` (breaks `session-security` → `session_security`).

### 3. Roadmap file serving
The roadmap files live in `Note-Factory/roadmaps/`. The app currently has no `RoadmapFile` service to load `.txt` files from disk for generation. A `notefactory.storage.roadmaps-dir` property + a new `RoadmapFileService` will let us load and feed roadmaps from that directory.

---

## Proposed Changes

### `backend/src/main/resources/application.properties`
- **[MODIFY]** Add DB config, notes-dir, roadmaps-dir

### `backend/src/main/resources/application-dev.properties`
- **[NEW]** H2 in-memory datasource, H2 Flyway locations override

### `backend/src/main/resources/db/migration/h2/V1__init-h2.sql`
- **[NEW]** H2-compatible schema (no `uuid-ossp`, no `JSONB`)

### `backend/src/main/java/com/example/notefactory/service/ArtifactStorageService.java`
- **[MODIFY]** Fix `sanitizeFilename` to preserve `-` in kebab-case names

### `backend/src/main/java/com/example/notefactory/service/RoadmapFileService.java`
- **[NEW]** Load roadmap `.txt` files from the configured `roadmaps-dir` and return parsed `Roadmap` objects (without DB persistence — for on-the-fly generation)

### `backend/src/main/java/com/example/notefactory/web/JobController.java`
- **[MODIFY]** Add a `/api/jobs/generate-from-file` endpoint that accepts a filename + optional chapterIndex, loads the roadmap from disk via `RoadmapFileService`, and kicks off a generation job

### `backend/pom.xml`
- **[MODIFY]** Add H2 test-scope dependency

## Verification Plan
1. `mvn spring-boot:run -Dspring-boot.run.profiles=dev` starts without errors
2. `POST /api/jobs/generate-from-file` with `SpringSecurity.txt` + `chapterIndex=5` (session-security) creates a job
3. Notes appear in `Note-Factory/notes/spring-security-roadmap/session-security/`
