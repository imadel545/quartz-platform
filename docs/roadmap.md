# Roadmap

## Roadmap objective

Deliver Quartz Platform incrementally as a production-grade telecom field operations platform with strong engineering discipline.

The roadmap prioritizes:
- architecture correctness
- build stability
- domain clarity
- MVP delivery
- advanced feature hardening later

## Current checkpoint (validated)

- Android emulator runtime path is operational.
- Android quality gate is green in both modes:
  - base: assemble + unit tests + lint + androidTest compile
  - connected: instrumentation tests on emulator
- Implemented workflow coverage:
  - map/site shell
  - technical site detail
  - guided XFeeder + guided RET shells
  - bounded Débit/QoS local-first foundation (typed site sessions, prerequisites, structured results, local history)
  - bounded Débit/QoS closure projection into local report flows (review in draft + triage signal in list)
  - Débit/QoS report-draft continuity from session context (open existing linked draft or create with `PERFORMANCE` provenance)
  - typed QoS test-family model aligned with Quartz manual and persisted local QoS script shell (create/edit/configure/select)
  - QoS Phase 2 hardening: per-family execution evidence persisted per session + completion guard coherence rules
  - QoS finalization cycle 2 hardening: script snapshot integrity (configured technologies + snapshot timestamp) and family-specific closure constraints (failure reason, phone target, technology alignment)
  - QoS report review hardening: per-family review details in draft and stronger family coverage/failure triage in list
  - QoS finalization cycle 3 hardening: persisted family timeline evidence, shared completion-assessment rule source, and stronger report triage signals (blocked/timeline coverage)
  - QoS finalization cycle 4 hardening: explicit execution-engine state projection, pause/resume timeline semantics, deterministic run-plan coverage, and persisted runner-progress updates for stronger interruption/recovery integrity
  - QoS finalization cycle 5 hardening: transactional timeline checkpoints (no event overwrite), deterministic recovery-state projection, and stronger run continuity visibility in performance/report surfaces
  - QoS finalization cycle 6 hardening: typed failure/block reason taxonomy persisted in family evidence + timeline, actionable operator guidance on execution surfaces, and dominant-issue triage signal in report list
  - QoS finalization cycle 7 hardening: device-aware preflight diagnostics (network/battery/GPS) with explicit operator refresh/apply flow, persisted diagnostics snapshot in `performance_sessions`, and diagnostics projection into ReportDraft/ReportList review surfaces
  - reviewer/supervisor control-tower mobile-first baseline:
    - cross-site local draft aggregation
    - deterministic attention ranking (sync/QoS/staleness)
    - bounded triage filters with saved-state persistence
    - quick actions to draft review and site detail
  - reviewer/supervisor control-tower v2 action-center hardening:
    - grouping modes (`ATTENTION`, `WORKFLOW`) with saved-state restoration
    - top-priority direct open action
    - sync retry actions from control tower (row scope + visible bulk scope)
    - dominant attention and stale-age signals surfaced for faster triage
  - reviewer/supervisor control-tower queue-intelligence acceleration:
    - deterministic queue presets for supervision intents
    - queue continuation/reset semantics for repeated triage loops
    - top motifs by site/workflow with direct open-draft shortcuts
  - reviewer/supervisor queue SLA intelligence:
    - deterministic age buckets + urgency classes for local deadline-risk triage
    - urgency-aware queue ordering and `act-now / overdue` preset
    - row-level urgency context and aggregate urgency motifs for faster supervision decisions
  - reviewer/supervisor queue orchestration v3:
    - persisted queue lifecycle status per draft (`UNTRIAGED`, `IN_REVIEW`, `WAITING_FIELD_FEEDBACK`, `RESOLVED`)
    - direct row-level queue transitions and visible-bulk mark-in-review
    - typed queue action journal for local auditability
    - queue-status filters and status motifs for action-center steering speed
  - mission-driven product/UX recovery on Android runtime surfaces:
    - Home Map reframed as mission entrypoint with explicit primary vs secondary actions
    - Home Map site-targeting controls moved behind explicit disclosure to keep mission context/map actions primary
    - Site Detail split into mission-launch actions and collapsible technical details
    - Site Detail local draft rows moved behind explicit disclosure with persistent draft summary signals
    - XFeeder guided runtime now prioritizes mission status/proximity signals and collapses history/sector context/geospatial advanced controls
    - RET guided runtime refocused on mission status/progress and reduced control density
    - Débit/QoS runtime now uses progressive disclosure for diagnostics/checklist/run-plan/timeline/script-editor to reduce mixed mission/debug overload
    - ReportDraft debug tooling moved behind explicit developer disclosure
    - ReportDraft guided-evidence details moved behind explicit disclosure after compact reviewer summary
    - ReportList now includes a compact mission triage summary card and simplified QoS closure wording for faster scanability
    - Control Tower advanced controls made collapsible for mobile triage readability
    - Control Tower queue-tuning filter controls are now disclosure-based while presets stay immediately available
    - reusable operational design primitives now applied cross-surface to enforce consistent hierarchy/severity/disclosure semantics
  - mission/runtime design-system hardening:
    - reusable mission header primitive with explicit primary/secondary action hierarchy (`MissionHeaderCard`)
    - reusable severity-based runtime alert/info primitive (`OperationalMessageCard`)
    - status signals now rendered as non-interactive badges (with explicit overflow indicator) to remove fake click affordances and improve scanability
  - shared workflow/session core reused by multiple guided flows
  - local report-draft continuity with typed provenance
  - workflow-typed closure projection in report draft flow for XFeeder/RET local review
  - bounded workflow-typed closure triage summary in report list for faster local draft screening
  - XFeeder geospatial session context with bounded altitude-aware proximity eligibility
  - explicit reference-altitude provenance (`TECHNICAL_DEFAULT`, `OPERATOR_OVERRIDE`, `UNAVAILABLE`)

