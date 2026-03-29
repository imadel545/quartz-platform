package com.quartz.platform.data.repository

import androidx.room.withTransaction
import com.quartz.platform.data.local.QuartzDatabase
import com.quartz.platform.data.local.dao.RetSessionDao
import com.quartz.platform.data.local.dao.RetStepDao
import com.quartz.platform.data.local.entity.RetStepEntity
import com.quartz.platform.data.local.mapper.toDomain
import com.quartz.platform.data.local.mapper.toEntity
import com.quartz.platform.domain.model.RetGuidedSession
import com.quartz.platform.domain.model.RetGuidedStep
import com.quartz.platform.domain.model.RetReferenceAltitudeSourceState
import com.quartz.platform.domain.model.RetResultOutcome
import com.quartz.platform.domain.model.RetSessionStatus
import com.quartz.platform.domain.model.RetStepCode
import com.quartz.platform.domain.model.RetStepStatus
import com.quartz.platform.domain.model.workflow.WorkflowGeospatialPolicy
import com.quartz.platform.domain.model.workflow.WorkflowCompletionGuard
import com.quartz.platform.domain.repository.RetGuidedSessionRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstRetGuidedSessionRepository @Inject constructor(
    private val sessionDao: RetSessionDao,
    private val stepDao: RetStepDao,
    private val database: QuartzDatabase
) : RetGuidedSessionRepository {

    override fun observeSectorSessionHistory(siteId: String, sectorId: String): Flow<List<RetGuidedSession>> {
        return sessionDao.observeHistoryBySector(siteId = siteId, sectorId = sectorId)
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

    override fun observeLatestSectorSession(siteId: String, sectorId: String): Flow<RetGuidedSession?> {
        return observeSectorSessionHistory(siteId = siteId, sectorId = sectorId)
            .map { history -> history.firstOrNull() }
    }

    override suspend fun createSession(
        siteId: String,
        sectorId: String,
        sectorCode: String
    ): RetGuidedSession {
        val now = System.currentTimeMillis()
        val session = RetGuidedSession(
            id = UUID.randomUUID().toString(),
            siteId = siteId,
            sectorId = sectorId,
            sectorCode = sectorCode,
            measurementZoneRadiusMeters = WorkflowGeospatialPolicy.DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS,
            measurementZoneExtensionReason = "",
            proximityModeEnabled = false,
            proximityReferenceAltitudeMeters = null,
            proximityReferenceAltitudeSource = RetReferenceAltitudeSourceState.UNAVAILABLE,
            status = RetSessionStatus.CREATED,
            resultOutcome = RetResultOutcome.NOT_RUN,
            notes = "",
            resultSummary = "",
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            completedAtEpochMillis = null,
            steps = defaultRetSteps()
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
        stepCode: RetStepCode,
        status: RetStepStatus
    ) {
        database.withTransaction {
            stepDao.updateStatus(
                sessionId = sessionId,
                code = stepCode.name,
                status = status.name
            )

            val existing = sessionDao.getById(sessionId) ?: return@withTransaction
            val nextStatus = if (existing.status == RetSessionStatus.CREATED.name && status != RetStepStatus.TODO) {
                RetSessionStatus.IN_PROGRESS
            } else {
                RetSessionStatus.valueOf(existing.status)
            }
            val completedAt = if (nextStatus == RetSessionStatus.COMPLETED) {
                existing.completedAtEpochMillis
            } else {
                null
            }

            sessionDao.updateSummary(
                sessionId = sessionId,
                status = nextStatus.name,
                resultOutcome = existing.resultOutcome,
                notes = existing.notes,
                resultSummary = existing.resultSummary,
                updatedAtEpochMillis = System.currentTimeMillis(),
                completedAtEpochMillis = completedAt
            )
        }
    }

    override suspend fun updateSessionSummary(
        sessionId: String,
        status: RetSessionStatus,
        resultOutcome: RetResultOutcome,
        notes: String,
        resultSummary: String
    ) {
        if (status == RetSessionStatus.COMPLETED) {
            val steps = stepDao.listBySession(sessionId)
            if (!buildRetCompletionGuard(steps).canComplete) {
                throw IllegalStateException("Required RET steps must be completed before closing the session.")
            }
        }

        val now = System.currentTimeMillis()
        sessionDao.updateSummary(
            sessionId = sessionId,
            status = status.name,
            resultOutcome = resultOutcome.name,
            notes = notes.trim(),
            resultSummary = resultSummary.trim(),
            updatedAtEpochMillis = now,
            completedAtEpochMillis = if (status == RetSessionStatus.COMPLETED) now else null
        )
    }

    override suspend fun updateSessionGeospatialContext(
        sessionId: String,
        measurementZoneRadiusMeters: Int,
        measurementZoneExtensionReason: String,
        proximityModeEnabled: Boolean,
        proximityReferenceAltitudeMeters: Double?,
        proximityReferenceAltitudeSource: RetReferenceAltitudeSourceState
    ) {
        sessionDao.updateGeospatialContext(
            sessionId = sessionId,
            measurementZoneRadiusMeters = WorkflowGeospatialPolicy.clampMeasurementZoneRadius(
                measurementZoneRadiusMeters
            ),
            measurementZoneExtensionReason = measurementZoneExtensionReason.trim(),
            proximityModeEnabled = proximityModeEnabled,
            proximityReferenceAltitudeMeters = proximityReferenceAltitudeMeters,
            proximityReferenceAltitudeSource = proximityReferenceAltitudeSource.name,
            updatedAtEpochMillis = System.currentTimeMillis()
        )
    }

    private fun defaultRetSteps(): List<RetGuidedStep> {
        return listOf(
            RetGuidedStep(
                code = RetStepCode.CALIBRATION_PRECHECK,
                required = true,
                status = RetStepStatus.TODO
            ),
            RetGuidedStep(
                code = RetStepCode.VALIDATION_CAPTURE,
                required = true,
                status = RetStepStatus.TODO
            ),
            RetGuidedStep(
                code = RetStepCode.RESTORE_TILT_AND_RESULT,
                required = true,
                status = RetStepStatus.TODO
            )
        )
    }
}

internal fun buildRetCompletionGuard(steps: List<RetStepEntity>): WorkflowCompletionGuard {
    return WorkflowCompletionGuard.fromRequiredStatuses(
        stepStatuses = steps.map { step ->
            step.required to RetStepStatus.valueOf(step.status)
        }
    )
}
