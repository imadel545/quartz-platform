package com.quartz.platform.data.repository

import androidx.room.withTransaction
import com.quartz.platform.data.local.QuartzDatabase
import com.quartz.platform.data.local.dao.PerformanceQosFamilyResultDao
import com.quartz.platform.data.local.dao.PerformanceQosTimelineEventDao
import com.quartz.platform.data.local.dao.PerformanceSessionDao
import com.quartz.platform.data.local.dao.PerformanceStepDao
import com.quartz.platform.data.local.entity.PerformanceStepEntity
import com.quartz.platform.data.local.mapper.toEntity
import com.quartz.platform.data.local.mapper.toDomain
import com.quartz.platform.domain.model.PerformanceGuidedStep
import com.quartz.platform.domain.model.PerformanceSession
import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.PerformanceStepCode
import com.quartz.platform.domain.model.PerformanceStepStatus
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.model.QosCompletionIssue
import com.quartz.platform.domain.model.QosFamilyExecutionStatus
import com.quartz.platform.domain.model.QosExecutionEventType
import com.quartz.platform.domain.model.QosExecutionTimelineEvent
import com.quartz.platform.domain.model.QosRunSummary
import com.quartz.platform.domain.model.ThroughputMetrics
import com.quartz.platform.domain.model.assessQosCompletion
import com.quartz.platform.domain.model.qosExecutionEventSortOrder
import com.quartz.platform.domain.model.workflow.WorkflowCompletionGuard
import com.quartz.platform.domain.repository.PerformanceSessionRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstPerformanceSessionRepository @Inject constructor(
    private val sessionDao: PerformanceSessionDao,
    private val stepDao: PerformanceStepDao,
    private val qosFamilyResultDao: PerformanceQosFamilyResultDao,
    private val qosTimelineEventDao: PerformanceQosTimelineEventDao,
    private val database: QuartzDatabase
) : PerformanceSessionRepository {

    override fun observeSiteSessionHistory(siteId: String): Flow<List<PerformanceSession>> {
        return sessionDao.observeHistoryBySite(siteId)
            .flatMapLatest { sessions ->
                if (sessions.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val sessionIds = sessions.map { it.id }
                    combine(
                        stepDao.observeBySessionIds(sessionIds),
                        qosFamilyResultDao.observeBySessionIds(sessionIds),
                        qosTimelineEventDao.observeBySessionIds(sessionIds)
                    ) { steps, familyResults, timelineEvents ->
                        val stepsBySession = steps.groupBy { step -> step.sessionId }
                        val familyResultsBySession = familyResults.groupBy { result -> result.sessionId }
                        val timelineBySession = timelineEvents.groupBy { event -> event.sessionId }
                        sessions.map { session ->
                            session.toDomain(
                                steps = stepsBySession[session.id].orEmpty(),
                                familyResults = familyResultsBySession[session.id].orEmpty(),
                                timelineEvents = timelineBySession[session.id].orEmpty()
                            )
                        }
                    }
                }
            }
    }

    override fun observeLatestSiteSession(
        siteId: String,
        workflowType: PerformanceWorkflowType
    ): Flow<PerformanceSession?> {
        return sessionDao.observeLatestBySiteAndType(siteId, workflowType.name)
            .flatMapLatest { session ->
                if (session == null) {
                    flowOf(null)
                } else {
                    combine(
                        stepDao.observeBySession(session.id),
                        qosFamilyResultDao.observeBySession(session.id),
                        qosTimelineEventDao.observeBySession(session.id)
                    ) { steps, familyResults, timelineEvents ->
                        session.toDomain(
                            steps = steps,
                            familyResults = familyResults,
                            timelineEvents = timelineEvents
                        )
                    }
                }
            }
    }

    override suspend fun createSession(
        siteId: String,
        siteCode: String,
        workflowType: PerformanceWorkflowType,
        operatorName: String?,
        technology: String?
    ): PerformanceSession {
        val now = System.currentTimeMillis()
        val session = PerformanceSession(
            id = UUID.randomUUID().toString(),
            siteId = siteId,
            siteCode = siteCode,
            workflowType = workflowType,
            operatorName = operatorName?.trim()?.takeIf { it.isNotBlank() },
            technology = technology?.trim()?.takeIf { it.isNotBlank() },
            status = PerformanceSessionStatus.CREATED,
            prerequisiteNetworkReady = false,
            prerequisiteBatterySufficient = false,
            prerequisiteLocationReady = false,
            throughputMetrics = ThroughputMetrics(),
            qosRunSummary = QosRunSummary(),
            notes = "",
            resultSummary = "",
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            completedAtEpochMillis = null,
            steps = defaultStepsFor(workflowType)
        )

        database.withTransaction {
            sessionDao.upsert(session.toEntity())
            stepDao.upsertAll(session.steps.mapIndexed { index, step ->
                step.toEntity(sessionId = session.id, displayOrder = index)
            })
        }
        return session
    }

    override suspend fun updateStepStatus(
        sessionId: String,
        stepCode: PerformanceStepCode,
        status: PerformanceStepStatus
    ) {
        database.withTransaction {
            stepDao.updateStatus(
                sessionId = sessionId,
                code = stepCode.name,
                status = status.name
            )
            val existing = sessionDao.getById(sessionId) ?: return@withTransaction
            val nextStatus = if (
                existing.status == PerformanceSessionStatus.CREATED.name &&
                status != PerformanceStepStatus.TODO
            ) {
                PerformanceSessionStatus.IN_PROGRESS
            } else {
                PerformanceSessionStatus.valueOf(existing.status)
            }

            sessionDao.updateExecution(
                sessionId = sessionId,
                status = nextStatus.name,
                prerequisiteNetworkReady = existing.prerequisiteNetworkReady,
                prerequisiteBatterySufficient = existing.prerequisiteBatterySufficient,
                prerequisiteLocationReady = existing.prerequisiteLocationReady,
                throughputDownloadMbps = existing.throughputDownloadMbps,
                throughputUploadMbps = existing.throughputUploadMbps,
                throughputLatencyMs = existing.throughputLatencyMs,
                throughputMinDownloadMbps = existing.throughputMinDownloadMbps,
                throughputMinUploadMbps = existing.throughputMinUploadMbps,
                throughputMaxLatencyMs = existing.throughputMaxLatencyMs,
                qosScriptId = existing.qosScriptId,
                qosScriptName = existing.qosScriptName,
                qosConfiguredRepeatCount = existing.qosConfiguredRepeatCount,
                qosConfiguredTechnologiesCsv = existing.qosConfiguredTechnologiesCsv,
                qosScriptSnapshotUpdatedAtEpochMillis = existing.qosScriptSnapshotUpdatedAtEpochMillis,
                qosTestFamiliesCsv = existing.qosTestFamiliesCsv,
                qosTargetTechnology = existing.qosTargetTechnology,
                qosTargetPhoneNumber = existing.qosTargetPhoneNumber,
                qosIterationCount = existing.qosIterationCount,
                qosSuccessCount = existing.qosSuccessCount,
                qosFailureCount = existing.qosFailureCount,
                notes = existing.notes,
                resultSummary = existing.resultSummary,
                updatedAtEpochMillis = System.currentTimeMillis(),
                completedAtEpochMillis = if (nextStatus == PerformanceSessionStatus.COMPLETED) {
                    existing.completedAtEpochMillis
                } else {
                    null
                }
            )
        }
    }

    override suspend fun updateSessionExecution(
        sessionId: String,
        status: PerformanceSessionStatus,
        prerequisiteNetworkReady: Boolean,
        prerequisiteBatterySufficient: Boolean,
        prerequisiteLocationReady: Boolean,
        throughputMetrics: ThroughputMetrics,
        qosRunSummary: QosRunSummary,
        notes: String,
        resultSummary: String
    ) {
        if (status == PerformanceSessionStatus.COMPLETED) {
            val existing = sessionDao.getById(sessionId)
                ?: throw IllegalStateException("Performance session introuvable.")
            val steps = stepDao.listBySession(sessionId)
            if (!buildPerformanceCompletionGuard(steps).canComplete) {
                throw IllegalStateException("Required Débit/QoS steps must be completed before closing the session.")
            }
            if (existing.workflowType == PerformanceWorkflowType.QOS_SCRIPT.name) {
                validateQosCompletionConsistency(qosRunSummary)
            }
        }

        val sanitizedQos = qosRunSummary.copy(
            scriptId = qosRunSummary.scriptId?.trim()?.takeIf { it.isNotBlank() },
            scriptName = qosRunSummary.scriptName?.trim()?.takeIf { it.isNotBlank() },
            configuredRepeatCount = qosRunSummary.configuredRepeatCount?.coerceAtLeast(1),
            configuredTechnologies = qosRunSummary.configuredTechnologies
                .map { technology -> technology.trim() }
                .filter { technology -> technology.isNotBlank() }
                .toSet(),
            scriptSnapshotUpdatedAtEpochMillis = qosRunSummary.scriptSnapshotUpdatedAtEpochMillis,
            selectedTestFamilies = qosRunSummary.selectedTestFamilies,
            targetTechnology = qosRunSummary.targetTechnology?.trim()?.takeIf { it.isNotBlank() },
            targetPhoneNumber = qosRunSummary.targetPhoneNumber?.trim()?.takeIf { it.isNotBlank() },
            iterationCount = qosRunSummary.iterationCount.coerceAtLeast(0),
            successCount = qosRunSummary.successCount.coerceAtLeast(0),
            failureCount = qosRunSummary.failureCount.coerceAtLeast(0)
        )

        val now = System.currentTimeMillis()
        database.withTransaction {
            sessionDao.updateExecution(
                sessionId = sessionId,
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
                qosScriptId = sanitizedQos.scriptId,
                qosScriptName = sanitizedQos.scriptName,
                qosConfiguredRepeatCount = sanitizedQos.configuredRepeatCount,
                qosConfiguredTechnologiesCsv = sanitizedQos.configuredTechnologies
                    .sorted()
                    .joinToString(","),
                qosScriptSnapshotUpdatedAtEpochMillis = sanitizedQos.scriptSnapshotUpdatedAtEpochMillis,
                qosTestFamiliesCsv = sanitizedQos.selectedTestFamilies
                    .map { family -> family.name }
                    .sorted()
                    .joinToString(","),
                qosTargetTechnology = sanitizedQos.targetTechnology,
                qosTargetPhoneNumber = sanitizedQos.targetPhoneNumber,
                qosIterationCount = sanitizedQos.iterationCount,
                qosSuccessCount = sanitizedQos.successCount,
                qosFailureCount = sanitizedQos.failureCount,
                notes = notes.trim(),
                resultSummary = resultSummary.trim(),
                updatedAtEpochMillis = now,
                completedAtEpochMillis = if (status == PerformanceSessionStatus.COMPLETED) now else null
            )

            qosFamilyResultDao.deleteBySession(sessionId)
            val familyResults = sanitizedQos.familyExecutionResults
                .filter { result -> result.family in sanitizedQos.selectedTestFamilies }
                .map { result -> result.toEntity(sessionId = sessionId, updatedAtEpochMillis = now) }
            if (familyResults.isNotEmpty()) {
                qosFamilyResultDao.upsertAll(familyResults)
            }

            qosTimelineEventDao.deleteBySession(sessionId)
            val timelineEvents = sanitizeTimelineEventsForPersistence(
                qosRunSummary = sanitizedQos,
                fallbackOccurredAtEpochMillis = now
            ).map { event ->
                event.toEntity(sessionId = sessionId)
            }
            if (timelineEvents.isNotEmpty()) {
                qosTimelineEventDao.upsertAll(timelineEvents)
            }
        }
    }

    private fun defaultStepsFor(workflowType: PerformanceWorkflowType): List<PerformanceGuidedStep> {
        return when (workflowType) {
            PerformanceWorkflowType.THROUGHPUT -> {
                listOf(
                    PerformanceGuidedStep(
                        code = PerformanceStepCode.PRECONDITIONS_CHECK,
                        required = true,
                        status = PerformanceStepStatus.TODO
                    ),
                    PerformanceGuidedStep(
                        code = PerformanceStepCode.EXECUTE_TEST,
                        required = true,
                        status = PerformanceStepStatus.TODO
                    ),
                    PerformanceGuidedStep(
                        code = PerformanceStepCode.REVIEW_RESULT,
                        required = true,
                        status = PerformanceStepStatus.TODO
                    )
                )
            }

            PerformanceWorkflowType.QOS_SCRIPT -> {
                listOf(
                    PerformanceGuidedStep(
                        code = PerformanceStepCode.PRECONDITIONS_CHECK,
                        required = true,
                        status = PerformanceStepStatus.TODO
                    ),
                    PerformanceGuidedStep(
                        code = PerformanceStepCode.EXECUTE_TEST,
                        required = true,
                        status = PerformanceStepStatus.TODO
                    ),
                    PerformanceGuidedStep(
                        code = PerformanceStepCode.SEND_RESULT,
                        required = true,
                        status = PerformanceStepStatus.TODO
                    )
                )
            }
        }
    }
}

