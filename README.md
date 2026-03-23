# Quartz Platform

Quartz Platform is a modern reconstruction of a field operations mobile platform for telecom site validation, radio measurement workflows, QoS testing, reporting, and back-office supervision.

This repository is organized as a monorepo and is designed for a production-grade, scalable, offline-first architecture.

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