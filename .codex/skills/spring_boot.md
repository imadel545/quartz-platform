# Spring Boot Skill

Use this skill when bootstrapping or modifying the backend API.

## Target stack
- Java 21
- Spring Boot 3
- Spring Security
- PostgreSQL
- PostGIS
- Flyway
- Redis
- OpenAPI

## Objective
Create a backend that is modular, explicit, testable, and production-oriented.

## Structural expectations
Use layered packaging with clear responsibilities:
- config
- security
- controller
- application
- domain
- infrastructure

## Mandatory rules
- controllers remain thin
- application layer orchestrates workflows
- domain stays independent from transport details
- infrastructure holds persistence/adapters
- database evolution is migration-driven
- OpenAPI baseline must be present
- configuration must be environment-driven

## Validation expectations
Whenever feasible:
- ensure project compiles
- ensure app can start locally
- ensure Flyway runs
- report actual validation commands and blockers

## Avoid
- giant service classes doing everything
- leaking JPA entities everywhere without boundary discipline
- mixing persistence and API contract logic
- skipping migrations for schema changes