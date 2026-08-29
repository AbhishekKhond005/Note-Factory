# NoteFactory — Java/Spring Boot Architecture

## 0. Purpose and Scope

This document defines the target architecture for a **from-scratch Java/Spring Boot** rewrite of NoteFactory, an AI-powered study-material generation platform. The existing implementation (Go backend + Next.js frontend) is used only as a behavioral reference, not as a template to port line-for-line.

The Java rewrite has four goals:

1. Preserve the behavior that already works in the Go version.
2. Replace the current single-file, in-memory, single-process design with a properly layered, persistent, testable Java backend.
3. Make the AI generation layer **provider-independent** — the same workflow must run through either the OpenCode CLI or a direct LLM API (OpenAI-compatible, Anthropic, etc.) without changing any orchestration code.
4. Make the system **genuinely agentic**: a goal is decomposed into tasks, tasks are delegated to specialized agents, state is durable, outputs are validated, and failures are retried or repaired without a human babysitting every step.

This is an architecture document. It intentionally contains no code. Class names, package names, and diagrams below describe *responsibilities and boundaries*, not a literal implementation to copy — the implementation should adapt these where real constraints (library behavior, performance, team size) suggest a better shape.

---

## 1. Executive Summary

NoteFactory takes a broad learning goal, such as:

> "Teach me Spring Security from beginner to advanced."

and turns it into a structured set of study materials. The intended pipeline:

```
User Goal
   |
   v
Roadmap Planning  (AI: produces a chapter/sub-chapter/topic tree)
   |
   v
Validated Roadmap  (backend: parsed, structurally checked, persisted)
   |
   v
Generation Orchestrator  (backend: decomposes a chapter into per-sub-chapter tasks)
   |
   +-------------------+-------------------+
   |                   |                   |
   v                   v                   v
Sub-chapter Worker  Sub-chapter Worker  Sub-chapter Worker   (bounded concurrency)
   |                   |                   |
   v                   v                   v
Two-step generation: outline agent -> notes-writing agent
   |                   |                   |
   +-------------------+-------------------+
                        |
                        v
                 Validation / Retry
                        |
              +---------+---------+
              |                   |
           accept               reject
              |                   |
              v                   v
          Persist Artifact     Retry (bounded) or mark failed
                        |
                        v
                 Chapter Merge
                        |
                        v
              Downloadable Study Material (Markdown / ZIP)
```

The core architectural rule that shapes everything else:

> **The LLM never owns the application's source of truth.** The backend owns job state, task state, artifact state, retry counts, permissions, and workflow transitions. The LLM is a stateless capability the backend calls into and validates the output of.

Five concerns are kept strictly separate, and the seams between them are the actual architecture:

- **Business orchestration** — what has to happen, in what order, and what happens on failure.
- **Agent behavior** — how a bounded reasoning task is framed as a prompt/response contract.
- **Model access** — how a prompt is physically turned into a response (CLI subprocess vs. HTTP API).
- **Persistence** — what survives an application restart.
- **Delivery** — how the frontend observes progress and retrieves output.

---

## 2. What the Existing (Go) System Already Establishes

The current Go implementation is a useful behavioral baseline. It currently provides:

- A roadmap domain model: a root roadmap containing chapters, each with sub-chapters, each with topics (a strict 3-level tree).
- A parser that reads a Unicode box-drawing "tree" text format (as produced by `tree` or requested from the LLM) and turns it into that structured model, including numeric-prefix stripping (`01-`) and extension stripping (`.md`).
- A generation job model with statuses `pending`, `queued`, `running`, `complete`, `failed`, `cancelled`, tracked **in memory only** (a Go map protected by a mutex, capped at 100 stored jobs with oldest-terminal-job eviction).
- Per-sub-chapter progress tracking with a `step` field describing what stage of generation is in progress (e.g. "generating prompt", "generating notes").
- A global, process-wide semaphore bounding concurrent `opencode` subprocesses (`MaxParallel`, default 1) — the single most important safety mechanism in the current system, because each `opencode` invocation is an expensive external process that can exhaust memory if left unbounded.
- A roadmap-generation mode: one AI call that asks for a full tree given a topic, with a strict textual format contract.
- A quick-overview mode: one AI call producing a short (300–600 word), beginner-level summary — a fundamentally different generation depth than full notes.
- A two-stage note-generation flow per sub-chapter: step 1 asks the model to design a prompt/outline template for the topic; step 2 feeds that template back in to produce the actual notes. This is already a (small) multi-agent pattern, just not named or isolated as one.
- A defensive "summary detection" heuristic: if the model's response looks like a *description* of having written a file rather than the file's content (too short, or contains phrases like "saved to", "word count:"), the system scans the model's scratch working directory for a Markdown file it may have written directly, and uses that instead.
- Chapter-level merging: after all sub-chapters in a chapter finish, their Markdown files are concatenated (sorted by filename, which encodes roadmap order) into one chapter file.
- REST endpoints for roadmap CRUD/generation, job creation/inspection/cancellation, and note retrieval/download.
- A WebSocket hub that broadcasts progress events (`status`, `progress`, `complete`, `error`) to all connected clients, keyed by job ID.
- ZIP and Markdown download endpoints that read files directly off local disk.
- `opencode` invoked as an external OS process, with a fallback to a Docker-wrapped invocation if the native run fails with a detected quota/rate-limit error (429, "quota", "rate limit", "payment required", "exhausted").
- Deliberate memory-hygiene measures aimed at very small deployment targets (a 512MB instance): output buffers are capped at 16MB, `runtime.GC()` is forced and a fixed sleep is inserted between sub-chapter generations to let subprocess memory be reclaimed before the next one starts.

