# Product Overview

## Product name

Quartz Platform

## Purpose

Quartz Platform is a field operations platform designed to assist telecom technicians and operational teams during on-site validation, measurement, testing, and reporting activities.

The platform combines:
- a mobile Android application used in the field
- a backend API for synchronization, domain logic and reporting
- a back-office web interface for administration and supervision

## Context

The project is based on:
- an operational guide describing workflows and major features
- a legacy APK artifact that provides technical clues but not a sufficient base for modern implementation

The new system is a modern rebuild, not a direct legacy continuation.

## Primary users

### Field technician
Uses the Android app to:
- find and inspect telecom sites
- visualize location and nearby context
- run measurement and validation workflows
- execute throughput and QoS scenarios
- capture and synchronize reports

### Supervisor / operations manager
Uses the back office to:
- supervise reports and execution quality
- manage accounts and device fleet
- inspect configuration and supported options
- distribute operational documentation and versions

### Administrator
Manages:
- access control
- site data
- sector/cell reference data
- scripts and validation options
- deployment and platform health

## Functional domains

### 1. Site discovery and map workflow
The system must support:
- site search by identifier or name
- map visualization
- current user position
- site selection
- contextual display of related information

### 2. Site technical inspection
The platform should expose technical information related to:
- sectors
- antennas
- cells
- installed vs forecast configuration
- related attributes required by field operations

### 3. Test session execution
The mobile application must support structured field sessions with:
- clear entry point
- preconditions
- execution states
- result capture
- resumable workflows
- final reporting

Current validated guided-session coverage includes XFeeder and RET shells with local persistence/resume, completion guards, and report-draft continuity. XFeeder proximity handling is altitude-aware with explicit local reference-altitude provenance (technical default vs operator override vs unavailable), while remaining intentionally bounded (no full telecom automation claims).
Current validated performance coverage also includes a bounded local-first Débit/QoS foundation (site-level guided sessions, prerequisites, structured result capture, and local session history) without claiming final telecom automation.
The QoS shell now includes typed test families aligned with the manual (`THROUGHPUT_LATENCY`, `VIDEO_STREAMING`, `SMS`, `VOLTE_CALL`, `CSFB_CALL`, `EMERGENCY_CALL`, `STANDARD_CALL`) and a local script definition layer (create/edit/configure/select) persisted in Room.
QoS session execution now captures bounded per-family evidence with explicit status (`NOT_RUN`, `PASSED`, `FAILED`, `BLOCKED`) and keeps aggregate execution counters coherent with that family-level evidence at completion time.
QoS session closure now enforces bounded family-specific execution rules for reviewer trust:
- failed family requires explicit failure reason,
- selected call/SMS families require target phone number,
- target technology must align with configured script technologies when present.
QoS script launch context is persisted as a local snapshot (configured technologies + snapshot timestamp) for deterministic review of what was actually executed.
QoS execution now keeps a bounded local timeline per selected family (`STARTED`, `PAUSED`, `RESUMED`, `PASSED`, `FAILED`, `BLOCKED`) so operator/supervisor review can inspect execution progression without exposing raw internal session state.
Timeline persistence is transactional (immutable event identity + ordered checkpoints), which keeps interruption/recovery evidence trustworthy when the same event type happens multiple times.
QoS failed/blocked execution now carries typed issue taxonomy codes (bounded to what the app really knows) across runtime state, timeline, and family evidence so operators and supervisors get consistent triage semantics.
QoS runtime now captures an explicit device diagnostics snapshot (network availability, battery level, GPS availability, capture timestamp) and allows operators to apply that snapshot to prerequisite flags before execution/closure.
QoS execution engine behavior is now explicit and deterministic for runtime/operator clarity:
- engine state projection (`READY`, `PREFLIGHT_BLOCKED`, `RUNNING`, `PAUSED`, `RESUMED`, `COMPLETED`, `FAILED`, `BLOCKED`),
- active family/repetition visibility,
- run-plan coverage visibility (planned vs pending vs terminal),
- bounded recovery projection (`NONE`, `RESUME_AVAILABLE`, `INVARIANT_BROKEN`) with next-run/checkpoint visibility for restart clarity,
- local runner-progress persistence during execution updates to improve resume integrity after interruption.
Current validated local reporting now includes bounded Débit/QoS closure review projection:
- ReportDraft shows typed performance closure signals (workflow type, execution state, required-step progress, prerequisites, and implemented metrics/results).
- ReportDraft includes observed device diagnostics snapshot when available for throughput/QoS reviewer context.
- ReportDraft includes concise per-family QoS execution review for supervisor/operator readability.
- ReportDraft includes a bounded QoS timeline section for closure auditability.
- ReportList exposes a concise performance triage summary for non-guided drafts, including family coverage/failure plus blocked/timeline, dominant issue code, engine/run-plan signals, and compact device diagnostics signals for QoS sessions.
- Performance sessions can now open-or-create a linked local report draft with explicit `PERFORMANCE` provenance for continuity and supervisor review.
Current validated supervisor visibility now also includes a mobile-first Reviewer Control Tower:
- cross-site draft aggregation for faster supervisor triage without opening each site manually,
- deterministic attention prioritization based on sync failures, QoS risk, and staleness,
- bounded filters (`ALL`, `NEEDS_ATTENTION`, `SYNC_FAILED`, `QOS_RISK`, `GUIDED`, `NON_GUIDED`),
- row actions to open the draft review directly or jump to site detail,
- action-center controls for faster execution from the triage surface:
  - open top-priority visible draft,
  - retry failed sync at row-level,
  - retry all visible failed sync rows in one action,
  - grouping by attention or workflow with restored grouping state after process death,
