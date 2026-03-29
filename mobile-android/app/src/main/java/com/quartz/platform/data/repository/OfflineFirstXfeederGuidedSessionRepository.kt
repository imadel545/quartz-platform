package com.quartz.platform.data.repository

import androidx.room.withTransaction
import com.quartz.platform.data.local.QuartzDatabase
import com.quartz.platform.data.local.dao.XfeederSessionDao
import com.quartz.platform.data.local.dao.XfeederStepDao
import com.quartz.platform.data.local.entity.XfeederSessionEntity
import com.quartz.platform.data.local.entity.XfeederStepEntity
import com.quartz.platform.data.local.mapper.toClosureProjection
import com.quartz.platform.data.local.mapper.toDomain
import com.quartz.platform.data.local.mapper.toEntity
import com.quartz.platform.domain.model.GuidedSessionClosureProjection
import com.quartz.platform.domain.model.XfeederClosureEvidence
import com.quartz.platform.domain.model.XfeederGeospatialPolicy
import com.quartz.platform.domain.model.XfeederGuidedSession
import com.quartz.platform.domain.model.XfeederGuidedStep
import com.quartz.platform.domain.model.XfeederReferenceAltitudeSourceState
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederSessionStatus
import com.quartz.platform.domain.model.XfeederStepCode
import com.quartz.platform.domain.model.XfeederStepStatus
import com.quartz.platform.domain.model.validateClosureEvidenceForFinalization
import com.quartz.platform.domain.model.workflow.WorkflowCompletionGuard
import com.quartz.platform.domain.repository.XfeederGuidedSessionRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstXfeederGuidedSessionRepository @Inject constructor(
    private val sessionDao: XfeederSessionDao,
    private val stepDao: XfeederStepDao,
    private val database: QuartzDatabase
) : XfeederGuidedSessionRepository {

    override fun observeSectorSessionHistory(siteId: String, sectorId: String): Flow<List<XfeederGuidedSession>> {
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

    override fun observeLatestSectorSession(siteId: String, sectorId: String): Flow<XfeederGuidedSession?> {
        return observeSectorSessionHistory(siteId = siteId, sectorId = sectorId)
            .map { history -> history.firstOrNull() }
    }

    override fun observeSiteClosureProjections(siteId: String): Flow<List<GuidedSessionClosureProjection>> {
        return sessionDao.observeBySite(siteId)
            .map(::toSiteClosureProjections)
    }

    override suspend fun createSession(
        siteId: String,
        sectorId: String,
        sectorCode: String
    ): XfeederGuidedSession {
        val now = System.currentTimeMillis()
        val session = XfeederGuidedSession(
            id = UUID.randomUUID().toString(),
            siteId = siteId,
            sectorId = sectorId,
            sectorCode = sectorCode,
            measurementZoneRadiusMeters = XfeederGeospatialPolicy.DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS,
            measurementZoneExtensionReason = "",
            proximityModeEnabled = false,
            proximityReferenceAltitudeMeters = null,
            proximityReferenceAltitudeSource = XfeederReferenceAltitudeSourceState.UNAVAILABLE,
            status = XfeederSessionStatus.CREATED,
            sectorOutcome = XfeederSectorOutcome.NOT_TESTED,
            closureEvidence = XfeederClosureEvidence(
                relatedSectorCode = "",
                unreliableReason = null,
                observedSectorCount = null
            ),
            notes = "",
            resultSummary = "",
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            completedAtEpochMillis = null,
            steps = defaultGuidedSteps()
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
        stepCode: XfeederStepCode,
        status: XfeederStepStatus
    ) {
        database.withTransaction {
            stepDao.updateStatus(
                sessionId = sessionId,
                code = stepCode.name,
                status = status.name
            )

            val existing = sessionDao.getById(sessionId) ?: return@withTransaction
            val nextStatus = if (existing.status == XfeederSessionStatus.CREATED.name && status != XfeederStepStatus.TODO) {
                XfeederSessionStatus.IN_PROGRESS
            } else {
                XfeederSessionStatus.valueOf(existing.status)
            }
            val completedAt = if (nextStatus == XfeederSessionStatus.COMPLETED) {
                existing.completedAtEpochMillis
            } else {
                null
            }

            sessionDao.updateSummary(
                sessionId = sessionId,
                status = nextStatus.name,
                sectorOutcome = existing.sectorOutcome,
                closureRelatedSectorCode = existing.closureRelatedSectorCode,
                closureUnreliableReason = existing.closureUnreliableReason,
                closureObservedSectorCount = existing.closureObservedSectorCount,
                notes = existing.notes,
                resultSummary = existing.resultSummary,
                updatedAtEpochMillis = System.currentTimeMillis(),
                completedAtEpochMillis = completedAt
            )
        }
    }

    override suspend fun updateSessionSummary(
        sessionId: String,
        status: XfeederSessionStatus,
        sectorOutcome: XfeederSectorOutcome,
        closureEvidence: XfeederClosureEvidence,
        notes: String,
        resultSummary: String
    ) {
        if (status == XfeederSessionStatus.COMPLETED) {
            val steps = stepDao.listBySession(sessionId)
            if (hasIncompleteRequiredSteps(steps)) {
                throw IllegalStateException("Required checklist steps must be completed before closing the session.")
            }
            val evidenceIssue = validateClosureEvidenceForFinalization(
                outcome = sectorOutcome,
                evidence = closureEvidence
            )
            if (evidenceIssue != null) {
                throw IllegalStateException("Closure evidence invalid for $sectorOutcome: $evidenceIssue")
            }
        }

        val now = System.currentTimeMillis()
        val completedAt = if (status == XfeederSessionStatus.COMPLETED) now else null
        val sanitizedEvidence = sanitizeClosureEvidenceForOutcome(
            outcome = sectorOutcome,
            evidence = closureEvidence
        )
        sessionDao.updateSummary(
            sessionId = sessionId,
            status = status.name,
            sectorOutcome = sectorOutcome.name,
            closureRelatedSectorCode = sanitizedEvidence.relatedSectorCode,
            closureUnreliableReason = sanitizedEvidence.unreliableReason?.name,
            closureObservedSectorCount = sanitizedEvidence.observedSectorCount,
            notes = notes,
            resultSummary = resultSummary,
            updatedAtEpochMillis = now,
            completedAtEpochMillis = completedAt
        )
    }

    override suspend fun updateSessionGeospatialContext(
        sessionId: String,
        measurementZoneRadiusMeters: Int,
        measurementZoneExtensionReason: String,
        proximityModeEnabled: Boolean,
        proximityReferenceAltitudeMeters: Double?,
        proximityReferenceAltitudeSource: XfeederReferenceAltitudeSourceState
    ) {
        sessionDao.updateGeospatialContext(
            sessionId = sessionId,
            measurementZoneRadiusMeters = XfeederGeospatialPolicy.clampMeasurementZoneRadius(
                measurementZoneRadiusMeters
            ),
            measurementZoneExtensionReason = measurementZoneExtensionReason.trim(),
            proximityModeEnabled = proximityModeEnabled,
            proximityReferenceAltitudeMeters = proximityReferenceAltitudeMeters,
            proximityReferenceAltitudeSource = proximityReferenceAltitudeSource.name,
            updatedAtEpochMillis = System.currentTimeMillis()
        )
    }

    private fun defaultGuidedSteps(): List<XfeederGuidedStep> {
        return listOf(
            XfeederGuidedStep(
                code = XfeederStepCode.PRECONDITION_NETWORK_READY,
                required = true,
                status = XfeederStepStatus.TODO
            ),
            XfeederGuidedStep(
                code = XfeederStepCode.PRECONDITION_MEASUREMENT_ZONE_READY,
                required = true,
                status = XfeederStepStatus.TODO
            ),
            XfeederGuidedStep(
                code = XfeederStepCode.OBSERVE_CONNECTED_CELLS,
                required = true,
                status = XfeederStepStatus.TODO
            ),
            XfeederGuidedStep(
                code = XfeederStepCode.CHECK_SECTOR_CROSSING,
                required = true,
                status = XfeederStepStatus.TODO
            ),
            XfeederGuidedStep(
                code = XfeederStepCode.CHECK_MIXFEEDER_ALTERNANCE,
                required = true,
                status = XfeederStepStatus.TODO
            ),
            XfeederGuidedStep(
                code = XfeederStepCode.FINALIZE_SECTOR_SUMMARY,
                required = false,
                status = XfeederStepStatus.TODO
            )
        )
    }
}

internal fun hasIncompleteRequiredSteps(
    steps: List<XfeederStepEntity>
): Boolean {
    return !buildCompletionGuard(steps).canComplete
}

internal fun buildCompletionGuard(steps: List<XfeederStepEntity>): WorkflowCompletionGuard {
    return WorkflowCompletionGuard.fromRequiredStatuses(
        stepStatuses = steps.map { step ->
            step.required to XfeederStepStatus.valueOf(step.status)
        }
    )
}

private fun sanitizeClosureEvidenceForOutcome(
    outcome: XfeederSectorOutcome,
    evidence: XfeederClosureEvidence
): XfeederClosureEvidence {
    return when (outcome) {
        XfeederSectorOutcome.CROSSED,
        XfeederSectorOutcome.MIXFEEDER -> {
            evidence.copy(
                relatedSectorCode = evidence.relatedSectorCode.trim(),
                unreliableReason = null,
                observedSectorCount = null
            )
        }

        XfeederSectorOutcome.UNRELIABLE -> {
            evidence.copy(
                relatedSectorCode = "",
                observedSectorCount = evidence.observedSectorCount
            )
        }

        else -> {
            XfeederClosureEvidence(
                relatedSectorCode = "",
                unreliableReason = null,
                observedSectorCount = null
            )
        }
    }
}

internal fun toSiteClosureProjections(
    sessions: List<XfeederSessionEntity>
): List<GuidedSessionClosureProjection> {
    return sessions
        .asSequence()
        .filter { it.status == XfeederSessionStatus.COMPLETED.name }
        .groupBy { it.sectorId }
        .values
        .mapNotNull { sectorSessions ->
            sectorSessions.maxByOrNull { session -> session.updatedAtEpochMillis }
        }
        .map { entity -> entity.toClosureProjection() }
        .sortedBy { projection -> projection.sectorCode }
}
