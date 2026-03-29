package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.RetReferenceAltitudeSourceState
import com.quartz.platform.domain.repository.RetGuidedSessionRepository
import javax.inject.Inject

class UpdateRetSessionGeospatialContextUseCase @Inject constructor(
    private val repository: RetGuidedSessionRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        measurementZoneRadiusMeters: Int,
        measurementZoneExtensionReason: String,
        proximityModeEnabled: Boolean,
        proximityReferenceAltitudeMeters: Double?,
        proximityReferenceAltitudeSource: RetReferenceAltitudeSourceState
    ) {
        repository.updateSessionGeospatialContext(
            sessionId = sessionId,
            measurementZoneRadiusMeters = measurementZoneRadiusMeters,
            measurementZoneExtensionReason = measurementZoneExtensionReason,
            proximityModeEnabled = proximityModeEnabled,
            proximityReferenceAltitudeMeters = proximityReferenceAltitudeMeters,
            proximityReferenceAltitudeSource = proximityReferenceAltitudeSource
        )
    }
}
