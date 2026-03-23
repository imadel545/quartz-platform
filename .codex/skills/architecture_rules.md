# Architecture Rules Skill

## Objective

Keep Quartz Platform aligned with explicit architecture boundaries.

## Rules
- read repository guidance before foundational work
- implement the smallest coherent slice
- preserve separation between presentation, domain, data, device, and infrastructure
- do not invent missing business rules silently
- keep domain concepts explicit
- keep summaries honest and complete

## Android-specific
- Kotlin first
- Compose first
- no business logic in UI
- no networking in ViewModels
- repositories defined at domain boundary
- device-specific constraints isolated from generic feature logic

## Backend-specific
- thin controllers
- application layer orchestrates workflows
- domain independent from DTOs
- schema changes require Flyway
- API contracts must stay explicit