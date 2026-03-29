package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.repository.XfeederGuidedSessionRepository
import com.quartz.platform.domain.model.XfeederReferenceAltitudeSourceState
import javax.inject.Inject

class UpdateXfeederSessionGeospatialContextUseCase @Inject constructor(
    private val repository: XfeederGuidedSessionRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        measurementZoneRadiusMeters: Int,
        measurementZoneExtensionReason: String,
        proximityModeEnabled: Boolean,
        proximityReferenceAltitudeMeters: Double?,
        proximityReferenceAltitudeSource: XfeederReferenceAltitudeSourceState
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
