# Quartz Platform

Quartz Platform is a modern reconstruction of a telecom field-operations platform for site validation, guided measurement workflows, local-first reporting, and operational supervision.

## Repository structure

```text
quartz-platform/
  docs/              # Product, architecture, domain and roadmap documentation
  mobile-android/    # Android mobile application (Kotlin, Compose)
  backend-api/       # Backend API (Spring Boot, Java 21)
  backoffice-web/    # Administrative web platform
  infra/             # Infrastructure, Docker, deployment, observability
  scripts/           # Build, bootstrap and local utility scripts
  .codex/            # Codex project configuration and local skills
```

## Current validated state (Android)

- Emulator runtime path works.
- Full Android quality gate is validated:
  - `./scripts/android-quality-gate.sh`
  - `./scripts/android-quality-gate.sh --with-connected`
- Implemented mobile scope:
  - Home map + site selection shell
  - Site detail enriched with sectors / antennas / cells
  - XFeeder guided workflow shell with geospatial session surface
  - RET guided workflow shell reusing shared workflow/session core
  - Débit/QoS local-first foundation with guided site sessions, prerequisites, structured results, and local history
  - QoS script shell persisted locally (typed test families, repeat/technology configuration, local script selection)
  - QoS script execution now stores typed per-family evidence (`NOT_RUN`, `PASSED`, `FAILED`, `BLOCKED`) with bounded failure reason/observed metrics
  - QoS completion guard is hardened for `QOS_SCRIPT`: selected families must have completed evidence and coherent aggregate counters before completion
  - QoS script launch snapshot integrity is local-first: configured technologies and script snapshot timestamp are persisted in session closure data
  - QoS family-specific closure checks require phone target for call/SMS families, failure reason on failed families, and target-technology alignment with script configuration
  - QoS execution timeline is now persisted locally per selected family (`STARTED`, `PASSED`, `FAILED`, `BLOCKED`) for deterministic closure auditability
  - QoS timeline persistence is now transactional: each timeline event has its own durable identity with sequential checkpoint ordering, so repeated event types (`PAUSED`/`RESUMED`/etc.) are preserved instead of overwritten
  - QoS execution engine now exposes explicit runtime states (`READY`, `PREFLIGHT_BLOCKED`, `RUNNING`, `PAUSED`, `RESUMED`, `COMPLETED`, `FAILED`, `BLOCKED`) with deterministic transition-driven projection
  - QoS execution projection now includes bounded recovery signals (`NONE`, `RESUME_AVAILABLE`, `INVARIANT_BROKEN`) plus next planned run and checkpoint count for interruption/restart transparency
  - QoS runner now tracks a structured local run plan per family/repetition and persists runner progress updates during execution to strengthen interruption/recovery behavior
  - QoS completion assessment is centralized in shared domain logic (single rule source reused by repository + ViewModel)
  - QoS failure/block handling now uses typed issue codes (`PREREQUISITE_NOT_READY`, `TARGET_TECHNOLOGY_MISMATCH`, `PHONE_TARGET_MISSING`, `NETWORK_UNAVAILABLE`, `THRESHOLD_NOT_MET`, `OPERATOR_ABORTED`, `UNKNOWN`) persisted in family evidence and timeline events
  - QoS preflight is device-aware: observed network/battery/GPS diagnostics can be refreshed and applied to prerequisite flags during execution
  - QoS session persistence now stores observed device diagnostics snapshot (`network`, `battery%`, `gps availability`, `capturedAt`) with explicit Room migration `22 -> 23`
  - QoS execution screen now exposes actionable operator guidance from typed issue codes for failed/blocked families
  - Débit/QoS closure projection in local reports:
    - ReportDraft reviewer panel now includes typed performance closure data (throughput and QoS script sessions)
    - ReportDraft now includes observed device diagnostics snapshot when available for throughput/QoS review
    - ReportDraft now includes a bounded QoS execution timeline review block
    - ReportDraft now includes typed QoS issue-code visibility on family outcomes and timeline terminal events
    - ReportList triage exposes concise performance health for non-guided drafts, including family coverage/failure signal, blocked/timeline coverage signal, dominant QoS issue code, and compact device diagnostics signal
  - Local report draft continuity with typed workflow provenance (`XFEEDER`, `RET`)
  - Workflow-typed local closure projection in report draft view (XFeeder + RET review fields)
  - Workflow-typed closure triage summary in report list rows (concise operator/supervisor signal)
  - Reviewer/Supervisor Control Tower mobile-first:
    - cross-site draft aggregation (all sites) with deterministic attention ranking
    - bounded attention taxonomy (`SYNC_FAILED`, `SYNC_PENDING`, `QOS_FAILED_OR_BLOCKED`, `QOS_PREREQUISITES_NOT_READY`, `STALE_DRAFT`)
    - persisted triage filter state in ViewModel saved state (`ALL`, `NEEDS_ATTENTION`, `SYNC_FAILED`, `QOS_RISK`, `GUIDED`, `NON_GUIDED`)
    - quick actions from control-tower row to open the linked draft or site detail
    - action-center v2 controls:
      - open top-priority visible draft directly
      - retry failed syncs from the control tower (row-level and visible bulk scope)
      - grouping modes (`ATTENTION`, `WORKFLOW`) with saved-state restoration
      - dominant attention signal + stale-age visibility for faster supervisor triage
    - queue-intelligence supervision acceleration:
      - queue presets (`NEEDS_ATTENTION_NOW`, `SYNC_FAILURES_FIRST`, `QOS_RISK_FIRST`, `STALE_GUIDED_WORK`, `GUIDED_UNRESOLVED`)
      - deterministic queue progression (`queueTopDraftId`, progressed items, reset action)
      - aggregated motifs (top site/workflow queue patterns with direct draft open shortcuts)
    - queue SLA intelligence (bounded/local-first):
      - explicit draft aging buckets (`FRESH`, `AGING`, `STALE`, `OVERDUE`)
      - explicit urgency taxonomy (`ACT_NOW`, `HIGH`, `WATCH`, `NORMAL`) with typed urgency reasons
      - urgency-aware ranking and preset (`ACT_NOW_OVERDUE`) for faster supervisor triage
      - urgency/aging visibility on each queue row plus aggregate urgency motifs
    - supervisor queue orchestration v3 (bounded/local-first):
      - persisted supervisor queue lifecycle per draft (`UNTRIAGED`, `IN_REVIEW`, `WAITING_FIELD_FEEDBACK`, `RESOLVED`)
      - per-draft action log with typed actions (single + bulk transitions, retry traceability)
      - queue-status filters and status motifs for faster triage/readability
      - direct row actions (mark in review / waiting feedback / resolved / reopen) and bulk mark-in-review on visible queue
    - product/UX architecture recovery (mission-driven runtime surfaces):
      - Home Map reframed as mission entrypoint (`Mission terrain`) with primary actions (control tower + recenter) and secondary/demo actions demoted
      - Site Detail split between mission actions and technical depth, with guided sector launch cards and collapsible technical details
      - RET guided session refocused on mission status/progress, with history progressive disclosure and reduced action density (status/outcome/step chips)
      - ReportDraft debug sync tools now hidden behind explicit developer disclosure toggle
      - Control Tower advanced controls (queue-status filter/grouping/motifs) behind explicit disclosure to reduce runtime cognitive load
  - Room migrations and schema snapshots kept explicit

## Explicitly out of scope at this stage

- Backend sync transport and auth/session integration
- Full drive mode / GPS trace automation
- RET telecom automation engines
- Telecom-grade QoS execution automation and modem/OEM orchestration

Use `mobile-android/README.md` for Android setup/run details and operational quality-gate commands.