private fun sanitizeTimelineEventsForPersistence(
    qosRunSummary: QosRunSummary,
    fallbackOccurredAtEpochMillis: Long
): List<QosExecutionTimelineEvent> {
    val selectedFamilies = qosRunSummary.selectedTestFamilies
    val providedEvents = qosRunSummary.executionTimelineEvents
        .filter { event -> event.family in selectedFamilies }
        .map { event ->
            event.copy(
                repetitionIndex = event.repetitionIndex.coerceAtLeast(1),
                occurredAtEpochMillis = event.occurredAtEpochMillis.coerceAtLeast(1L),
                checkpointSequence = event.checkpointSequence.coerceAtLeast(0)
            )
        }

    if (providedEvents.isNotEmpty()) {
        return providedEvents
            .sortedWith(
                compareBy<QosExecutionTimelineEvent> { event -> event.occurredAtEpochMillis }
                    .thenBy { event ->
                        if (event.checkpointSequence > 0) {
                            event.checkpointSequence
                        } else {
                            Int.MAX_VALUE
                        }
                    }
                    .thenBy { event -> event.family.name }
                    .thenBy { event -> event.repetitionIndex }
                    .thenBy { event -> qosExecutionEventSortOrder(event.eventType) }
            )
            .mapIndexed { index, event ->
                event.copy(checkpointSequence = index + 1)
            }
    }

    // Backward-compatible fallback for sessions persisted before explicit runner timeline support.
    return qosRunSummary.selectedTestFamilies
        .sortedBy { family -> family.name }
        .flatMap { family ->
            val result = qosRunSummary.familyExecutionResults.firstOrNull { it.family == family }
            val status = result?.status ?: QosFamilyExecutionStatus.NOT_RUN
            if (status == QosFamilyExecutionStatus.NOT_RUN) {
                emptyList()
            } else {
                val terminalType = when (status) {
                    QosFamilyExecutionStatus.PASSED -> QosExecutionEventType.PASSED
                    QosFamilyExecutionStatus.FAILED -> QosExecutionEventType.FAILED
                    QosFamilyExecutionStatus.BLOCKED -> QosExecutionEventType.BLOCKED
                    QosFamilyExecutionStatus.NOT_RUN -> null
                } ?: return@flatMap emptyList()

                listOf(
                    QosExecutionTimelineEvent(
                        family = family,
                        repetitionIndex = 1,
                        eventType = QosExecutionEventType.STARTED,
                        occurredAtEpochMillis = fallbackOccurredAtEpochMillis
                    ),
                    QosExecutionTimelineEvent(
                        family = family,
                        repetitionIndex = 1,
                        eventType = terminalType,
                        reasonCode = result?.failureReasonCode,
                        reason = result?.failureReason,
                        occurredAtEpochMillis = fallbackOccurredAtEpochMillis
                    )
                )
            }
        }
        .mapIndexed { index, event ->
            event.copy(checkpointSequence = index + 1)
        }
}

