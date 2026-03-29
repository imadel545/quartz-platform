package com.quartz.platform.presentation.ret.session

import com.quartz.platform.domain.model.GeoCoordinate
import com.quartz.platform.domain.model.RetProximityEligibilityState
import com.quartz.platform.domain.model.RetReferenceAltitudeSourceState
import com.quartz.platform.domain.model.RetGuidedSession
import com.quartz.platform.domain.model.RetGeospatialPolicy
import com.quartz.platform.domain.model.RetResultOutcome
import com.quartz.platform.domain.model.RetSessionStatus
import com.quartz.platform.domain.model.UserLocation

data class RetGuidedSessionUiState(
    val isLoading: Boolean = true,
    val siteId: String = "",
    val siteLabel: String = "",
    val sectorId: String = "",
    val sectorCode: String = "",
    val session: RetGuidedSession? = null,
    val sessionHistory: List<RetGuidedSession> = emptyList(),
    val latestSessionId: String? = null,
    val selectedStatus: RetSessionStatus = RetSessionStatus.CREATED,
    val selectedOutcome: RetResultOutcome = RetResultOutcome.NOT_RUN,
    val siteCoordinate: GeoCoordinate? = null,
    val measurementZoneCoordinate: GeoCoordinate? = null,
    val measurementZoneRadiusMeters: Int = RetGeospatialPolicy.DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS,
    val measurementZoneExtensionReasonInput: String = "",
    val proximityReferenceAltitudeInput: String = "",
    val technicalReferenceAltitudeMeters: Double? = null,
    val effectiveReferenceAltitudeMeters: Double? = null,
    val proximityReferenceAltitudeSource: RetReferenceAltitudeSourceState =
        RetReferenceAltitudeSourceState.UNAVAILABLE,
    val proximityModeEnabled: Boolean = false,
    val userLocation: UserLocation? = null,
    val userAltitudeMeters: Double? = null,
    val userAltitudeVerticalAccuracyMeters: Float? = null,
    val distanceToMeasurementZoneMeters: Int? = null,
    val isInsideMeasurementZone: Boolean? = null,
    val proximityEligibilityState: RetProximityEligibilityState = RetProximityEligibilityState.UNAVAILABLE,
    val notesInput: String = "",
    val resultSummaryInput: String = "",
    val completionGuardMessage: String? = null,
    val hasUnsavedChanges: Boolean = false,
    val isCreatingSession: Boolean = false,
    val isCreatingDraft: Boolean = false,
    val isSavingSummary: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)
