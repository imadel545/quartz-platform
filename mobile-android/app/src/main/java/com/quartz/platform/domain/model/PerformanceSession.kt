package com.quartz.platform.domain.model

import com.quartz.platform.domain.model.workflow.WorkflowClosureSummary
import com.quartz.platform.domain.model.workflow.WorkflowCompletionGuard
import com.quartz.platform.domain.model.workflow.WorkflowSessionIdentity
import com.quartz.platform.domain.model.workflow.WorkflowSessionStatus
import com.quartz.platform.domain.model.workflow.WorkflowStepState
import com.quartz.platform.domain.model.workflow.WorkflowStepStatus

enum class PerformanceWorkflowType {
    THROUGHPUT,
    QOS_SCRIPT
}

enum class QosTestFamily {
    THROUGHPUT_LATENCY,
    VIDEO_STREAMING,
    SMS,
    VOLTE_CALL,
    CSFB_CALL,
    EMERGENCY_CALL,
    STANDARD_CALL
}

enum class QosFamilyExecutionStatus {
    NOT_RUN,
    PASSED,
    FAILED,
    BLOCKED
}

data class QosFamilyExecutionResult(
    val family: QosTestFamily,
    val status: QosFamilyExecutionStatus,
    val failureReason: String? = null,
    val observedLatencyMs: Int? = null,
    val observedDownloadMbps: Double? = null,
    val observedUploadMbps: Double? = null
) {
    val isCompleted: Boolean
        get() = status == QosFamilyExecutionStatus.PASSED || status == QosFamilyExecutionStatus.FAILED
}

typealias PerformanceSessionStatus = WorkflowSessionStatus
typealias PerformanceStepStatus = WorkflowStepStatus
typealias PerformanceGuidedStep = WorkflowStepState<PerformanceStepCode>

enum class PerformanceStepCode {
    PRECONDITIONS_CHECK,
    EXECUTE_TEST,
    REVIEW_RESULT,
    SEND_RESULT
}

data class ThroughputMetrics(
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val latencyMs: Int? = null,
    val minDownloadMbps: Double? = null,
    val minUploadMbps: Double? = null,
    val maxLatencyMs: Int? = null
) {
    val hasAnyMeasurement: Boolean
        get() = downloadMbps != null || uploadMbps != null || latencyMs != null
}

data class QosRunSummary(
    val scriptId: String? = null,
    val scriptName: String? = null,
    val configuredRepeatCount: Int? = null,
    val configuredTechnologies: Set<String> = emptySet(),
    val scriptSnapshotUpdatedAtEpochMillis: Long? = null,
    val selectedTestFamilies: Set<QosTestFamily> = emptySet(),
    val familyExecutionResults: List<QosFamilyExecutionResult> = emptyList(),
    val targetTechnology: String? = null,
    val targetPhoneNumber: String? = null,
    val iterationCount: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0
)

data class PerformanceSession(
    val id: String,
    val siteId: String,
    val siteCode: String,
    val workflowType: PerformanceWorkflowType,
    val operatorName: String?,
    val technology: String?,
    val status: PerformanceSessionStatus,
    val prerequisiteNetworkReady: Boolean,
    val prerequisiteBatterySufficient: Boolean,
    val prerequisiteLocationReady: Boolean,
    val throughputMetrics: ThroughputMetrics,
    val qosRunSummary: QosRunSummary,
    val notes: String,
    val resultSummary: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val completedAtEpochMillis: Long?,
    val steps: List<PerformanceGuidedStep>
) {
    val identity: WorkflowSessionIdentity
        get() = WorkflowSessionIdentity(
            sessionId = id,
            siteId = siteId,
            scopeId = siteId,
            scopeCode = siteCode
        )

    val closureSummary: WorkflowClosureSummary<PerformanceWorkflowType>
        get() = WorkflowClosureSummary(
            outcome = workflowType,
            notes = notes,
            resultSummary = resultSummary
        )

    val preconditionsReady: Boolean
        get() = prerequisiteNetworkReady && prerequisiteBatterySufficient && prerequisiteLocationReady

    fun completionGuard(): WorkflowCompletionGuard = WorkflowCompletionGuard.fromSteps(steps)
}
