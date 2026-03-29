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
  - Local report draft continuity with typed workflow provenance (`XFEEDER`, `RET`)
  - Room migrations and schema snapshots kept explicit

## Explicitly out of scope at this stage

- Backend sync transport and auth/session integration
- Full drive mode / GPS trace automation
- RET telecom automation engines
- QoS execution engine and scripts runtime

Use `mobile-android/README.md` for Android setup/run details and operational quality-gate commands.
