package com.quartz.platform.data.repository

import androidx.room.withTransaction
import com.quartz.platform.data.local.QuartzDatabase
import com.quartz.platform.data.local.dao.PerformanceQosFamilyResultDao
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
import com.quartz.platform.domain.model.QosFamilyExecutionStatus
import com.quartz.platform.domain.model.QosRunSummary
import com.quartz.platform.domain.model.QosTestFamily
import com.quartz.platform.domain.model.ThroughputMetrics
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
                        qosFamilyResultDao.observeBySessionIds(sessionIds)
                    ) { steps, familyResults ->
                        val stepsBySession = steps.groupBy { step -> step.sessionId }
                        val familyResultsBySession = familyResults.groupBy { result -> result.sessionId }
                        sessions.map { session ->
                            session.toDomain(
                                steps = stepsBySession[session.id].orEmpty(),
                                familyResults = familyResultsBySession[session.id].orEmpty()
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
                        qosFamilyResultDao.observeBySession(session.id)
                    ) { steps, familyResults ->
                        session.toDomain(steps = steps, familyResults = familyResults)
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

internal fun validateQosCompletionConsistency(qosRunSummary: QosRunSummary) {
    if (qosRunSummary.selectedTestFamilies.isEmpty()) {
        throw IllegalStateException("At least one QoS family must be selected before closing the session.")
    }

    val byFamily = qosRunSummary.familyExecutionResults.associateBy { result -> result.family }
    val selectedResults = qosRunSummary.selectedTestFamilies.map { family ->
        family to byFamily[family]
    }
    val missing = selectedResults.any { (_, result) ->
        val status = result?.status ?: QosFamilyExecutionStatus.NOT_RUN
        status != QosFamilyExecutionStatus.PASSED && status != QosFamilyExecutionStatus.FAILED
    }
    if (missing) {
        throw IllegalStateException("All selected QoS families must be marked PASSED or FAILED before completion.")
    }

    val missingFailureReason = selectedResults.any { (_, result) ->
        result?.status == QosFamilyExecutionStatus.FAILED && result.failureReason.isNullOrBlank()
    }
    if (missingFailureReason) {
        throw IllegalStateException("Failed QoS families must provide a failure reason before completion.")
    }

    val requiresPhoneTarget = qosRunSummary.selectedTestFamilies.any { family ->
        family in PHONE_REQUIRED_QOS_FAMILIES
    }
    if (requiresPhoneTarget && qosRunSummary.targetPhoneNumber.isNullOrBlank()) {
        throw IllegalStateException("A target phone number is required for selected QoS call/SMS families.")
    }

    if (qosRunSummary.configuredTechnologies.isNotEmpty()) {
        val targetTechnology = qosRunSummary.targetTechnology
            ?: throw IllegalStateException("Select a target technology before completing this QoS script session.")
        if (targetTechnology !in qosRunSummary.configuredTechnologies) {
            throw IllegalStateException("Selected target technology is not part of the configured QoS script technologies.")
        }
    }

    val passedCount = selectedResults.count { (_, result) ->
        result?.status == QosFamilyExecutionStatus.PASSED
    }
    val failedCount = selectedResults.count { (_, result) ->
        result?.status == QosFamilyExecutionStatus.FAILED
    }
    val completedCount = passedCount + failedCount

    if (qosRunSummary.successCount != passedCount ||
        qosRunSummary.failureCount != failedCount ||
        qosRunSummary.iterationCount != completedCount
    ) {
        throw IllegalStateException("QoS aggregate counters are inconsistent with family execution results.")
    }
}

private val PHONE_REQUIRED_QOS_FAMILIES: Set<QosTestFamily> = setOf(
    QosTestFamily.SMS,
    QosTestFamily.VOLTE_CALL,
    QosTestFamily.CSFB_CALL,
    QosTestFamily.EMERGENCY_CALL,
    QosTestFamily.STANDARD_CALL
)

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