- compact triage context showing dominant attention signal and stale draft age on each visible row.
Control Tower queue intelligence is now strengthened with bounded mobile-first supervisor acceleration:
- queue presets for common supervision intents (attention-now, sync-failures-first, qos-risk-first, stale-guided, guided-unresolved),
- deterministic queue continuation (`open next`) with explicit local progression reset,
- aggregated motifs (top site/workflow patterns) to identify recurring queue pressure quickly,
- direct open-draft shortcuts from motif cards to reduce identify→open latency.
Queue SLA intelligence is now added to improve urgency-based supervision:
- bounded age buckets (`FRESH`, `AGING`, `STALE`, `OVERDUE`) derived from local draft timestamps,
- bounded urgency classes (`ACT_NOW`, `HIGH`, `WATCH`, `NORMAL`) with explicit urgency reasons,
- urgency-aware queue ordering and an `act-now / overdue` preset for faster time-risk triage,
- per-row urgency context and urgency motifs for compact supervisor readability.
Supervisor Queue Orchestration v3 now adds bounded queue lifecycle control for reviewer/supervisor execution:
- persisted queue status per draft (`UNTRIAGED`, `IN_REVIEW`, `WAITING_FIELD_FEEDBACK`, `RESOLVED`),
- row-level queue actions (mark in review / waiting feedback / resolved / reopen),
- visible-bulk transition shortcut (mark visible untriaged rows as in-review),
- queue-status filter and status motifs for faster queue steering without leaving mobile control-tower flow.
Product/UX recovery now also reinforces mission-driven runtime execution clarity:
- Home Map acts as mission entrypoint (primary actions first, secondary/demo actions clearly demoted),
- Home Map keeps site-targeting controls in progressive disclosure so map mission context stays primary,
- Site Detail separates mission actions from technical structure with progressive disclosure,
- Site Detail keeps local draft counts visible while moving full draft rows behind explicit disclosure,
- XFeeder guided runtime now prioritizes mission header/proximity status and defers history + sector/advanced geospatial details behind disclosures,
- RET guided execution exposes mission status/progress first and collapses full history by default,
- Débit/QoS runtime now separates mission context from diagnostics/checklist/run-plan/timeline/script-editor using bounded disclosure controls,
- ReportDraft keeps reviewer flow primary while debug sync tools require explicit developer disclosure,
- ReportDraft guided evidence now starts with compact reviewer signals and keeps detailed projections behind disclosure,
- ReportList adds a mission triage summary card with compact QoS closure wording for faster supervisor scan,
- Control Tower keeps high-value triage visible while advanced controls are intentionally collapsible.
- shared operational presentation patterns (mission cards, signal chips, disclosure controls) are now reused across these surfaces to keep action hierarchy and severity language consistent in runtime.
- mission/runtime design-system hardening now standardizes:
  - mission entry headers with explicit primary vs secondary action hierarchy (`MissionHeaderCard`),
  - runtime alert/info rendering through shared severity cards (`OperationalMessageCard`),
  - non-interactive severity badges with explicit overflow signal (`+N`) for clearer scanability and safer touch semantics.
