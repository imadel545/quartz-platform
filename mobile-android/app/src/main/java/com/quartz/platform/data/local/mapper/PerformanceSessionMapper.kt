package com.quartz.platform.data.local.mapper

import com.quartz.platform.data.local.entity.PerformanceQosFamilyResultEntity
import com.quartz.platform.data.local.entity.PerformanceQosTimelineEventEntity
import com.quartz.platform.data.local.entity.PerformanceSessionEntity
import com.quartz.platform.data.local.entity.PerformanceStepEntity
import com.quartz.platform.domain.model.QosFamilyExecutionResult
import com.quartz.platform.domain.model.QosExecutionEventType
import com.quartz.platform.domain.model.QosExecutionTimelineEvent
import com.quartz.platform.domain.model.QosFamilyExecutionStatus
import com.quartz.platform.domain.model.PerformanceGuidedStep
import com.quartz.platform.domain.model.PerformanceSession
import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.PerformanceStepCode
import com.quartz.platform.domain.model.PerformanceStepStatus
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.model.QosRunSummary
import com.quartz.platform.domain.model.QosTestFamily
import com.quartz.platform.domain.model.ThroughputMetrics
import com.quartz.platform.domain.model.qosExecutionEventSortOrder

fun PerformanceSessionEntity.toDomain(
    steps: List<PerformanceStepEntity>,
    familyResults: List<PerformanceQosFamilyResultEntity>,
    timelineEvents: List<PerformanceQosTimelineEventEntity>
): PerformanceSession {
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
            configuredRepeatCount = qosConfiguredRepeatCount,
            configuredTechnologies = qosConfiguredTechnologiesCsv.split(',')
                .map { value -> value.trim() }
                .filter { value -> value.isNotBlank() }
                .sorted()
                .toCollection(linkedSetOf()),
            scriptSnapshotUpdatedAtEpochMillis = qosScriptSnapshotUpdatedAtEpochMillis,
            selectedTestFamilies = qosTestFamiliesCsv.split(',')
                .map { value -> value.trim() }
                .filter { value -> value.isNotBlank() }
                .mapNotNull { raw -> runCatching { QosTestFamily.valueOf(raw) }.getOrNull() }
                .sortedBy { family -> family.name }
                .toCollection(linkedSetOf()),
            familyExecutionResults = familyResults.mapNotNull { result ->
                val family = runCatching { QosTestFamily.valueOf(result.family) }.getOrNull()
                    ?: return@mapNotNull null
                QosFamilyExecutionResult(
                    family = family,
                    status = runCatching { QosFamilyExecutionStatus.valueOf(result.status) }
                        .getOrDefault(QosFamilyExecutionStatus.NOT_RUN),
                    failureReason = result.failureReason,
                    observedLatencyMs = result.observedLatencyMs,
                    observedDownloadMbps = result.observedDownloadMbps,
                    observedUploadMbps = result.observedUploadMbps
                )
            }.sortedBy { result -> result.family.name },
            executionTimelineEvents = timelineEvents.mapNotNull { event ->
                val family = runCatching { QosTestFamily.valueOf(event.family) }.getOrNull()
                    ?: return@mapNotNull null
                QosExecutionTimelineEvent(
                    family = family,
                    repetitionIndex = event.repetitionIndex,
                    eventType = runCatching {
                        QosExecutionEventType.valueOf(event.eventType)
                    }.getOrDefault(QosExecutionEventType.STARTED),
                    reason = event.reason,
                    occurredAtEpochMillis = event.occurredAtEpochMillis
                )
            }.sortedWith(
                compareByDescending<QosExecutionTimelineEvent> { event ->
                    event.occurredAtEpochMillis
                }.thenBy { event ->
                    event.family.name
                }.thenBy { event ->
                    event.repetitionIndex
                }.thenBy { event ->
                    qosExecutionEventSortOrder(event.eventType)
                }
            ),
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

fun QosFamilyExecutionResult.toEntity(
    sessionId: String,
    updatedAtEpochMillis: Long
): PerformanceQosFamilyResultEntity {
    return PerformanceQosFamilyResultEntity(
        sessionId = sessionId,
        family = family.name,
        status = status.name,
        failureReason = failureReason?.trim()?.takeIf { value -> value.isNotBlank() },
        observedLatencyMs = observedLatencyMs,
        observedDownloadMbps = observedDownloadMbps,
        observedUploadMbps = observedUploadMbps,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}

fun QosExecutionTimelineEvent.toEntity(
    sessionId: String
): PerformanceQosTimelineEventEntity {
    return PerformanceQosTimelineEventEntity(
        sessionId = sessionId,
        family = family.name,
        repetitionIndex = repetitionIndex.coerceAtLeast(1),
        eventType = eventType.name,
        reason = reason?.trim()?.takeIf { value -> value.isNotBlank() },
        occurredAtEpochMillis = occurredAtEpochMillis
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
        qosConfiguredRepeatCount = qosRunSummary.configuredRepeatCount,
        qosConfiguredTechnologiesCsv = qosRunSummary.configuredTechnologies
            .map { technology -> technology.trim() }
            .filter { technology -> technology.isNotBlank() }
            .toSortedSet()
            .joinToString(","),
        qosScriptSnapshotUpdatedAtEpochMillis = qosRunSummary.scriptSnapshotUpdatedAtEpochMillis,
        qosTestFamiliesCsv = qosRunSummary.selectedTestFamilies
            .map { family -> family.name }
            .sorted()
            .joinToString(","),
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
