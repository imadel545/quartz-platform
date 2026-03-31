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
    val targetTechnology: String? = null,
    val targetPhoneNumber: String? = null,
    val iterationCount: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0
)

data class QosScriptTemplate(
    val id: String,
    val name: String,
    val description: String
)

object LocalQosScriptCatalog {
    val defaults: List<QosScriptTemplate> = listOf(
        QosScriptTemplate(
            id = "qos-latency-throughput",
            name = "Latence + Débit",
            description = "Ping série + upload/download avec vérification des seuils."
        ),
        QosScriptTemplate(
            id = "qos-voice-sms",
            name = "Voix / SMS",
            description = "Vérification appel VoLTE/CSFB, appel standard et confirmation SMS."
        ),
        QosScriptTemplate(
            id = "qos-video-streaming",
            name = "Streaming vidéo",
            description = "Lecture multi-résolution, buffering et latence de session vidéo."
        )
    )
}

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
