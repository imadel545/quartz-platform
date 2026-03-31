package com.quartz.platform.domain.model

data class SiteReportListItem(
    val draftId: String,
    val siteId: String,
    val originWorkflowType: ReportDraftOriginWorkflowType? = null,
    val title: String,
    val revision: Int,
    val updatedAtEpochMillis: Long,
    val syncTrace: ReportSyncTrace,
    val closureSummary: ReportListClosureSummary? = null
) {
    val syncState: ReportSyncState
        get() = syncTrace.state
}

sealed interface ReportListClosureSummary {
    data class Xfeeder(
        val sectorOutcome: XfeederSectorOutcome,
        val signal: XfeederSignal? = null
    ) : ReportListClosureSummary

    data class Ret(
        val resultOutcome: RetResultOutcome,
        val requiredStepCount: Int,
        val completedRequiredStepCount: Int
    ) : ReportListClosureSummary

    data class Throughput(
        val sessionStatus: PerformanceSessionStatus,
        val preconditionsReady: Boolean,
        val requiredStepCount: Int,
        val completedRequiredStepCount: Int,
        val downloadMbps: Double?,
        val uploadMbps: Double?,
        val latencyMs: Int?
    ) : ReportListClosureSummary

    data class Qos(
        val sessionStatus: PerformanceSessionStatus,
        val executionEngineState: QosExecutionEngineState,
        val preconditionsReady: Boolean,
        val requiredStepCount: Int,
        val completedRequiredStepCount: Int,
        val scriptName: String?,
        val configuredRepeatCount: Int? = null,
        val targetTechnology: String? = null,
        val configuredTechnologyCount: Int = 0,
        val targetTechnologyAligned: Boolean = true,
        val testFamilyCount: Int = 0,
        val completedFamilyCount: Int = 0,
        val failedFamilyCount: Int = 0,
        val blockedFamilyCount: Int = 0,
        val timelineEventCount: Int = 0,
        val timelineFamilyCoverageCount: Int = 0,
        val requiredRepeatCount: Int = 1,
        val familiesMeetingRequiredRepeatCount: Int = 0,
        val passFailRunCount: Int = 0,
        val blockedRunCount: Int = 0,
        val plannedRunCount: Int = 0,
        val pendingRunCount: Int = 0,
        val activeFamily: QosTestFamily? = null,
        val activeRepetitionIndex: Int? = null,
        val iterationCount: Int,
        val successCount: Int,
        val failureCount: Int
    ) : ReportListClosureSummary
}

enum class XfeederSignal {
    RELATED_SECTOR,
    UNRELIABLE,
    OBSERVED_MULTIPLE
}
