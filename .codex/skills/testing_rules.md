# Testing Rules Skill

Use this skill for all implementation tasks.

## Objective
Maintain a serious validation discipline from the beginning.

## General rules
- do not claim code works without validation
- run the smallest relevant checks available
- report exactly what was validated and what could not be validated
- prefer deterministic tests over manual confidence claims

## Android
Aim for:
- ViewModel tests
- use case tests
- mapper tests
- repository tests where practical

## Backend
Aim for:
- unit tests for application/domain logic
- integration tests for controller/persistence paths
- Testcontainers for database-backed integration tests where appropriate

## Delivery summary
Always report:
- commands executed
- pass/fail result
- unvalidated areas
- risks or blockers