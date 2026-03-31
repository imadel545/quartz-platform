# Domain Model

## Objective

Define the initial explicit domain model for Quartz Platform.

This model is intentionally designed to avoid vague generic objects and to preserve clear business semantics across mobile, backend, and reporting workflows.

---

## Domain principles

- domain objects must represent real business concepts
- naming must stay explicit
- entities and value objects must not be reduced to transport-only structures
- the model must support offline-first workflows and synchronization
- the model must support progressive extension for telecom-specific constraints

---

## Core domain areas

### Identity and access
- User
- Role
- Account

### Device and execution context
- Device
- DeviceCapability
- DeviceStatus

### Telecom reference model
- Site
- Sector
- Antenna
- Cell

### Field execution
- TestSession
- TestStep
- ThroughputRun
- QoSScript
- QoSRun
- RetValidationRun

### Reporting and synchronization
- Report
- SyncJob
- AuditEntry

---

## Core entities

## User
Represents a platform user.

Suggested fields:
- id
- accountId
- email
- displayName
- role
- status
- createdAt
- updatedAt

## Role
Represents the user's authorization scope.

Initial values may include:
- TECHNICIAN
- SUPERVISOR
- ADMIN

## Account
Represents an organizational context.

Suggested fields:
- id
- name
- status
- createdAt
- updatedAt

---

## Device
Represents a physical Android device used in field operations.

Suggested fields:
- id
- accountId
- assignedUserId
- manufacturer
- model
- osVersion
- appVersion
- status
- lastSeenAt

## DeviceCapability
Represents what the device can or cannot support.

Suggested capability dimensions:
- supportsTelephonyInspection
- supportsAdvancedRadioFeatures
- supportsLocationPrecisionMode
- supportsThroughputWorkflow
- supportsQoSWorkflow
- degradedReason

## DeviceStatus
Represents runtime visibility of device health and readiness.

Suggested fields:
- batteryLevel
- networkState
- locationState
- permissionState
- syncState
- lastHeartbeatAt

---

## Site
Represents a telecom site.

Suggested fields:
- id
- externalCode
- name
- latitude
- longitude
- status
- metadataVersion

## Sector
Represents a sector associated with a site.

Suggested fields:
- id
- siteId
- name
- azimuth
- status

## Antenna
Represents an antenna asset or configuration item.

Suggested fields:
- id
- sectorId
- reference
- installedState
- forecastState
- tiltConfigured
- tiltObserved

## Cell
Represents a network cell associated with a sector and/or antenna.

Suggested fields:
- id
- sectorId
- antennaId
- technology
- operator
- band
- pciOrEquivalent
- status

---

## TestSession
Represents a technician-driven execution session on or around a site.

Suggested fields:
- id
- siteId
- userId
- deviceId
- type
- status
- startedAt
- endedAt
- localOnly
- syncStatus

For guided geospatial sessions (for example XFeeder), a bounded geospatial context is tracked locally:
- measurement zone radius
- extension reason
- proximity mode flag
- effective proximity reference altitude
- reference altitude source (`TECHNICAL_DEFAULT`, `OPERATOR_OVERRIDE`, `UNAVAILABLE`)
- computed proximity eligibility state (`SUPPORTED`, `UNAVAILABLE`, `INELIGIBLE`, `ELIGIBLE`)

For bounded site-level performance workflows (Débit/QoS foundation), local session state tracks:
- workflow type (`THROUGHPUT`, `QOS_SCRIPT`)
- QoS test families (`THROUGHPUT_LATENCY`, `VIDEO_STREAMING`, `SMS`, `VOLTE_CALL`, `CSFB_CALL`, `EMERGENCY_CALL`, `STANDARD_CALL`)
- QoS family execution evidence per selected family with explicit status (`NOT_RUN`, `PASSED`, `FAILED`, `BLOCKED`)
- QoS timeline evidence per selected family with typed events (`STARTED`, `PASSED`, `FAILED`, `BLOCKED`) and timestamped occurrence
- script snapshot context captured at execution time:
  - configured technologies set
  - script snapshot timestamp
- prerequisite readiness flags
- structured throughput metrics and thresholds
- structured QoS script execution summary
- required step progression and completion guard
- completion invariant for `QOS_SCRIPT` closure:
  - selected families present
  - selected families covered by completed family evidence
  - failed families must include failure reason
  - phone target required for selected call/SMS families
  - target technology must align with configured script technologies when available
  - aggregate counters coherent with family evidence
- bounded closure projection for local report review:
  - detailed projection in ReportDraft
  - concise triage projection in ReportList (including QoS family coverage/failure/blocked/timeline signals)
- linked report-draft continuity with explicit provenance (`originWorkflowType = PERFORMANCE`, `originSessionId`).
- local QoS script definitions persisted in Room (`qos_scripts`) with name, repeat count, technologies, and selected test families.

Possible status values:
- CREATED
- IN_PROGRESS
- COMPLETED
- FAILED
- CANCELLED
- SYNC_PENDING
- SYNCED

## TestStep
Represents a structured step inside a test session.

Suggested fields:
- id
- testSessionId
- code
- label
- status
- startedAt
- completedAt
- resultSummary

---

## ThroughputRun
Represents a throughput measurement execution.

Suggested fields:
- id
- testSessionId
- operator
- technology
- downloadMbps
- uploadMbps
- latencyMs
- status
- measuredAt
- latitude
- longitude

## QoSScript
Represents a reusable QoS execution definition.

Suggested fields:
- id
- accountId
- name
- version
- enabled
- definition
- createdAt
- updatedAt

## QoSRun
Represents one execution of a QoS scenario/script.

Suggested fields:
- id
- testSessionId
- qosScriptId
- status
- startedAt
- endedAt
- summary
- rawResultReference

## RetValidationRun
Represents a RET-oriented validation workflow execution.

Suggested fields:
- id
- testSessionId
- operator
- band
- status
- calibrationStatus
- validationSummary
- executedAt

---

## Report
Represents a field report produced from one or more workflows.

Suggested fields:
- id
- accountId
- siteId
- userId
- deviceId
- testSessionId
- status
- localCreatedAt
- submittedAt
- syncStatus
- summary
- attachmentCount

Possible sync values:
- LOCAL_ONLY
- PENDING
- SYNCED
- FAILED

For local-first review before backend sync, report drafts may expose workflow-typed closure projections
(for example XFeeder sector closure or RET closure summary) instead of raw workflow internals.

## SyncJob
Represents a durable synchronization unit.

Suggested fields:
- id
- aggregateType
- aggregateId
- operationType
- payloadReference
- status
- retryCount
- lastAttemptAt
- nextAttemptAt

## AuditEntry
Represents traceability for relevant platform actions.

Suggested fields:
- id
- actorType
- actorId
- action
- targetType
- targetId
- timestamp
- details
