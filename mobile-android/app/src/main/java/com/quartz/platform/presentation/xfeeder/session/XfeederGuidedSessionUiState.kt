package com.quartz.platform.presentation.xfeeder.session

import com.quartz.platform.domain.model.XfeederGuidedSession
import com.quartz.platform.domain.model.XfeederGeospatialPolicy
import com.quartz.platform.domain.model.XfeederProximityEligibilityState
import com.quartz.platform.domain.model.XfeederReferenceAltitudeSourceState
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederSessionStatus
import com.quartz.platform.domain.model.XfeederUnreliableReason
import com.quartz.platform.domain.model.UserLocation

data class XfeederSectorCellContextItem(
    val label: String,
    val technology: String,
    val operatorName: String,
    val band: String,
    val isConnected: Boolean
)

data class XfeederSystemOperatorContextItem(
    val technology: String,
    val operatorName: String,
    val band: String,
    val totalCells: Int,
    val connectedCells: Int
)

data class XfeederGuidedSessionUiState(
    val isLoading: Boolean = true,
    val siteId: String = "",
    val siteLabel: String = "",
    val siteLatitude: Double? = null,
    val siteLongitude: Double? = null,
    val sectorId: String = "",
    val sectorCode: String = "",
    val session: XfeederGuidedSession? = null,
    val sessionHistory: List<XfeederGuidedSession> = emptyList(),
    val latestSessionId: String? = null,
    val selectedStatus: XfeederSessionStatus = XfeederSessionStatus.CREATED,
    val selectedOutcome: XfeederSectorOutcome = XfeederSectorOutcome.NOT_TESTED,
    val relatedSectorCodeInput: String = "",
    val selectedUnreliableReason: XfeederUnreliableReason? = null,
    val observedSectorCountInput: String = "",
    val measurementZoneLatitude: Double? = null,
    val measurementZoneLongitude: Double? = null,
    val measurementZoneRadiusMeters: Int = XfeederGeospatialPolicy.DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS,
    val measurementZoneExtensionReasonInput: String = "",
    val proximityReferenceAltitudeInput: String = "",
    val technicalReferenceAltitudeMeters: Double? = null,
    val effectiveReferenceAltitudeMeters: Double? = null,
    val proximityReferenceAltitudeSource: XfeederReferenceAltitudeSourceState =
        XfeederReferenceAltitudeSourceState.UNAVAILABLE,
    val proximityModeEnabled: Boolean = false,
    val userLocation: UserLocation? = null,
    val userAltitudeMeters: Double? = null,
    val userAltitudeVerticalAccuracyMeters: Float? = null,
    val distanceToMeasurementZoneMeters: Int? = null,
    val isInsideMeasurementZone: Boolean? = null,
    val proximityEligibilityState: XfeederProximityEligibilityState = XfeederProximityEligibilityState.UNAVAILABLE,
    val sectorAzimuthDegrees: Int? = null,
    val sectorCells: List<XfeederSectorCellContextItem> = emptyList(),
    val systemOperatorContexts: List<XfeederSystemOperatorContextItem> = emptyList(),
    val notesInput: String = "",
    val resultSummaryInput: String = "",
    val completionGuardMessage: String? = null,
    val hasUnsavedChanges: Boolean = false,
    val isCreatingSession: Boolean = false,
    val isRefreshingLocation: Boolean = false,
    val isCreatingDraft: Boolean = false,
    val isSavingSummary: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)
