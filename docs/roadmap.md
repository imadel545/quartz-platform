---

# 3) `docs/roadmap.md`

```md
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