internal fun validateQosCompletionConsistency(qosRunSummary: QosRunSummary) {
    val assessment = assessQosCompletion(qosRunSummary)
    if (assessment.canComplete) return

    val firstIssue = assessment.issues.first()
    val message = when (firstIssue) {
        QosCompletionIssue.SCRIPT_REFERENCE_MISSING ->
            "QoS script reference is missing before completion."
        QosCompletionIssue.TEST_FAMILIES_MISSING ->
            "At least one QoS family must be selected before closing the session."
        QosCompletionIssue.FAMILY_RESULT_INCOMPLETE ->
            "All selected QoS families must be marked PASSED or FAILED before completion."
        QosCompletionIssue.REPETITION_COVERAGE_INCOMPLETE ->
            "Each selected QoS family must cover the configured repeat count with PASS/FAIL results."
        QosCompletionIssue.FAILURE_REASON_CODE_MISSING ->
            "Failed or blocked QoS families must provide a typed failure reason before completion."
        QosCompletionIssue.PHONE_TARGET_MISSING ->
            "A target phone number is required for selected QoS call/SMS families."
        QosCompletionIssue.TARGET_TECHNOLOGY_INVALID ->
            "Selected target technology is not part of the configured QoS script technologies."
        QosCompletionIssue.COUNTERS_INCONSISTENT ->
            "QoS aggregate counters are inconsistent with family execution results."
    }
    throw IllegalStateException(message)
}

internal fun buildPerformanceCompletionGuard(
    steps: List<PerformanceStepEntity>
): WorkflowCompletionGuard {
    return WorkflowCompletionGuard.fromRequiredStatuses(
        stepStatuses = steps.map { step ->
            step.required to runCatching {
                PerformanceStepStatus.valueOf(step.status)
            }.getOrDefault(PerformanceStepStatus.TODO)
        }
    )
}