## Next major roadmap items (after current lot)

1. Backend sync/auth foundation aligned with mobile offline contracts.
2. End-to-end report synchronization with conflict handling and observability.
3. Progressive hardening of advanced telecom workflows (RET/QoS/drive) without collapsing modular boundaries.

---

## Phase 0 — framing and repository governance

### Goals
- establish repository structure
- document architecture and product context
- define agent rules and quality constraints
- prepare Codex-specific guidance

### Deliverables
- `README.md`
- `AGENTS.md`
- `docs/product-overview.md`
- `docs/architecture.md`
- `docs/roadmap.md`
- `.codex/skills/*`

### Exit criteria
- repo structure is in place
- engineering rules are explicit
- product scope is documented
- Codex has enough context to operate safely

---

## Phase 1 — platform bootstrap

## 1A. Android bootstrap

### Goals
- initialize Android project
- configure Kotlin, Compose, Hilt, Room, WorkManager
- establish package structure
- create baseline app shell
- validate local build

### Deliverables
- Gradle setup
- Android app module
- application class
- base navigation shell
- design system seed
- basic logging setup
- base test scaffolding

### Exit criteria
- app builds in Android Studio
- unit test baseline runs
- package structure matches architecture intent

## 1B. Backend bootstrap

### Goals
- initialize Spring Boot backend
- configure Java 21
- set up basic modular package structure
- configure PostgreSQL/PostGIS integration
- add Flyway baseline
- expose health endpoint
- validate local run

### Deliverables
- Spring Boot project skeleton
- base configuration
- health endpoint
- Flyway baseline migration
- OpenAPI baseline
- Docker-ready local setup

### Exit criteria
- backend starts locally
- database migration runs successfully
- API documentation baseline is available

## 1C. Local infrastructure bootstrap

### Goals
- prepare Docker Compose for local dependencies
- define local development topology
- prepare environment variable templates

### Deliverables
- compose file
- postgres service
- redis service
- optional object storage service
- environment templates

### Exit criteria
- local services start predictably
- backend can connect locally

---

## Phase 2 — domain foundation

## 2A. Core domain model

### Goals
- define explicit domain model
- define repository interfaces
- define initial use cases

### Priority entities
- User
- Device
- Site
- Sector
- Antenna
- Cell
- TestSession
- Report
- SyncJob

### Exit criteria
- domain model is typed and coherent
- repository contracts exist
- DTO/domain separation is respected

## 2B. API contracts

### Goals
- define first stable API contracts for:
  - auth/session
  - site list/search
  - site detail
  - report list
  - sync intake baseline

### Exit criteria
- controller contracts are documented
- DTOs validated
- OpenAPI reflects initial contract set

---

## Phase 3 — MVP vertical slice

## 3A. Authentication baseline

### Goals
- mobile sign-in/session handling
- protected backend routes
- secure token/session storage

### Exit criteria
- authenticated mobile flow exists
- backend access control is wired
- session lifecycle is testable

## 3B. Site map and site detail MVP

### Goals
- display site list/search
- show map and selected site
- show site technical summary
- support local cache of reference data

### Exit criteria
- technician can browse/search/select a site
- local data access works
- UI architecture remains clean

## 3C. Report capture baseline

### Goals
- create a basic field report
- persist locally
- show sync state
- submit to backend when possible

### Exit criteria
- report flow works offline-first
- synchronization baseline works end-to-end

---

## Phase 4 — test session foundation

## 4A. Test session engine baseline

### Goals
- define generic test session lifecycle
- support start / in progress / completed / failed states
- store session locally
- sync final results

### Exit criteria
- session lifecycle is explicit
- reports can reference sessions
- architecture allows future specialized test types

## 4B. Throughput testing baseline

### Goals
- support throughput run workflow
- capture metadata and result structure
- persist and report results

### Exit criteria
- throughput run is modeled and stored
- flow is observable and resumable

---

## Phase 5 — advanced telecom workflows

### Candidate areas
- QoS scripts and QoS runs
- RET workflows
- advanced guided validations
- capability-aware device execution
- richer reporting and supervision

### Exit criteria
- advanced features remain modular
- device constraints remain isolated
- no architecture collapse under complexity

---

## Phase 6 — hardening and production readiness

### Goals
- improve observability
- add CI quality gates
- expand test coverage
- tune performance
- harden sync and failure handling
- improve administrative supervision

### Deliverables
- CI workflows
- lint/test/build gates
- crash/error monitoring integration
- metrics dashboards
- operational runbooks

### Exit criteria
- project is operationally understandable
- failures are diagnosable
- build and release discipline exists

---

## Implementation discipline

For every roadmap item:
- implement the smallest vertical slice that proves the architecture
- validate before expanding
- document assumptions
- refuse hidden shortcuts in foundation code

---

## Anti-patterns to avoid

- building all modules before first runnable slice
- introducing advanced telecom complexity before baseline stability
- creating a fake “enterprise” structure with no working build
- burying sync logic in UI code
- leaking persistence details into domain
- adding platform-specific hacks in generic modules

---

## Recommended execution order now

1. fill all architecture and Codex guidance files
2. bootstrap Android project
3. bootstrap backend project
4. bootstrap local infra
5. implement first runnable MVP slice
