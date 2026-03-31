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
  - QoS completion assessment is centralized in shared domain logic (single rule source reused by repository + ViewModel)
  - Débit/QoS closure projection in local reports:
    - ReportDraft reviewer panel now includes typed performance closure data (throughput and QoS script sessions)
    - ReportDraft now includes a bounded QoS execution timeline review block
    - ReportList triage exposes concise performance health for non-guided drafts, including family coverage/failure signal plus blocked/timeline coverage signal for QoS sessions
  - Local report draft continuity with typed workflow provenance (`XFEEDER`, `RET`)
  - Workflow-typed local closure projection in report draft view (XFeeder + RET review fields)
  - Workflow-typed closure triage summary in report list rows (concise operator/supervisor signal)
  - Room migrations and schema snapshots kept explicit

## Explicitly out of scope at this stage

- Backend sync transport and auth/session integration
- Full drive mode / GPS trace automation
- RET telecom automation engines
- QoS execution engine and scripts runtime

Use `mobile-android/README.md` for Android setup/run details and operational quality-gate commands.
