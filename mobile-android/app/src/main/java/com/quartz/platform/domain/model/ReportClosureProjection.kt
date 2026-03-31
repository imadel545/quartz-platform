package com.quartz.platform.domain.model

sealed interface ReportClosureProjection {
    val sessionId: String
    val siteId: String
    val sectorId: String?
    val sectorCode: String?
    val workflowType: ReportDraftOriginWorkflowType?
    val updatedAtEpochMillis: Long
}

data class XfeederReportClosureProjection(
    override val sessionId: String,
    override val siteId: String,
    override val sectorId: String,
    override val sectorCode: String,
    val sectorOutcome: XfeederSectorOutcome,
    val relatedSectorCode: String?,
    val unreliableReason: XfeederUnreliableReason?,
    val observedSectorCount: Int?,
    override val updatedAtEpochMillis: Long
) : ReportClosureProjection {
    override val workflowType: ReportDraftOriginWorkflowType
        get() = ReportDraftOriginWorkflowType.XFEEDER
}

data class RetReportClosureProjection(
    override val sessionId: String,
    override val siteId: String,
    override val sectorId: String,
    override val sectorCode: String,
    val sessionStatus: RetSessionStatus,
    val resultOutcome: RetResultOutcome,
    val requiredStepCount: Int,
    val completedRequiredStepCount: Int,
    val measurementZoneRadiusMeters: Int,
    val proximityModeEnabled: Boolean,
    val resultSummary: String?,
    override val updatedAtEpochMillis: Long
) : ReportClosureProjection {
    override val workflowType: ReportDraftOriginWorkflowType
        get() = ReportDraftOriginWorkflowType.RET
}

data class ThroughputReportClosureProjection(
    override val sessionId: String,
    override val siteId: String,
    val siteCode: String,
    val sessionStatus: PerformanceSessionStatus,
    val preconditionsReady: Boolean,
    val requiredStepCount: Int,
    val completedRequiredStepCount: Int,
    val downloadMbps: Double?,
    val uploadMbps: Double?,
    val latencyMs: Int?,
    val minDownloadMbps: Double?,
    val minUploadMbps: Double?,
    val maxLatencyMs: Int?,
    val resultSummary: String?,
    override val updatedAtEpochMillis: Long
) : ReportClosureProjection {
    override val sectorId: String? = null
    override val sectorCode: String? = null
    override val workflowType: ReportDraftOriginWorkflowType = ReportDraftOriginWorkflowType.PERFORMANCE
}

data class QosReportClosureProjection(
    override val sessionId: String,
    override val siteId: String,
    val siteCode: String,
    val sessionStatus: PerformanceSessionStatus,
    val preconditionsReady: Boolean,
    val requiredStepCount: Int,
    val completedRequiredStepCount: Int,
    val scriptName: String?,
    val configuredRepeatCount: Int? = null,
    val configuredTechnologies: Set<String> = emptySet(),
    val scriptSnapshotUpdatedAtEpochMillis: Long? = null,
    val testFamilies: Set<QosTestFamily> = emptySet(),
    val familyExecutionResults: List<QosFamilyExecutionResult> = emptyList(),
    val executionTimelineEvents: List<QosExecutionTimelineEvent> = emptyList(),
    val executionEngineState: QosExecutionEngineState = QosExecutionEngineState.READY,
    val recoveryState: QosRecoveryState = QosRecoveryState.NONE,
    val checkpointCount: Int = 0,
    val activeFamily: QosTestFamily? = null,
    val activeRepetitionIndex: Int? = null,
    val nextFamily: QosTestFamily? = null,
    val nextRepetitionIndex: Int? = null,
    val targetTechnology: String?,
    val iterationCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val resultSummary: String?,
    override val updatedAtEpochMillis: Long
) : ReportClosureProjection {
    override val sectorId: String? = null
    override val sectorCode: String? = null
    override val workflowType: ReportDraftOriginWorkflowType = ReportDraftOriginWorkflowType.PERFORMANCE

    val selectedFamilyCount: Int
        get() = testFamilies.size

    val completedFamilyCount: Int
        get() = familyExecutionResults.count { result -> result.isCompleted }

    val failedFamilyCount: Int
        get() = familyExecutionResults.count { result -> result.status == QosFamilyExecutionStatus.FAILED }

    val blockedFamilyCount: Int
        get() = familyExecutionResults.count { result -> result.status == QosFamilyExecutionStatus.BLOCKED }

    val dominantIssueCode: QosExecutionIssueCode?
        get() = familyExecutionResults
            .asSequence()
            .filter { result ->
                result.status == QosFamilyExecutionStatus.FAILED ||
                    result.status == QosFamilyExecutionStatus.BLOCKED
            }
            .mapNotNull { result -> result.failureReasonCode }
            .groupingBy { code -> code }
            .eachCount()
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<QosExecutionIssueCode, Int>> { entry -> entry.value }
                    .thenBy { entry -> entry.key.name }
            )
            .firstOrNull()
            ?.key

    val timelineFamilyCoverageCount: Int
        get() = executionTimelineEvents.map { event -> event.family }.toSet().size

    val requiredRepeatCount: Int
        get() = configuredRepeatCount?.coerceAtLeast(1) ?: 1

    val passFailRunCount: Int
        get() = executionTimelineEvents.count { event ->
            event.eventType == QosExecutionEventType.PASSED ||
                event.eventType == QosExecutionEventType.FAILED
        }

    val blockedRunCount: Int
        get() = executionTimelineEvents.count { event ->
            event.eventType == QosExecutionEventType.BLOCKED
        }

    val plannedRunCount: Int
        get() = selectedFamilyCount * requiredRepeatCount

    val familiesMeetingRequiredRepeatCount: Int
        get() = testFamilies.count { family ->
            executionTimelineEvents.count { event ->
                event.family == family &&
                    (event.eventType == QosExecutionEventType.PASSED ||
                        event.eventType == QosExecutionEventType.FAILED)
            } >= requiredRepeatCount
        }

    val pendingRunCount: Int
        get() = (plannedRunCount - passFailRunCount - blockedRunCount).coerceAtLeast(0)
}
