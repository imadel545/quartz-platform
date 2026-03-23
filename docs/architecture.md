# Architecture

## 1. Objective

Quartz Platform must be rebuilt as a modular, production-grade, offline-first telecom field operations system.

The platform includes:
- an Android mobile application for field technicians
- a backend API for synchronization, reporting, and domain workflows
- a back-office web platform for supervision and administration
- an infrastructure layer for local development, deployment, observability, and runtime operations

This is not a basic CRUD application.
It is a field-oriented operational platform with geospatial workflows, telecom-specific constraints, device-specific capabilities, offline execution requirements, and reporting responsibilities.

---

## 2. Product-driven architecture context

The product scope comes from a Quartz operational guide describing:
- main map-based site search and site selection
- detailed site inspection by sector, antenna, and cell
- XFeeder / MixFeeder workflows
- throughput tests
- mode proximity
- mode drive
- RET validation flows
- QOS and QOS scripts
- report browsing
- device-specific locking band workflows
- web back office

The new implementation must preserve the business intent while modernizing architecture, quality, maintainability, observability, and scalability.

---

## 3. High-level system architecture

```text
Field Technician
    |
    v
Android Mobile App
    |
    | HTTPS / Authenticated API / Sync
    v
Backend API
    |
    +--> PostgreSQL / PostGIS
    +--> Redis
    +--> Object Storage
    +--> Metrics / Logs / Traces
    |
    v
Back-office Web App