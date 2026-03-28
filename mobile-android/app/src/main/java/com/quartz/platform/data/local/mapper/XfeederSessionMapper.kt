package com.quartz.platform.data.local.mapper

import com.quartz.platform.data.local.entity.XfeederSessionEntity
import com.quartz.platform.data.local.entity.XfeederStepEntity
import com.quartz.platform.domain.model.GuidedSessionClosureProjection
import com.quartz.platform.domain.model.XfeederClosureEvidence
import com.quartz.platform.domain.model.XfeederGuidedSession
import com.quartz.platform.domain.model.XfeederGuidedStep
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederSessionStatus
import com.quartz.platform.domain.model.XfeederStepCode
import com.quartz.platform.domain.model.XfeederStepStatus
import com.quartz.platform.domain.model.XfeederUnreliableReason

fun XfeederSessionEntity.toDomain(steps: List<XfeederStepEntity>): XfeederGuidedSession {
    return XfeederGuidedSession(
        id = id,
        siteId = siteId,
        sectorId = sectorId,
        sectorCode = sectorCode,
        measurementZoneRadiusMeters = measurementZoneRadiusMeters,
        measurementZoneExtensionReason = measurementZoneExtensionReason,
        proximityModeEnabled = proximityModeEnabled,
        status = XfeederSessionStatus.valueOf(status),
        sectorOutcome = XfeederSectorOutcome.valueOf(sectorOutcome),
        closureEvidence = XfeederClosureEvidence(
            relatedSectorCode = closureRelatedSectorCode,
            unreliableReason = closureUnreliableReason?.let(XfeederUnreliableReason::valueOf),
            observedSectorCount = closureObservedSectorCount
        ),
        notes = notes,
        resultSummary = resultSummary,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        completedAtEpochMillis = completedAtEpochMillis,
        steps = steps.map { step ->
            XfeederGuidedStep(
                code = XfeederStepCode.valueOf(step.code),
                required = step.required,
                status = XfeederStepStatus.valueOf(step.status)
            )
        }
    )
}

fun XfeederGuidedSession.toEntity(): XfeederSessionEntity {
    return XfeederSessionEntity(
        id = id,
        siteId = siteId,
        sectorId = sectorId,
        sectorCode = sectorCode,
        measurementZoneRadiusMeters = measurementZoneRadiusMeters,
        measurementZoneExtensionReason = measurementZoneExtensionReason,
        proximityModeEnabled = proximityModeEnabled,
        status = status.name,
        sectorOutcome = sectorOutcome.name,
        closureRelatedSectorCode = closureEvidence.relatedSectorCode,
        closureUnreliableReason = closureEvidence.unreliableReason?.name,
        closureObservedSectorCount = closureEvidence.observedSectorCount,
        notes = notes,
        resultSummary = resultSummary,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        completedAtEpochMillis = completedAtEpochMillis
    )
}

fun XfeederGuidedStep.toEntity(sessionId: String, displayOrder: Int): XfeederStepEntity {
    return XfeederStepEntity(
        sessionId = sessionId,
        code = code.name,
        required = required,
        status = status.name,
        displayOrder = displayOrder
    )
}

fun XfeederSessionEntity.toClosureProjection(): GuidedSessionClosureProjection {
    val relatedSectorCodeValue = closureRelatedSectorCode.trim().takeIf { it.isNotBlank() }
    return GuidedSessionClosureProjection(
        sessionId = id,
        siteId = siteId,
        sectorId = sectorId,
        sectorCode = sectorCode,
        sectorOutcome = XfeederSectorOutcome.valueOf(sectorOutcome),
        relatedSectorCode = relatedSectorCodeValue,
        unreliableReason = closureUnreliableReason?.let(XfeederUnreliableReason::valueOf),
        observedSectorCount = closureObservedSectorCount,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}
