# Acceptance Criteria

## Objective

Define the minimum quality and validation thresholds for Quartz Platform deliverables.

These criteria are intended to prevent fragile implementation and to guide bounded, verifiable delivery.

---

## Global acceptance criteria

A task is not considered complete unless:

- scope is respected
- changed files are coherent with the requested objective
- architecture boundaries are preserved
- assumptions are made explicit
- validation results are reported honestly
- known gaps are listed clearly
- no hidden shortcuts are introduced in foundational areas

---

## Documentation acceptance criteria

Documentation work is accepted if:
- files are internally consistent
- terminology is explicit
- product and architecture intent are aligned
- no major contradiction exists across docs
- guidance is actionable for implementation

---

## Android bootstrap acceptance criteria

The Android bootstrap is accepted if:
- project opens in Android Studio without structural breakage
- Gradle configuration is coherent
- Kotlin is the primary language
- Jetpack Compose is the default UI foundation
- application class exists
- main activity exists
- app shell is runnable
- package structure reflects documented architecture
- DI baseline is present
- local persistence scaffold is present
- sync scaffold is present
- test baseline exists
- no business logic is embedded in UI layers

---

## Backend bootstrap acceptance criteria

The backend bootstrap is accepted if:
- project structure is coherent and modular
- Java 21 and Spring Boot 3 are used
- app starts locally
- health endpoint is exposed
- Flyway baseline is wired
- database configuration is environment-driven
- OpenAPI baseline exists
- architecture boundaries are visible in code layout

---

## Quality acceptance criteria

Implementation quality is accepted if:
- naming is explicit
- classes are not overloaded with unrelated responsibilities
- domain logic is not leaked into UI/controllers
- code is testable
- low-value placeholder structure is avoided
- dependencies are justified
- no claim of validation is made without actual execution

---

## Reporting format acceptance criteria

Every significant task completion must include:
1. Objective
2. Files changed
3. Implementation summary
4. Validation run
5. Assumptions and constraints
6. Known gaps
7. Next recommended step