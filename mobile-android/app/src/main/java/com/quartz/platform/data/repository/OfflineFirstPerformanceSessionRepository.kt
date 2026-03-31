package com.quartz.platform.data.repository

import androidx.room.withTransaction
import com.quartz.platform.data.local.QuartzDatabase
import com.quartz.platform.data.local.dao.PerformanceSessionDao
import com.quartz.platform.data.local.dao.PerformanceStepDao
import com.quartz.platform.data.local.entity.PerformanceStepEntity
import com.quartz.platform.data.local.mapper.toDomain
import com.quartz.platform.data.local.mapper.toEntity
import com.quartz.platform.domain.model.PerformanceGuidedStep
import com.quartz.platform.domain.model.PerformanceSession
import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.PerformanceStepCode
import com.quartz.platform.domain.model.PerformanceStepStatus
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.model.QosRunSummary
import com.quartz.platform.domain.model.ThroughputMetrics
import com.quartz.platform.domain.model.workflow.WorkflowCompletionGuard
import com.quartz.platform.domain.repository.PerformanceSessionRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstPerformanceSessionRepository @Inject constructor(
    private val sessionDao: PerformanceSessionDao,
    private val stepDao: PerformanceStepDao,
    private val database: QuartzDatabase
) : PerformanceSessionRepository {

    override fun observeSiteSessionHistory(siteId: String): Flow<List<PerformanceSession>> {
        return sessionDao.observeHistoryBySite(siteId)
            .flatMapLatest { sessions ->
                if (sessions.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val sessionIds = sessions.map { it.id }
                    stepDao.observeBySessionIds(sessionIds).map { steps ->
                        val stepsBySession = steps.groupBy { it.sessionId }
                        sessions.map { session ->
                            session.toDomain(stepsBySession[session.id].orEmpty())
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
                    stepDao.observeBySession(session.id).map { steps -> session.toDomain(steps) }
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
            val steps = stepDao.listBySession(sessionId)
            if (!buildPerformanceCompletionGuard(steps).canComplete) {
                throw IllegalStateException("Required Débit/QoS steps must be completed before closing the session.")
            }
        }

        val sanitizedQos = qosRunSummary.copy(
            scriptId = qosRunSummary.scriptId?.trim()?.takeIf { it.isNotBlank() },
            scriptName = qosRunSummary.scriptName?.trim()?.takeIf { it.isNotBlank() },
            configuredRepeatCount = qosRunSummary.configuredRepeatCount?.coerceAtLeast(1),
            selectedTestFamilies = qosRunSummary.selectedTestFamilies,
            targetTechnology = qosRunSummary.targetTechnology?.trim()?.takeIf { it.isNotBlank() },
            targetPhoneNumber = qosRunSummary.targetPhoneNumber?.trim()?.takeIf { it.isNotBlank() },
            iterationCount = qosRunSummary.iterationCount.coerceAtLeast(0),
            successCount = qosRunSummary.successCount.coerceAtLeast(0),
            failureCount = qosRunSummary.failureCount.coerceAtLeast(0)
        )

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
            updatedAtEpochMillis = System.currentTimeMillis(),
            completedAtEpochMillis = if (status == PerformanceSessionStatus.COMPLETED) {
                System.currentTimeMillis()
            } else {
                null
            }
        )
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
