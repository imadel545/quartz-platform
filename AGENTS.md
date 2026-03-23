# AGENTS.md

## Mission

Build Quartz Platform as a modern, production-grade reconstruction of a telecom field operations system.

Quartz Platform is not a toy app, a prototype, or a CRUD-only project.

It is a field-oriented operational platform combining:
- an Android mobile application for field technicians
- a backend API for synchronization, domain logic, reporting, and administration
- a back-office web console for supervision and management
- an infrastructure and observability layer

The implementation must target high reliability, strong modularity, offline-first operation, and maintainable long-term evolution.

---

## Product reality

This project comes from a legacy context:
- an operational guide describing workflows and product behavior
- an old APK artifact used only for reverse-engineering clues

The new implementation must be treated as a modern rebuild.
It must not be a fragile clone of legacy behavior.
It must preserve business intent while improving architecture, quality, observability, and maintainability.

---

## Operating principles

You are expected to behave like a disciplined senior engineer working under architecture constraints.

You must:
- reason before changing files
- read the relevant documentation in `docs/` before implementation
- work in bounded, verifiable increments
- preserve clear separation of concerns
- avoid hidden assumptions
- avoid speculative rewrites
- prefer explicitness over cleverness
- produce code that can survive production use and team maintenance

You must not:
- hallucinate nonexistent requirements
- invent APIs or flows without documenting assumptions
- bury business logic in UI layers
- create tightly coupled code
- add dependencies casually
- fake completeness with placeholder-heavy architecture
- optimize prematurely at the cost of clarity
- perform giant uncontrolled refactors

---

## Core system goals

The system must be designed for:

- offline-first field execution
- geospatial and site-based workflows
- structured test sessions
- throughput and QoS measurements
- synchronization under unreliable connectivity
- device capability awareness
- robust reporting and auditing
- modular evolution of advanced telecom features

---

## Repository layout

- `docs/`: product, architecture, domain, roadmap, acceptance criteria
- `mobile-android/`: Android application in Kotlin
- `backend-api/`: backend API in Spring Boot / Java 21
- `backoffice-web/`: administrative web application
- `infra/`: docker, deployment, observability, runtime topology
- `.codex/`: project-specific Codex configuration and skills
- `scripts/`: local automation and helper scripts

---

## Mandatory engineering behavior

For every task:

1. read the relevant files in `docs/`
2. identify the exact scope of the requested change
3. define the smallest coherent implementation unit
4. implement only what is needed for that unit
5. run the relevant validations when possible
6. summarize:
   - objective
   - files touched
   - design decisions
   - commands run
   - assumptions
   - remaining limitations
   - next recommended step

Do not skip the summary.

---

## Quality bar

Every change must aim for:

- explicit structure
- low coupling
- strong cohesion
- readable naming
- traceable logic
- testability
- production-safe defaults
- maintainable boundaries
- deterministic build behavior

Avoid:
- god objects
- giant utility classes
- fragile inheritance hierarchies
- hidden state mutations
- side effects in presentation code
- duplicated domain logic across layers
- transport models leaking into domain

---

## Architecture rules: global

- Keep domain concepts explicit and typed
- Prefer composable modules over monolithic classes
- Use dependency injection consistently
- Keep business rules in domain/application layers
- Keep infrastructure concerns isolated
- Avoid magic constants
- Centralize configuration
- No secrets in source code
- All database schema changes require migrations
- All external contracts must be documented

---

## Architecture rules: Android

### Target stack
- Kotlin
- Jetpack Compose
- MVVM
- Clean Architecture
- Coroutines / Flow
- Hilt
- Room
- WorkManager
- Retrofit / OkHttp
- DataStore

### Layering expectations
Use explicit boundaries:

- `presentation`
- `domain`
- `data`
- `device`
- `core`

### Non-negotiable Android rules
- Kotlin is the primary language
- Compose is the default UI system
- No business logic in composables
- No networking inside ViewModels
- No direct persistence logic inside composables
- ViewModels orchestrate state, not infrastructure details
- Repositories are abstractions owned by domain
- Use cases encapsulate business actions
- Offline-first must be considered from the beginning
- Device-specific constraints must live in dedicated capability/integration modules
- Long-running field operations must use appropriate Android execution models

### Android code expectations
- state-driven UI
- immutable UI state models where possible
- lifecycle-safe asynchronous operations
- explicit permission handling
- graceful degradation for unsupported device features
- logs useful for diagnosis, not noise

---

## Architecture rules: backend

### Target stack
- Java 21
- Spring Boot 3
- Spring Security
- PostgreSQL
- PostGIS
- Flyway
- Redis
- OpenAPI
- Testcontainers
- Micrometer / Prometheus

### Layering expectations
Use explicit backend boundaries:

- `controller`
- `application`
- `domain`
- `infrastructure`

### Non-negotiable backend rules
- DTOs must not leak into domain
- Controllers must stay thin
- Business orchestration belongs in application services/use cases
- Persistence logic belongs in infrastructure/repository adapters
- Schema changes require Flyway migrations
- Public APIs must be documented
- Auditability and observability are first-class concerns
- Geospatial concerns must be modeled intentionally
- Avoid anemic domain models when domain invariants matter

---

## Architecture rules: web

### Target stack
- Next.js
- TypeScript
- Tailwind CSS
- schema validation
- typed API contracts

### Rules
- TypeScript only
- no untyped API payload handling
- keep API layer separate from UI components
- keep validation explicit
- do not mix admin logic everywhere in component trees

---

## Architecture rules: infrastructure

- Local development must be reproducible
- Docker Compose is the local baseline
- Secrets must come from environment/config, never source
- Observability must be designed early
- Build pipelines must validate quality gates
- Runtime topology must remain understandable

---

## Product-specific engineering guidance

Quartz Platform must explicitly model:

- sites
- sectors
- antennas
- cells
- test sessions
- throughput runs
- QoS scripts and runs
- reports
- synchronization state
- device capability state
- user/operator context where relevant

Do not collapse these into vague generic objects unless there is a strong reason.

---

## Device capability realism

Do not assume all Android devices behave the same.

The system must be able to reason about:
- Android version
- device manufacturer
- model family
- permissions granted
- network/radio support level
- feature support availability
- degraded mode behavior

Unsupported features must fail gracefully and transparently.

---

## Offline-first requirements

The mobile app must be built with offline constraints in mind:
- local persistence
- queued synchronization
- resumable workflows
- conflict-aware sync design
- visibility of sync state
- tolerance to poor connectivity and battery constraints

Do not build a network-dependent architecture and promise offline support later.

---

## Validation expectations

Whenever possible, validate using:
- compilation/build checks
- unit tests
- integration tests
- lint/static analysis
- deterministic local commands

Never claim success without checking what can reasonably be checked.

---

## Delivery format for task completion

For each completed task, return:

1. Objective
2. Files changed
3. Implementation summary
4. Validation run
5. Assumptions and constraints
6. Known gaps
7. Next recommended step

---

## Priority order for project execution

1. documentation and architecture framing
2. repository and build bootstrap
3. domain model and contracts
4. mobile/backend MVP
5. synchronization and reporting
6. advanced telecom/device features
7. hardening, observability, performance tuning

---

## What must not happen

- Do not try to build the full platform in one pass
- Do not generate fake enterprise architecture without executable substance
- Do not over-engineer abstractions before concrete usage appears
- Do not hide uncertainty
- Do not produce demo-quality shortcuts in core layers
- Do not use “temporary” hacks in foundational modules
- Do not state that something works unless it has been validated