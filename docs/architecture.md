# Architecture

## 1. Objective

Quartz Platform must be rebuilt as a modular, production-grade, offline-first telecom field operations system.

The platform includes:
- an Android mobile application for field technicians
- a backend API for synchronization, reporting, and domain workflows
- a back-office web platform for supervision and administration
- an infrastructure layer for local development, deployment, observability, and runtime operations

This is not a basic CRUD application.
It is a field-oriented operational platform with geospatial workflows, telecom-specific constraints, device-specific capabilities, offline execution requirements, and reporting responsibilities.

---

## 2. Product-driven architecture context

The product scope comes from a Quartz operational guide describing:
- main map-based site search and site selection
- detailed site inspection by sector, antenna, and cell
- XFeeder / MixFeeder workflows
- throughput tests
- mode proximity
- mode drive
- RET validation flows
- QOS and QOS scripts
- report browsing
- device-specific locking band workflows
- web back office

The new implementation must preserve the business intent while modernizing architecture, quality, maintainability, observability, and scalability.

## 2.1 Current Android workflow coverage snapshot (validated)

Current Android implementation covers a bounded local-first workflow baseline:
- home map + site selection
- site detail with sectors / antennas / cells
- guided XFeeder workflow with persisted geospatial session context
- guided RET workflow reusing shared workflow/session core
- guided Débit/QoS site-level workflow foundation reusing the same session core principles
- local report draft continuity with workflow-typed provenance
- local report closure projection that stays workflow-typed (`XFEEDER`, `RET`) for reviewer-readable draft inspection
- bounded local performance closure projection (`THROUGHPUT`, `QOS_SCRIPT`) into report surfaces:
  - detailed reviewer view in ReportDraft
  - concise triage signal in ReportList for non-guided drafts
- bounded local QoS script layer persisted in Room (`qos_scripts`) with:
  - typed test-family selection
  - repeat count and technology configuration
  - deterministic projection of configured QoS context into report closures
- bounded QoS family execution evidence persisted per performance session (`performance_qos_family_results`) with explicit status:
  - `NOT_RUN`, `PASSED`, `FAILED`, `BLOCKED`
  - typed failure reason taxonomy (`failureReasonCode`) for deterministic operator/reviewer triage
- bounded QoS timeline evidence persisted per performance session (`performance_qos_timeline_events`) with typed events:
  - `STARTED`, `PAUSED`, `RESUMED`, `PASSED`, `FAILED`, `BLOCKED`
  - typed terminal reason taxonomy (`reasonCode`) aligned with family evidence
  - transactional event persistence with immutable event identity and sequential checkpoint ordering for deterministic replay/recovery
- bounded QoS execution engine projection derived from session evidence:
  - explicit engine state (`READY`, `PREFLIGHT_BLOCKED`, `RUNNING`, `PAUSED`, `RESUMED`, `COMPLETED`, `FAILED`, `BLOCKED`)
  - active family/repetition visibility
  - deterministic run-plan coverage (planned, pending, terminal)
  - bounded recovery state projection (`NONE`, `RESUME_AVAILABLE`, `INVARIANT_BROKEN`) with next planned run and checkpoint count
- QoS execution now keeps a bounded observed-device diagnostics snapshot on `performance_sessions`:
  - network status (`AVAILABLE`/`UNAVAILABLE`)
  - battery level percentage
  - GPS availability
  - diagnostics capture timestamp
  - persisted via Room migration `22 -> 23`
- QoS preflight can apply observed diagnostics snapshot to session prerequisite flags for deterministic operator flow
- `QOS_SCRIPT` completion hardening in repository/domain flow:
  - selected families required
  - each selected family must be completed (`PASSED`/`FAILED`)
  - failed families require explicit failure reason
  - call/SMS families require target phone number
  - target technology must align with script configured technologies when configuration exists
  - aggregate counters must stay coherent with family evidence
- session closure keeps an immutable QoS script snapshot context for review:
  - configured script technologies
  - script snapshot timestamp (`updatedAtEpochMillis` at selection/save time)
- report-side performance projection is strengthened but still bounded:
  - ReportDraft includes per-family QoS closure details and timeline review
  - ReportDraft includes observed-device diagnostics snapshot when available
  - ReportDraft includes typed QoS issue taxonomy visibility (code + optional detail) for failed/blocked outcomes
  - ReportDraft includes QoS execution-engine context (state, active run, plan progress)
  - ReportList includes concise QoS triage for failed/blocked/coverage signals plus engine-state/run-plan signal, dominant issue code, and compact device diagnostics signals
- reviewer/supervisor control-tower projection is now available as a mobile-first cross-site triage surface:
  - deterministic aggregation over all local drafts (not site-scoped only)
  - bounded attention-signal taxonomy (`SYNC_FAILED`, `SYNC_PENDING`, `QOS_FAILED_OR_BLOCKED`, `QOS_PREREQUISITES_NOT_READY`, `STALE_DRAFT`)
  - deterministic rank ordering and bounded saved-state filter/grouping for triage continuity
  - action-center shortcuts:
    - open top-priority visible draft
    - retry failed sync from row scope
    - retry failed sync in current visible filtered scope
  - row-level dominant attention signal + stale-age context for quicker supervisor decisions
  - queue-intelligence layer (still bounded/local-first):
    - deterministic queue presets over filtered drafts
    - explicit queue progression state (`progressedDraftIds`) with reset
    - queue-top action semantics for faster repeated supervisor review loops
    - lightweight aggregated motifs by site/workflow for pattern-oriented triage
  - queue SLA intelligence layer (bounded/local-first):
    - deterministic age-bucket derivation (`FRESH`, `AGING`, `STALE`, `OVERDUE`)
    - deterministic urgency classification (`ACT_NOW`, `HIGH`, `WATCH`, `NORMAL`) with typed urgency reason
    - urgency rank integrated into queue ordering (explainable, no opaque ML scoring)
    - urgency motifs and row-level urgency exposure for reviewer/supervisor action speed

For XFeeder proximity, implementation is intentionally explicit and bounded:
- `UNAVAILABLE`: location/altitude data cannot be trusted for eligibility
- `SUPPORTED`: proximity evaluation is available but no effective reference altitude is currently available
- `INELIGIBLE`: distance and/or altitude constraints are not satisfied
- `ELIGIBLE`: distance and altitude constraints are satisfied

Reference altitude provenance is explicit and local-first:
- `TECHNICAL_DEFAULT`: read-only local technical source from sector/antenna data
- `OPERATOR_OVERRIDE`: explicit operator-entered override
- `UNAVAILABLE`: no technical source and no override

This keeps product feedback honest without claiming full telecom-grade altitude precision or drive automation.

---

## 3. High-level system architecture

```text
Field Technician
    |
    v
Android Mobile App
    |
    | HTTPS / Authenticated API / Sync
    v
Backend API
    |
    +--> PostgreSQL / PostGIS
    +--> Redis
    +--> Object Storage
    +--> Metrics / Logs / Traces
    |
    v
Back-office Web App
