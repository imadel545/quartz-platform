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
    override val workflowType: ReportDraftOriginWorkflowType? = null
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
    val targetTechnology: String?,
    val iterationCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val resultSummary: String?,
    override val updatedAtEpochMillis: Long
) : ReportClosureProjection {
    override val sectorId: String? = null
    override val sectorCode: String? = null
    override val workflowType: ReportDraftOriginWorkflowType? = null
}