- Mission Surface Consolidation v1 finalized the critical runtime surfaces:
  - QoS/throughput execution now behaves as a strict mission console (preflight first, then active run, then outcome capture; advanced controls collapsed),
  - ReportDraft now prioritizes reviewer flow and keeps technical/editor/debug tooling secondary behind explicit disclosure,
  - Control Tower now behaves as a supervision queue surface first (SLA/urgency + actions above fold), with tuning and advanced controls disclosure-based.
- Mission Surface Consolidation v2 now extends that recovery to the next weak runtime surfaces:
  - ReportList behaves as an operational review queue instead of a flat local draft list, with mission summary metrics, top-priority triage, dominant issue labeling, and next-action guidance,
  - XFeeder guided runtime now separates mission progress, terrain context, and outcome capture from checklist/history/advanced context so the operator path is readable in seconds,
  - shared UI patterns now include compact metric rows and stronger empty-state cards, allowing denser but clearer mission/review summaries without reverting to text-heavy cards.
- Premium Runtime Finalization v2 (current cycle) strengthens the same product direction:
  - ReportList now exposes explicit queue-state semantics (`État file de revue`) and filter/visible-count controls while moving queue rendering logic into dedicated section composables for maintainability.
  - XFeeder now shows runtime-state guidance even when no session exists and uses selected-state chips for status/outcome/checklist updates, reducing action ambiguity for technicians.
  - XFeeder and ReportList monolith pressure is reduced by extracting section composables (`XfeederGuidedSessionSections.kt`, `ReportListQueueSections.kt`).
  - Shared operational cards/signals now enforce stronger visual hierarchy (card rhythm, signal readability, compact severity language) for cross-surface premium consistency.
- Premium Runtime Finalization v3 (current cycle) raises mission-entry and RET execution clarity:
  - Home Map now surfaces explicit mission runtime state (site selection + location readiness), mission metrics, and contextual primary action above the fold.
  - Site Detail now separates mission launch, guided workflows, local drafts, and technical structure with progressive disclosure and extracted section composables (`SiteDetailSections.kt`).
  - RET guided runtime now follows mission-first ordering (header/state/summary/actions first) with checklist, execution controls, review capture, history, and geospatial advanced context disclosure-based via `RetGuidedSessionSections.kt`.
  - Runtime proof captures for these surfaces are stored in `artifacts/runtime-proof/mission-surface-consolidation-v3/`.
- QoS mission-console recovery now applies the same product rule to Débit/QoS:
  - the performance screen is structured around mission summary, preflight, execution, outcome capture, and advanced support,
  - advanced script/timeline/history tooling no longer competes with the active operator path above the fold,
  - QoS/throughput runtime is clearer for field execution while preserving the existing bounded local-first workflow truth.

### 4. Throughput and QoS testing
The system should support:
- latency checks
- download/upload tests
- scenario-based QoS execution
- result storage
- operator/device traceability

### 5. RET and advanced validation flows
The product roadmap includes guided validation flows such as:
- RET workflows
- step reset/retry support
- contextual history
- operator/band specific validation

### 6. Reporting
The platform must provide:
- report generation
- synchronization status
- technician visibility
- supervisor access through web back office
- workflow-typed local closure review in mobile drafts before backend sync (bounded XFeeder/RET projection)

### 7. Device and capability awareness
The system must account for:
- Android version differences
- manufacturer-specific constraints
- radio/network capability limitations
- feature degradation when unsupported

## Product requirements philosophy

Quartz Platform must be:

- offline-first
- field-resilient
- geospatially aware
- modular
- observable
- secure
- scalable
- maintainable

## Strategic implementation direction

The recommended implementation stack is:

### Mobile
- Kotlin
- Jetpack Compose
- MVVM + Clean Architecture
- Room
- WorkManager
- Coroutines / Flow
- Hilt

### Backend
- Java 21
- Spring Boot 3
- PostgreSQL + PostGIS
- Flyway
- Redis
- OpenAPI

### Web
- Next.js
- TypeScript

## Delivery strategy

The platform should be rebuilt incrementally:

### Phase 1
- architecture and repository bootstrap
- documentation and domain framing

### Phase 2
- Android and backend foundations
- local dev environment
- CI baseline

### Phase 3
- MVP map/site/reporting workflow

### Phase 4
- advanced test orchestration
- QoS scenarios
- device capability matrix
- reporting hardening

## Quality expectations

This project must be executed at a high engineering standard with:

- explicit architecture
- scalable code structure
- strong validation discipline
- production-oriented design
- minimal technical debt creation