These are **behavioral precedents**, not constraints the Java version must literally reproduce. In particular:

- In-memory job state and local-disk artifacts are explicitly *not* to be carried forward as the target design — see §8 (Persistence).
- The fixed sleep/manual-GC memory hygiene is a symptom of running a subprocess-heavy Go binary on a 512MB PaaS instance. In Java, the equivalent concern (bounding subprocess memory) is handled by the concurrency/semaphore design (§13), not by manual GC hints.
- The two-step "outline, then write" flow becomes an explicit two-agent pipeline rather than two inline prompts inside one function (§10–§12).

---

## 3. Primary Design Principle: Separate the Application From the Model

```
                    NoteFactory Core (domain + orchestration)
                                |
                                | GenerationProvider (interface)
                                v
                  +--------------------------------+
                  |                                |
                  v                                v
          OpenCodeGenerationProvider        ApiGenerationProvider
          (spawns a CLI subprocess)         (HTTP call to an LLM API)
```

Nothing outside the provider implementations may know:

- that a subprocess is being spawned, or its working directory, environment variables, or CLI flags;
- provider-specific HTTP headers, SDK objects, or API keys;
- a specific model's response JSON shape.

The rest of the system deals exclusively in domain concepts: a generation *request* (goal + context + constraints), a *response* (text + metadata), a *task* (a unit of orchestrated work), an *agent* (a role that turns a task into a request/response pair), an *attempt* (one try at a task, which may fail), and an *artifact* (a persisted output).

This boundary is what allows OpenCode today, a direct API tomorrow, and a self-hosted model later, without touching the orchestrator, the agents' prompts-as-contracts, the job state machine, or the REST/WebSocket layer.

---

## 4. Is This Actually Agentic?

Calling something "agentic" because it calls an LLM is weak. The working definition used here:

> An agent is a bounded, autonomous component that receives a goal and context, produces a structured outcome (not just free text), and participates in a larger stateful workflow that an orchestrator — not the agent — controls.

NoteFactory qualifies under this definition when the pipeline has all of the following properties, each of which this architecture makes a first-class concern:

1. **Goal decomposition** — a broad topic becomes a roadmap; a roadmap chapter becomes a set of independent sub-chapter tasks.
2. **Delegation** — independent sub-chapter tasks are handed to specialist workers, not resolved inline.
3. **Statefulness** — task state, retry counts, and artifacts are durable, not held only in a request-handler's local variables.
4. **Tool use** — agents act only through the `GenerationProvider` and application-owned tools; they never touch the database, the filesystem, or job state directly.
5. **Feedback** — outputs pass through a validation step; validation failure triggers a bounded retry or repair, not silent acceptance.
6. **Partial execution** — one sub-chapter failing does not invalidate sub-chapters that already succeeded.
7. **Supervision** — the orchestrator decides what runs next, in what order, and with what concurrency; agents never call each other directly.
8. **Provider independence** — none of the above properties change if the underlying model runtime changes.

That is the bar this architecture is designed to clear, and it is the reason the orchestrator/agent/provider split in §3, §10–§14 is treated as non-negotiable rather than a style preference.

---

## 5. Architectural Goals

### Functional
- Create a learning roadmap for a topic via AI generation.
- Accept and validate a user-uploaded roadmap file.
- Parse a tree-formatted roadmap into a structured domain model.
- Generate study notes for one, several, or all sub-chapters of a chapter.
- Generate a short beginner-level overview for a topic (a distinct, cheaper generation mode).
- Track progress at job, chapter, and sub-chapter granularity.
- Support cancellation of queued or in-flight work.
- Serve generated notes for in-app viewing.
- Support Markdown (single file) and ZIP (full set) downloads.
- Support regenerating a specific failed or unsatisfactory sub-chapter without re-running the whole chapter.
- Support multiple LLM backends without any change to orchestration or agent code.

### Non-functional
- Job, task, and artifact state must survive an application restart.
- Expensive AI work must be bounded by an explicit, configurable concurrency limit — this is the single most important operational safety property in the current system and must be preserved and strengthened, not weakened.
- No component may spawn processes or API calls without going through the bounded scheduler.
- Large generated documents should not be forced to live entirely in JVM heap when a streaming or reference-based approach is possible.
- The design must not preclude running more than one backend instance later, even though the initial deployment target is a single instance.
- Every agent invocation must be observable: what was asked, what came back, how long it took, whether it was accepted.
- Orchestration must be deterministic and testable even though the thing it orchestrates (an LLM) is not.
- A failure inside one provider implementation must not corrupt or hang unrelated jobs.
- The system should be understandable end-to-end by a single engineer reading this document plus the codebase.

