package com.quartz.platform.domain.model

import com.quartz.platform.domain.model.workflow.WorkflowGeospatialPolicy

object RetGeospatialPolicy {
    const val DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS =
        WorkflowGeospatialPolicy.DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS
    const val MEASUREMENT_ZONE_EXTENSION_STEP_METERS =
        WorkflowGeospatialPolicy.MEASUREMENT_ZONE_EXTENSION_STEP_METERS
    const val MAX_MEASUREMENT_ZONE_RADIUS_METERS =
        WorkflowGeospatialPolicy.MAX_MEASUREMENT_ZONE_RADIUS_METERS
    const val TARGET_OFFSET_FROM_SITE_METERS = WorkflowGeospatialPolicy.TARGET_OFFSET_FROM_SITE_METERS
    const val PROXIMITY_MAX_DISTANCE_METERS = WorkflowGeospatialPolicy.PROXIMITY_MAX_DISTANCE_METERS
    const val PROXIMITY_MAX_VERTICAL_ACCURACY_METERS =
        WorkflowGeospatialPolicy.PROXIMITY_MAX_VERTICAL_ACCURACY_METERS

    fun clampMeasurementZoneRadius(radiusMeters: Int): Int {
        return WorkflowGeospatialPolicy.clampMeasurementZoneRadius(radiusMeters)
    }

    fun isExtensionReasonRequired(radiusMeters: Int): Boolean {
        return WorkflowGeospatialPolicy.hasMeasurementZoneExtension(radiusMeters)
    }

    fun evaluateProximityEligibility(
        distanceMeters: Int?,
        userAltitudeMeters: Double?,
        userVerticalAccuracyMeters: Float?,
        referenceAltitudeMeters: Double?
    ): RetProximityEligibilityState {
        return WorkflowGeospatialPolicy.evaluateProximityEligibility(
            distanceMeters = distanceMeters,
            userAltitudeMeters = userAltitudeMeters,
            userVerticalAccuracyMeters = userVerticalAccuracyMeters,
            referenceAltitudeMeters = referenceAltitudeMeters
        )
    }

    fun resolveReferenceAltitudeSource(
        technicalReferenceAltitudeMeters: Double?,
        operatorOverrideAltitudeMeters: Double?
    ): RetReferenceAltitudeSourceState {
        return WorkflowGeospatialPolicy.resolveReferenceAltitudeSource(
            technicalReferenceAltitudeMeters = technicalReferenceAltitudeMeters,
            operatorOverrideAltitudeMeters = operatorOverrideAltitudeMeters
        )
    }

    fun resolveEffectiveReferenceAltitudeMeters(
        sourceState: RetReferenceAltitudeSourceState,
        technicalReferenceAltitudeMeters: Double?,
        operatorOverrideAltitudeMeters: Double?
    ): Double? {
        return WorkflowGeospatialPolicy.resolveEffectiveReferenceAltitudeMeters(
            sourceState = sourceState,
            technicalReferenceAltitudeMeters = technicalReferenceAltitudeMeters,
            operatorOverrideAltitudeMeters = operatorOverrideAltitudeMeters
        )
    }
}
