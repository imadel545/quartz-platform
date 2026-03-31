package com.quartz.platform.data.local.mapper

import com.quartz.platform.data.local.entity.PerformanceSessionEntity
import com.quartz.platform.data.local.entity.PerformanceStepEntity
import com.quartz.platform.domain.model.PerformanceGuidedStep
import com.quartz.platform.domain.model.PerformanceSession
import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.PerformanceStepCode
import com.quartz.platform.domain.model.PerformanceStepStatus
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.model.QosRunSummary
import com.quartz.platform.domain.model.ThroughputMetrics

fun PerformanceSessionEntity.toDomain(steps: List<PerformanceStepEntity>): PerformanceSession {
    return PerformanceSession(
        id = id,
        siteId = siteId,
        siteCode = siteCode,
        workflowType = runCatching {
            PerformanceWorkflowType.valueOf(workflowType)
        }.getOrDefault(PerformanceWorkflowType.THROUGHPUT),
        operatorName = operatorName,
        technology = technology,
        status = runCatching {
            PerformanceSessionStatus.valueOf(status)
        }.getOrDefault(PerformanceSessionStatus.CREATED),
        prerequisiteNetworkReady = prerequisiteNetworkReady,
        prerequisiteBatterySufficient = prerequisiteBatterySufficient,
        prerequisiteLocationReady = prerequisiteLocationReady,
        throughputMetrics = ThroughputMetrics(
            downloadMbps = throughputDownloadMbps,
            uploadMbps = throughputUploadMbps,
            latencyMs = throughputLatencyMs,
            minDownloadMbps = throughputMinDownloadMbps,
            minUploadMbps = throughputMinUploadMbps,
            maxLatencyMs = throughputMaxLatencyMs
        ),
        qosRunSummary = QosRunSummary(
            scriptId = qosScriptId,
            scriptName = qosScriptName,
            targetTechnology = qosTargetTechnology,
            targetPhoneNumber = qosTargetPhoneNumber,
            iterationCount = qosIterationCount,
            successCount = qosSuccessCount,
            failureCount = qosFailureCount
        ),
        notes = notes,
        resultSummary = resultSummary,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        completedAtEpochMillis = completedAtEpochMillis,
        steps = steps.map { step ->
            PerformanceGuidedStep(
                code = PerformanceStepCode.valueOf(step.code),
                required = step.required,
                status = runCatching {
                    PerformanceStepStatus.valueOf(step.status)
                }.getOrDefault(PerformanceStepStatus.TODO)
            )
        }
    )
}

fun PerformanceSession.toEntity(): PerformanceSessionEntity {
    return PerformanceSessionEntity(
        id = id,
        siteId = siteId,
        siteCode = siteCode,
        workflowType = workflowType.name,
        operatorName = operatorName,
        technology = technology,
        status = status.name,
        prerequisiteNetworkReady = prerequisiteNetworkReady,
        prerequisiteBatterySufficient = prerequisiteBatterySufficient,
        prerequisiteLocationReady = prerequisiteLocationReady,
        throughputDownloadMbps = throughputMetrics.downloadMbps,
        throughputUploadMbps = throughputMetrics.uploadMbps,
        throughputLatencyMs = throughputMetrics.latencyMs,
        throughputMinDownloadMbps = throughputMetrics.minDownloadMbps,
        throughputMinUploadMbps = throughputMetrics.minUploadMbps,
        throughputMaxLatencyMs = throughputMetrics.maxLatencyMs,
        qosScriptId = qosRunSummary.scriptId,
        qosScriptName = qosRunSummary.scriptName,
        qosTargetTechnology = qosRunSummary.targetTechnology,
        qosTargetPhoneNumber = qosRunSummary.targetPhoneNumber,
        qosIterationCount = qosRunSummary.iterationCount,
        qosSuccessCount = qosRunSummary.successCount,
        qosFailureCount = qosRunSummary.failureCount,
        notes = notes,
        resultSummary = resultSummary,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        completedAtEpochMillis = completedAtEpochMillis
    )
}

fun PerformanceGuidedStep.toEntity(sessionId: String, displayOrder: Int): PerformanceStepEntity {
    return PerformanceStepEntity(
        sessionId = sessionId,
        code = code.name,
        required = required,
        status = status.name,
        displayOrder = displayOrder
    )
}