---

## 6. High-Level System Architecture

```
+-----------------------------------------------------------------------------+
|                              Next.js Frontend                               |
|   Roadmap upload/visualize · Chapter/section picker · Generation dashboard  |
|                     REST calls  +  WebSocket subscription                    |
+------------------------------------+----------------------------------------+
                                      |
                                      v
+-----------------------------------------------------------------------------+
|                          Spring Boot Application                            |
|                                                                               |
|  +------------------+     +-----------------------+     +----------------+  |
|  | API Layer         | --> | Application Services  | --> | Domain Layer   |  |
|  | REST controllers   |     | (use-case orchestration|    | Entities,      |  |
|  | WebSocket endpoint  |     |  job/task lifecycle)   |    | state machines |  |
|  +------------------+     +-----------------------+     +----------------+  |
|                                      |                                       |
|                                      v                                       |
|                     +-----------------------------------+                   |
|                     |     Generation Orchestrator        |                   |
|                     |  task graph, scheduling, retries    |                   |
|                     +-----------------+-------------------+                  |
|                                       |                                       |
|                                       v                                       |
|                     +-----------------------------------+                   |
|                     |            Agents                  |                   |
|                     |  Planner / Outliner / Writer /     |                   |
|                     |  Critic / Repair                    |                   |
|                     +-----------------+-------------------+                  |
|                                       |                                       |
|                                       v                                       |
|                     +-----------------------------------+                   |
|                     |      GenerationProvider (SPI)      |                   |
|                     +----------+--------------+-----------+                   |
|                                |              |                              |
|                                v              v                              |
|                     +------------------+ +------------------+               |
|                     | OpenCode Provider | | Direct API       |               |
|                     | (subprocess)      | | Provider (HTTP)  |               |
|                     +------------------+ +------------------+               |
+------------------------------------+----------------------------------------+
                                      |
             +------------------------+------------------------+
             |                        |                          |
             v                        v                          v
     +---------------+       +----------------+         +------------------+
     | PostgreSQL     |       | Redis          |         | Artifact Store   |
     | jobs/tasks/    |       | queue signal,  |         | Markdown files,  |
     | attempts/      |       | pub/sub for WS |         | ZIP bundles      |
     | roadmaps       |       | fan-out, cache |         | (local / S3)     |
     +---------------+       +----------------+         +------------------+
```

Redis is shown as part of the target architecture but is **not required for the first implementation slice** (see §22, "Recommended Build Order"). A single-instance deployment can run entirely on Spring's in-process scheduling plus PostgreSQL, with Redis introduced only when a second instance is added.

---

## 7. Layering and Module Boundaries

The application is a **modular monolith**: one deployable Spring Boot artifact, internally partitioned into modules with a strict, one-directional dependency graph. This is deliberate — see §26 for the reasoning against microservices at this stage.

Layers, outer to inner:

1. **API module** — REST controllers, WebSocket endpoint, request/response DTOs, input validation. Depends on the application layer only. Knows nothing about JPA entities, agents, or providers.
2. **Application module** — use-case services (`CreateGenerationJob`, `CancelJob`, `GetJobStatus`, `DownloadArtifact`, …). Depends on the domain module and the orchestrator. Owns transaction boundaries.
3. **Orchestration module** — the `GenerationOrchestrator`, task scheduling, retry policy, concurrency control. Depends on the domain module and the agent module's *interfaces* only.
4. **Agent module** — one class per agent role, each implementing a common `Agent` contract. Depends on the domain module and the provider module's *interface* only, never on a concrete provider.
5. **Provider module** — `GenerationProvider` interface plus the OpenCode and Direct-API implementations. Depends on nothing above it.
6. **Domain module** — entities, value objects, the task/job state machines, domain events. Depends on nothing else in the application (no Spring, no JPA annotations bleeding into pure domain logic where practical).
7. **Persistence module** — JPA repositories, entity-to-domain mapping, migration scripts. Implements repository interfaces declared in the domain/application layer (dependency inversion).
8. **Infrastructure module** — filesystem/S3 artifact storage, WebSocket broadcasting, external process execution helpers, metrics/logging wiring.

Dependency direction is strictly inward: API → Application → Orchestration/Domain → (interfaces implemented by) Provider/Persistence/Infrastructure. Nothing in Domain, Orchestration, or Agent may import a Spring web annotation, a JPA annotation, or a subprocess API directly.

---

## 8. Persistence: Promoting In-Memory State to Durable State

The most important structural change from the Go version is this: **job state, task state, and artifact metadata move from an in-memory map to PostgreSQL.** This single change is what makes restart recovery, horizontal scaling, and auditability possible at all — none of those are achievable with a mutex-guarded map, no matter how well-written.

What is persisted:

