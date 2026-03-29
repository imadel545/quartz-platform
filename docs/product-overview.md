# Product Overview

## Product name

Quartz Platform

## Purpose

Quartz Platform is a field operations platform designed to assist telecom technicians and operational teams during on-site validation, measurement, testing, and reporting activities.

The platform combines:
- a mobile Android application used in the field
- a backend API for synchronization, domain logic and reporting
- a back-office web interface for administration and supervision

## Context

The project is based on:
- an operational guide describing workflows and major features
- a legacy APK artifact that provides technical clues but not a sufficient base for modern implementation

The new system is a modern rebuild, not a direct legacy continuation.

## Primary users

### Field technician
Uses the Android app to:
- find and inspect telecom sites
- visualize location and nearby context
- run measurement and validation workflows
- execute throughput and QoS scenarios
- capture and synchronize reports

### Supervisor / operations manager
Uses the back office to:
- supervise reports and execution quality
- manage accounts and device fleet
- inspect configuration and supported options
- distribute operational documentation and versions

### Administrator
Manages:
- access control
- site data
- sector/cell reference data
- scripts and validation options
- deployment and platform health

## Functional domains

### 1. Site discovery and map workflow
The system must support:
- site search by identifier or name
- map visualization
- current user position
- site selection
- contextual display of related information

### 2. Site technical inspection
The platform should expose technical information related to:
- sectors
- antennas
- cells
- installed vs forecast configuration
- related attributes required by field operations

### 3. Test session execution
The mobile application must support structured field sessions with:
- clear entry point
- preconditions
- execution states
- result capture
- resumable workflows
- final reporting

Current validated guided-session coverage includes XFeeder and RET shells with local persistence/resume, completion guards, and report-draft continuity. XFeeder proximity handling is altitude-aware with explicit local reference-altitude provenance (technical default vs operator override vs unavailable), while remaining intentionally bounded (no full telecom automation claims).

### 4. Throughput and QoS testing
The system should support:
- latency checks
- download/upload tests
- scenario-based QoS execution
- result storage
- operator/device traceability

### 5. RET and advanced validation flows
The product roadmap includes guided validation flows such as:
- RET workflows
- step reset/retry support
- contextual history
- operator/band specific validation

### 6. Reporting
The platform must provide:
- report generation
- synchronization status
- technician visibility
- supervisor access through web back office

### 7. Device and capability awareness
The system must account for:
- Android version differences
- manufacturer-specific constraints
- radio/network capability limitations
- feature degradation when unsupported

## Product requirements philosophy

Quartz Platform must be:

- offline-first
- field-resilient
- geospatially aware
- modular
- observable
- secure
- scalable
- maintainable

## Strategic implementation direction

The recommended implementation stack is:

### Mobile
- Kotlin
- Jetpack Compose
- MVVM + Clean Architecture
- Room
- WorkManager
- Coroutines / Flow
- Hilt

### Backend
- Java 21
- Spring Boot 3
- PostgreSQL + PostGIS
- Flyway
- Redis
- OpenAPI

### Web
- Next.js
- TypeScript

## Delivery strategy

The platform should be rebuilt incrementally:

### Phase 1
- architecture and repository bootstrap
- documentation and domain framing

### Phase 2
- Android and backend foundations
- local dev environment
- CI baseline

### Phase 3
- MVP map/site/reporting workflow

### Phase 4
- advanced test orchestration
- QoS scenarios
- device capability matrix
- reporting hardening

## Quality expectations

This project must be executed at a high engineering standard with:

- explicit architecture
- scalable code structure
- strong validation discipline
- production-oriented design
- minimal technical debt creation
