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
        val preconditionsReady: Boolean,
        val requiredStepCount: Int,
        val completedRequiredStepCount: Int,
        val scriptName: String?,
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