- **Roadmap** — title, source (uploaded vs. AI-generated), raw text, parsed structure, creation time.
- **GenerationJob** — the top-level unit the frontend created (a chapter generation, a multi-chapter generation, or an overview generation): status, requested scope, timestamps, owning roadmap reference.
- **GenerationTask** — one task per sub-chapter (or per overview): status, assigned agent role, step description, retry count, current attempt reference, output artifact reference, error detail.
- **GenerationAttempt** — one row per actual model invocation for a task: which provider, which model, prompt reference, response reference (or pointer to it), duration, outcome (accepted/rejected/errored), validation notes.
- **Artifact** — a generated document's metadata: logical name, storage location, size, checksum, which task/attempt produced it, and its relationship to a merged chapter artifact if applicable.

What is *not* persisted in the relational database:

- Full prompt and response bodies for every attempt are not required to live in a relational column forever — they can be written to the artifact store (or a dedicated "raw output" location) with only a pointer kept in `GenerationAttempt`, keeping the database itself lean. Retention policy for raw prompts/responses is a configuration decision (§27), not an architectural one.

What Redis is for (once introduced): short-lived coordination — a pub/sub channel so any backend instance can broadcast a WebSocket progress event regardless of which instance is handling that job's WebSocket connection, and (optionally) a lease mechanism so two instances never claim the same queued task. Redis is not the system of record for anything; PostgreSQL is.

The filesystem (or S3-compatible object storage) holds the actual generated Markdown and ZIP bytes, referenced by path/key from the `Artifact` table. This mirrors what the Go version already does for file storage — that part of the design is sound and is preserved, only the *bookkeeping* about those files moves out of memory and into the database.

---

## 9. Core Domain Model

- **Roadmap** — `title`, ordered list of `Chapter`.
- **Chapter** — `name`, ordered list of `SubChapter`.
- **SubChapter** — `name`, ordered list of `Topic` (plain strings describing concrete learning points).
- **GenerationJob** — the unit a user-facing action creates. Has a `JobStatus` (see §11) and one or more `GenerationTask`s.
- **GenerationTask** — one per sub-chapter (or the single "overview" task for overview jobs). Has its own status, independent of sibling tasks.
- **GenerationAgent** (role, not a Spring bean necessarily 1:1) — a named capability: `RoadmapPlanner`, `OutlineAgent`, `NoteWriterAgent`, `CriticAgent`, `RepairAgent`, `OverviewAgent`.
- **GenerationAttempt** — one execution of an agent against a task; carries the outcome and, on failure, the reason.
- **GenerationRequest / GenerationResponse** — the provider-facing contract: a request carries the assembled prompt plus generation parameters (model hint, max tokens, temperature policy); a response carries raw text plus provider metadata (latency, token counts if available, which provider handled it).
- **Artifact** — a persisted output: a single sub-chapter's notes file, a merged chapter file, or a ZIP bundle.

This mirrors the existing Go `RoadMap`/`Chapter`/`SubChapter` tree faithfully (§2), while adding the `Task`/`Attempt`/`Agent`/`Artifact` vocabulary needed to make the workflow durable and agentic rather than a single procedural function.

---

## 10. Agent Architecture

