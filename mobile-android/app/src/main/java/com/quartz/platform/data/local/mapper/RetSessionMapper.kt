package com.quartz.platform.data.local.mapper

import com.quartz.platform.data.local.entity.RetSessionEntity
import com.quartz.platform.data.local.entity.RetStepEntity
import com.quartz.platform.domain.model.RetGuidedSession
import com.quartz.platform.domain.model.RetGuidedStep
import com.quartz.platform.domain.model.RetReferenceAltitudeSourceState
import com.quartz.platform.domain.model.RetResultOutcome
import com.quartz.platform.domain.model.RetSessionStatus
import com.quartz.platform.domain.model.RetStepCode
import com.quartz.platform.domain.model.RetStepStatus
import com.quartz.platform.domain.model.RetClosureProjection

fun RetSessionEntity.toDomain(steps: List<RetStepEntity>): RetGuidedSession {
    return RetGuidedSession(
        id = id,
        siteId = siteId,
        sectorId = sectorId,
        sectorCode = sectorCode,
        measurementZoneRadiusMeters = measurementZoneRadiusMeters,
        measurementZoneExtensionReason = measurementZoneExtensionReason,
        proximityModeEnabled = proximityModeEnabled,
        proximityReferenceAltitudeMeters = proximityReferenceAltitudeMeters,
        proximityReferenceAltitudeSource = runCatching {
            RetReferenceAltitudeSourceState.valueOf(proximityReferenceAltitudeSource)
        }.getOrDefault(RetReferenceAltitudeSourceState.UNAVAILABLE),
        status = RetSessionStatus.valueOf(status),
        resultOutcome = RetResultOutcome.valueOf(resultOutcome),
        notes = notes,
        resultSummary = resultSummary,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        completedAtEpochMillis = completedAtEpochMillis,
        steps = steps.map { step ->
            RetGuidedStep(
                code = RetStepCode.valueOf(step.code),
                required = step.required,
                status = RetStepStatus.valueOf(step.status)
            )
        }
    )
}

fun RetGuidedSession.toEntity(): RetSessionEntity {
    return RetSessionEntity(
        id = id,
        siteId = siteId,
        sectorId = sectorId,
        sectorCode = sectorCode,
        measurementZoneRadiusMeters = measurementZoneRadiusMeters,
        measurementZoneExtensionReason = measurementZoneExtensionReason,
        proximityModeEnabled = proximityModeEnabled,
        proximityReferenceAltitudeMeters = proximityReferenceAltitudeMeters,
        proximityReferenceAltitudeSource = proximityReferenceAltitudeSource.name,
        status = status.name,
        resultOutcome = resultOutcome.name,
        notes = notes,
        resultSummary = resultSummary,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        completedAtEpochMillis = completedAtEpochMillis
    )
}

fun RetGuidedStep.toEntity(sessionId: String, displayOrder: Int): RetStepEntity {
    return RetStepEntity(
        sessionId = sessionId,
        code = code.name,
        required = required,
        status = status.name,
        displayOrder = displayOrder
    )
}

fun RetSessionEntity.toClosureProjection(steps: List<RetStepEntity>): RetClosureProjection {
    val requiredStepCount = steps.count { step -> step.required }
    val completedRequiredStepCount = steps.count { step ->
        step.required && runCatching {
            RetStepStatus.valueOf(step.status)
        }.getOrDefault(RetStepStatus.TODO) == RetStepStatus.DONE
    }
    return RetClosureProjection(
        sessionId = id,
        siteId = siteId,
        sectorId = sectorId,
        sectorCode = sectorCode,
        sessionStatus = runCatching {
            RetSessionStatus.valueOf(status)
        }.getOrDefault(RetSessionStatus.CREATED),
        resultOutcome = runCatching {
            RetResultOutcome.valueOf(resultOutcome)
        }.getOrDefault(RetResultOutcome.NOT_RUN),
        requiredStepCount = requiredStepCount,
        completedRequiredStepCount = completedRequiredStepCount,
        measurementZoneRadiusMeters = measurementZoneRadiusMeters,
        proximityModeEnabled = proximityModeEnabled,
        resultSummary = resultSummary.trim().takeIf { it.isNotBlank() },
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}