Every agent implements the same shape of contract: given a goal plus bounded context, produce either a structured success outcome or a structured failure outcome — never silent text that the rest of the system has to sniff for meaning (the Go version's `isSummaryResponse` heuristic is a symptom of *not* having this contract; see §12 for how the Java version replaces the heuristic).

Agents in the pipeline:

- **RoadmapPlannerAgent** — turns a topic (plus optional user priority guidance) into a tree-structured roadmap proposal. Equivalent to the Go `GenerateRoadmap` call, but its output goes through the Roadmap Validation Pipeline (§16) instead of being trusted as-is.
- **OutlineAgent** — given a sub-chapter's name and topic list, produces a detailed content outline/prompt-template for that sub-chapter. Equivalent to the Go version's "step 1" prompt-template generation.
- **NoteWriterAgent** — given the outline produced above, writes the actual Markdown study notes. Equivalent to the Go version's "step 2" notes generation.
- **CriticAgent** — evaluates a `NoteWriterAgent` output against explicit acceptance criteria (structure present, minimum length, no leftover placeholder text, no "I saved this to a file" narration, matches requested topics) and returns an accept/reject verdict with reasons. This does not need to be a second LLM call for every criterion — many of the criteria are deterministic checks the orchestrator can run without invoking a model at all (see §16); an LLM-backed critic is reserved for qualitative judgment where deterministic checks are insufficient.
- **RepairAgent** — given a rejected output plus the critic's stated reasons, produces a corrected version, or explicitly reports "not repairable" so the orchestrator can fall back to a full retry instead of looping on a bad attempt.
- **OverviewAgent** — the short, beginner-level, single-call generation mode. Kept as its own agent (not a shorter path through `NoteWriterAgent`) because its acceptance criteria and prompt intent are genuinely different (brevity is a requirement, not a shortcoming).

None of these agents ever call the provider directly by name — they depend on the `GenerationProvider` interface and are handed a provider instance (or the orchestrator calls the provider on the agent's behalf, passing the assembled request). Agents own **prompt construction and response interpretation**; they do not own **process execution or HTTP transport**.

---

## 11. Task and Job State Machines

**Job status** (top-level, user-visible): `PENDING → QUEUED → RUNNING → COMPLETE | FAILED | CANCELLED`. A job's terminal status is derived from its tasks: complete if all required tasks succeeded, complete-with-partial-failure if some succeeded and some did not (surfaced distinctly to the frontend rather than silently reported as full success — the Go version already reports "N/M sub-chapters generated successfully", and this distinction should be a first-class status rather than a message string), failed if none succeeded, cancelled if the user cancelled before completion.

**Task status** (per sub-chapter): `PENDING → QUEUED → RUNNING → VALIDATING → COMPLETE | FAILED | CANCELLED`, with an explicit `RETRYING` state entered when a validation rejection triggers another attempt within the retry budget.

Rules that must hold regardless of implementation detail:

- A task can only move forward through this machine; no transition skips validation on the success path.
- Cancellation is cooperative: a task already mid-attempt finishes that attempt (it isn't safe or necessary to hard-kill a subprocess mid-write), but no *new* attempt is started once cancellation is observed. This matches the Go version's existing cancellation check points and generalizes them into a state-machine guard evaluated before every attempt.
- A job never reports `COMPLETE` while any of its tasks are non-terminal.
- Every state transition is a single atomic database update, not a read-then-write from application code without a transaction boundary — this prevents the class of race condition that an in-memory mutex-guarded map (the current Go design) is otherwise fine for, but which becomes a real risk once more than one thread — or eventually more than one instance — can touch the same job.

---

## 12. Replacing the "Summary Response" Heuristic With a Real Validation Contract

The Go version detects that a model "talked about" saving a file instead of returning its content by string-matching phrases like "saved to" or checking `len(output) < 500`, then falls back to scanning the model's temp working directory for a Markdown file. This works, but it is a heuristic patch over a missing contract.

In the Java architecture this becomes explicit, layered validation, run by the orchestrator after every attempt and before an artifact is persisted:

1. **Structural validation** (deterministic, no model call): output is non-empty, exceeds a minimum length appropriate to the generation mode (overview vs. full notes have different thresholds), does not consist primarily of narration-about-writing-a-file phrases, and — for overview/notes — contains at minimum the requested topic name.
2. **Format validation** (deterministic): the response is valid Markdown-ish text (no unclosed code fences, no leftover prompt-template placeholders like `{{TOPIC}}`).
3. **Semantic validation** (optional, model-backed): the `CriticAgent` is invoked only when steps 1–2 pass but a qualitative judgment is still warranted (e.g. "does this actually cover the requested topics", "is this Java-focused as requested"). This is the expensive path and should be skippable via configuration for cost-sensitive deployments.

A response that fails structural or format validation is rejected without ever reaching a human or being written to disk as a final artifact, and triggers either a repair attempt or a full retry (§13). This removes the need for the "scan the temp directory for a stray file" workaround entirely, because the generation prompt/response contract is enforced up front rather than inferred after the fact. (If a provider implementation still writes files as a side effect rather than returning content directly — which is possible with agentic CLI tools — the provider layer, not the agent or orchestrator layer, is responsible for normalizing that into a single returned string; see §14.)

---

## 13. Orchestration, Scheduling, and Retry

The **GenerationOrchestrator** is the only component allowed to decide *when* an agent runs. Its responsibilities:

- Decompose a job into tasks (one per sub-chapter, or a single task for overview jobs) at job-creation time, persisting all tasks up front in `PENDING` status — not lazily as work happens to get to them. This makes "how much work is left" answerable from the database alone, with no in-memory bookkeeping.
- Claim tasks for execution respecting the global concurrency budget (the direct successor to the Go version's `jobSem` channel). In the single-instance deployment this is an in-process bounded executor; once more than one instance exists, claiming becomes a transactional "claim the next pending task" query (or a Redis-backed lease) so two instances never run the same task twice.
- Invoke the appropriate agent(s) for a task, in order (Outline → Write → Validate → maybe Repair), recording a `GenerationAttempt` for each model call.
- Apply retry policy: a bounded number of attempts (configurable, small — e.g. 2–3) before a task is marked `FAILED`. A repair attempt (cheaper, targeted) is preferred over a full retry (expensive, from scratch) when the critic's rejection reason is specific enough to repair.
- Trigger chapter merge once all of a chapter's tasks reach a terminal state, mirroring the Go version's "merge after all sub-chapters finish" behavior, but as an explicit orchestrator-driven step rather than something the HTTP handler does inline after `sync.WaitGroup.Wait()`.
- Emit domain events (`TaskStarted`, `TaskProgressed`, `TaskCompleted`, `TaskFailed`, `JobCompleted`, …) that the delivery layer (§17) translates into WebSocket messages. The orchestrator does not know about WebSockets at all — that is a hard boundary.

This is where "task graph vs. a simple thread pool" matters: the Go version's per-chapter goroutine fan-out with a `sync.WaitGroup` is a thread pool, not a task graph — it works because sub-chapters are truly independent, but it cannot express dependency (e.g. "merge waits for all sub-chapters") as anything other than code sequencing, and it holds no state if the process restarts mid-generation. The orchestrator described here expresses "merge depends on all sub-chapter tasks" as a persisted, checkable fact, which is what makes restart recovery a matter of "resume scheduling from the database" rather than "the job silently vanished."

---

## 14. Generation Provider Abstraction

```
GenerationProvider (interface)
   generate(GenerationRequest) -> GenerationResponse
   |
   +-- OpenCodeGenerationProvider
   |     - allocates an isolated temporary working directory per call
   |     - invokes the OpenCode CLI as a subprocess with the assembled prompt
   |     - enforces a process timeout and an output-size cap (mirrors the
   |       Go version's 16MB limited buffer, generalized as configuration)
   |     - normalizes CLI output: strips ANSI codes, unwraps markdown code
   |       fences, and — if the CLI wrote a file to the working directory
   |       instead of returning content on stdout — reads that file and
   |       returns its content as the response, so this quirk is fully
   |       contained inside this one provider and invisible to agents
   |     - classifies subprocess failures (quota/rate-limit vs. genuine
   |       error) so the orchestrator can decide whether a retry is even
   |       worth attempting
   |
   +-- ApiGenerationProvider
         - calls a remote LLM HTTP API (OpenAI-compatible, Anthropic, etc.)
         - owns auth, timeout, and retry-on-transient-network-error policy
         - maps provider-specific response shapes into the common
           GenerationResponse
```

Provider selection is configuration, not code — a deployment picks one provider (or, later, different providers per agent role, e.g. a cheap model for outline generation and a stronger model for note writing) via application configuration, and nothing in the orchestrator or agent layer changes.

The **Docker-wrapped fallback execution** the Go version uses as a quota-limit workaround (retry the same prompt inside a container with different network egress) is preserved as an *optional secondary strategy inside `OpenCodeGenerationProvider`*, not promoted to a generic system-wide fallback — it is specifically a workaround for one provider's failure mode and should stay scoped there.

---

## 15. Concurrency and Resource Control

The Go version's single global semaphore bounding all `opencode` subprocesses is the correct instinct and is preserved, but generalized:

- A **global generation concurrency limit** bounds how many agent invocations (of any kind, from any job) may be in flight at once, configurable per deployment (a laptop can afford 4; a small server should stay at 1–2). This is the direct successor to `MaxParallel` / `jobSem`.
- A **per-job concurrency limit** (optional, defaults to "no additional limit beyond the global one") prevents one very large chapter from starving every other job's tasks of the global budget.
- Task claiming and slot acquisition happen together, transactionally, so a task is never marked `RUNNING` in the database without actually having secured a concurrency slot — avoiding a class of bug where job state and actual execution state can drift apart.
- Backpressure is explicit: when the global limit is saturated, newly created tasks sit in `QUEUED`/`PENDING` and the frontend is told a queue position (mirroring the Go version's `QueuePos` field on `ProgressEvent`), rather than the request blocking or being rejected.

Manual GC hints and fixed sleeps between generations (present in the Go version as a workaround for a 512MB PaaS target) are **not** part of the Java design. The equivalent safety property — bounding how much subprocess/heap memory is in use at once — is achieved structurally by the concurrency limit itself; the JVM's own GC and the OS's process lifecycle handle reclamation without needing to be told when to run.

---

## 16. Roadmap Parsing and Validation

The roadmap parser is preserved essentially as-is in behavior, because it is already a well-scoped, well-tested piece of the current system (§2): it reads a 3-level Unicode tree (chapter / sub-chapter / topic), strips numeric ordering prefixes and file extensions from names, and rejects nesting deeper than 3 levels with a clear line-numbered error. In the Java version this becomes a standalone parser component with no framework dependency, unit-tested against the same category of inputs the current `parser_test.go` already covers (well-formed trees, missing root, over-deep nesting, mixed prefixes).

What is added on top, as a **Roadmap Validation Pipeline** that AI-generated roadmaps must pass before being offered to the user as final (uploaded/user-authored roadmaps skip straight to structural parsing since a human already made the judgment call):

- Structural validity (parses at all, respects depth limits) — reuses the parser above.
- Reasonable shape (chapter count and sub-chapter count fall within the same sane bounds the current prompt already requests — 8–16 chapters, 3–6 sub-chapters — checked here rather than trusted from the prompt alone).
- No duplicate chapter/sub-chapter names at the same level.
- No empty chapters (a chapter with zero sub-chapters is a planning failure, not a valid roadmap).

A roadmap that fails validation is **not** silently accepted; the orchestrator can retry the `RoadmapPlannerAgent` once with the validation failure appended as corrective guidance before giving up and surfacing an error to the user — the same repair-before-fail philosophy used for note generation (§13).

---

## 17. API and Real-Time Delivery

### REST surface (functionally equivalent to the current API, reshaped around persisted resources)
- Roadmap resource: list, fetch by id, parse raw text, upload, AI-generate.
- Job resource: create (chapter generation, selected-sub-chapters generation, or overview generation), list, fetch by id, cancel.
- Task resource: fetch by id (enables regenerating a single failed sub-chapter without recreating the whole job — an explicit improvement over the current all-or-nothing chapter job).
- Artifact resource: fetch notes content, download single-file Markdown, download ZIP bundle.
- System resource: health, current concurrency/queue status.

### WebSocket
A single subscription channel per job (or a general firehose the frontend filters client-side, matching current behavior) delivering the same event vocabulary the Go version already defines: status changed, sub-chapter progress, sub-chapter complete/error, job complete. The orchestrator emits domain events; a thin delivery adapter translates domain events into WebSocket messages. This adapter is the *only* place that needs to change if the transport (WebSocket vs. Server-Sent Events vs. something else) ever changes.

### Command/query separation
Endpoints that start work (`POST /jobs`, `POST /roadmaps/generate`) return immediately with the created resource in a queued/pending state; they never block on generation completing. Endpoints that read state never trigger work as a side effect. This is already true of the Go version's design and is preserved deliberately.

---

## 18. Security and Trust Boundaries

- **Prompt injection boundary**: content coming from an uploaded roadmap, or from within generated notes, is *data*, never re-interpreted as an instruction to the orchestrator. Agents may read topic names and outlines as content to write about; they are never given the ability to affect job/task state, trigger other jobs, or change configuration as a side effect of what they return. The `GenerationResponse` contract (§10, §14) is intentionally "just text plus metadata" so there is no path from model output to privileged action.
- **Provider secret management**: API keys and OpenCode auth configuration live in environment/config, never in the database, never in a prompt, and never logged.
- **Filesystem boundary for the OpenCode provider**: each subprocess invocation runs inside an isolated temporary working directory that is deleted afterward; the process is never given the application's own working directory or credentials directory.
- **Upload validation**: uploaded roadmap files are size-capped and content-type-checked before being persisted or parsed.
- **No authentication is assumed to exist in the MVP** (matching the current local-first Go deployment), but the module boundaries above (API layer strictly separate from application services) are exactly what makes adding authentication/authorization later a matter of adding a filter/interceptor at the API layer, not a redesign.

---

## 19. Observability

- **Structured logs** at each state transition (task claimed, attempt started/finished, validation verdict, job completed) carrying job/task/attempt IDs so a single generation can be traced end-to-end across log lines.
- **Metrics**: active generation count vs. configured limit, queue depth, attempt success/failure rate per agent role, attempt latency per provider, retry rate, merge success rate. These are the numbers that answer "is the system healthy" and "is the model quality degrading" respectively.
- **Health/readiness**: a liveness check (is the process up) separate from a readiness check (can it reach its database and, ideally, verify the configured provider is reachable) — the current Go `/api/health` is a liveness check only and should stay simple, with readiness added as its own endpoint.
- Raw prompts and responses are logged/stored with a retention policy, not indefinitely by default, since they can be large and may contain the user's topic content verbatim — this is a configuration knob, not a hardcoded choice.

---

## 20. Frontend Integration

The Next.js frontend's existing contract (roadmap upload/visualize, chapter/section picker, generation dashboard with live progress, jobs list) is preserved as the target UX. The frontend does not need to change its mental model — job → per-sub-chapter progress → merged download — because the REST/WebSocket surface described in §17 is designed to be response-compatible in shape with what `web/app/lib/api.js` already calls. The only user-visible additions the new backend enables are: resuming to see a job's progress after a backend restart (impossible with the current in-memory design), and regenerating a single failed sub-chapter instead of the whole chapter.

---

## 21. Technology Mapping

| Concern | Technology |
|---|---|
| Web/API layer | Spring Web (REST controllers), Spring WebSocket |
| Application/domain services | Plain Spring `@Service` beans, framework-light domain classes |
| Persistence | Spring Data JPA + PostgreSQL; Flyway (or Liquibase) for schema migrations |
| Task scheduling / concurrency | Java `ExecutorService` with a bounded pool sized from configuration (single instance); a transactional "claim next pending task" pattern (multi-instance) |
| Cross-instance coordination (later) | Redis: pub/sub for WebSocket fan-out, optional lease-based task claiming |
| Subprocess execution (OpenCode provider) | `ProcessBuilder`, with explicit timeout handling and bounded output capture (Java equivalent of the Go `limitedBuffer`) |
| Direct API provider | A minimal HTTP client (Java `HttpClient` or a small SDK) per supported provider |
| Artifact storage | Local filesystem initially (mirrors current behavior); an `ArtifactStore` interface allows swapping in S3-compatible storage without touching callers |
| Observability | Spring Boot Actuator, Micrometer, structured logging (e.g. JSON log encoder) |
| Testing | JUnit 5 for unit tests; Testcontainers for PostgreSQL/Redis integration tests; a fake `GenerationProvider` implementation for deterministic orchestrator/agent tests that never call a real model |

Spring's job is deliberately narrow: dependency wiring, transaction management, REST/WebSocket transport, and JPA persistence. It should not leak into the domain/orchestration/agent modules as annotations scattered through business logic (§7).

---

## 22. Recommended Build Order

Building this in one shot is the wrong approach; each stage below should be a working, demoable system before the next stage starts.

**Stage 1 — Minimal Viable Architecture (single instance, no agents-as-separate-classes yet is fine internally, but the seams from §3/§7 must already exist):**
Next.js → Spring Boot → PostgreSQL for job/task state → an in-process orchestrator running the outline-then-write flow → OpenCode provider only → local filesystem artifacts → WebSocket progress. This is functionally equivalent to the current Go system, but durable.

**Stage 2 — Real agent boundaries and validation:**
Split the two-step flow into explicit `OutlineAgent`/`NoteWriterAgent` classes behind the `Agent` contract; add the `CriticAgent` (deterministic checks first, model-backed check optional); add `RepairAgent`; add the Roadmap Validation Pipeline (§16) for AI-generated roadmaps.

**Stage 3 — Provider independence proven, not just designed:**
Add `ApiGenerationProvider` as a second, real implementation and prove that switching providers via configuration alone changes nothing else — this is the checkpoint that validates §3 was actually followed rather than just described.

**Stage 4 — Recovery and horizontal readiness:**
Add restart-recovery (resume scheduling of any task left `RUNNING`/`QUEUED` at process start), transactional task claiming, and — only once a second instance is actually needed — Redis-backed WebSocket fan-out and/or task leasing.

Each stage should have its own passing test suite before the next stage begins; §21's fake-provider pattern makes Stages 1–3's orchestration logic testable without ever calling a real model or spawning a real subprocess.

---

## 23. Testing Strategy

- **Unit tests**: parser (reuse the existing Go test cases as a spec — well-formed trees, missing root line, over-deep nesting, numeric-prefix and extension stripping), state machine transition legality, retry/repair decision logic, structural/format validation rules (§12).
- **Provider contract tests**: a shared test suite run against both `OpenCodeGenerationProvider` and `ApiGenerationProvider` (and the fake provider used elsewhere) asserting they all satisfy the same `GenerationProvider` contract — same timeout behavior, same error classification shape — so a provider swap can never silently change orchestrator-visible behavior.
- **Integration tests**: full job lifecycle against a real (Testcontainers) PostgreSQL instance, using the fake provider so tests are fast and deterministic; specifically covering partial failure (some sub-chapters succeed, some fail), cancellation mid-job, and restart recovery (kill and restart the orchestrator mid-job, assert it resumes correctly).
- **Concurrency tests**: assert the global limit is never exceeded under load, and that queued tasks eventually run once a slot frees up.

---

## 24. Deployment

Two supported modes, mirroring the current Go deployment options:

- **Local/OpenCode mode** — single JVM process, single PostgreSQL instance (can be embedded/local for development), OpenCode CLI available on PATH or Docker for the quota-fallback path. This is the default developer and "run on my machine" experience, matching the current README's local-first design.
- **Direct-API mode** — same JVM process, `ApiGenerationProvider` configured with a real provider's API key, no dependency on a local CLI binary or Docker at all — suitable for a small always-on server deployment where installing/maintaining an interactive CLI tool is undesirable.

Both modes share the same application artifact and the same database schema; the only difference is provider configuration (§14), which is exactly the point of the provider abstraction.

---

## 25. Why a Modular Monolith, Not Microservices, First

Splitting the orchestrator, agents, and providers into separate deployable services now would add network calls, serialization, and distributed-failure handling to a system that doesn't yet have enough load or team size to justify it, and — more importantly — would obscure the actual architectural achievement here, which is the *internal* separation between orchestration, agent behavior, and model access (§3–§4). A modular monolith with strict internal module boundaries (§7) gets all of the design benefit (testability, provider independence, replaceability) without the operational cost, and can be split into services later precisely *because* the boundaries were enforced as module boundaries from day one, not because services were introduced speculatively up front.

---

## 26. Final Architectural Statement

> NoteFactory is a durable, agentic content-generation workflow engine, delivered as a full-stack learning-material product, in which specialized agents perform bounded AI tasks under the control of a deterministic orchestrator, and a provider abstraction lets the identical workflow run through either a local OpenCode CLI process or a direct LLM API call — with job, task, and artifact state owned entirely by the application, never by the model.

The strongest single design decision in this document is the layered separation of *what* the application wants to accomplish (orchestration), from *how* an agent reasons about one bounded piece of it (agent behavior), from *how* a specific model is invoked (provider). That separation is what lets OpenCode-today become direct-API-tomorrow become a self-hosted model later, without rewriting the workflow — and it's what turns "a Spring Boot app that calls an LLM" into an actual workflow engine with AI-backed steps.
