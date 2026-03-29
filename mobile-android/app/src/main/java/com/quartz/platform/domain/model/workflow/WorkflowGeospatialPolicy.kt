package com.quartz.platform.domain.model.workflow

enum class WorkflowProximityEligibilityState {
    SUPPORTED,
    UNAVAILABLE,
    INELIGIBLE,
    ELIGIBLE
}

enum class WorkflowReferenceAltitudeSourceState {
    TECHNICAL_DEFAULT,
    OPERATOR_OVERRIDE,
    UNAVAILABLE
}

object WorkflowGeospatialPolicy {
    const val DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS = 120
    const val MEASUREMENT_ZONE_EXTENSION_STEP_METERS = 30
    const val MAX_MEASUREMENT_ZONE_RADIUS_METERS = 300
    const val TARGET_OFFSET_FROM_SITE_METERS = 90.0
    const val PROXIMITY_MAX_DISTANCE_METERS = 70
    const val PROXIMITY_MAX_VERTICAL_ACCURACY_METERS = 15f

    fun clampMeasurementZoneRadius(radiusMeters: Int): Int {
        return radiusMeters.coerceIn(
            minimumValue = DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS,
            maximumValue = MAX_MEASUREMENT_ZONE_RADIUS_METERS
        )
    }

    fun hasMeasurementZoneExtension(radiusMeters: Int): Boolean {
        return radiusMeters > DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS
    }

    fun evaluateProximityEligibility(
        distanceMeters: Int?,
        userAltitudeMeters: Double?,
        userVerticalAccuracyMeters: Float?,
        referenceAltitudeMeters: Double?
    ): WorkflowProximityEligibilityState {
        if (distanceMeters == null) return WorkflowProximityEligibilityState.UNAVAILABLE
        if (distanceMeters > PROXIMITY_MAX_DISTANCE_METERS) {
            return WorkflowProximityEligibilityState.INELIGIBLE
        }
        if (userAltitudeMeters == null) return WorkflowProximityEligibilityState.UNAVAILABLE
        if (
            userVerticalAccuracyMeters != null &&
            userVerticalAccuracyMeters > PROXIMITY_MAX_VERTICAL_ACCURACY_METERS
        ) {
            return WorkflowProximityEligibilityState.UNAVAILABLE
        }
        if (referenceAltitudeMeters == null) return WorkflowProximityEligibilityState.SUPPORTED
        return if (userAltitudeMeters >= referenceAltitudeMeters) {
            WorkflowProximityEligibilityState.ELIGIBLE
        } else {
            WorkflowProximityEligibilityState.INELIGIBLE
        }
    }

    fun resolveReferenceAltitudeSource(
        technicalReferenceAltitudeMeters: Double?,
        operatorOverrideAltitudeMeters: Double?
    ): WorkflowReferenceAltitudeSourceState {
        return when {
            operatorOverrideAltitudeMeters != null -> WorkflowReferenceAltitudeSourceState.OPERATOR_OVERRIDE
            technicalReferenceAltitudeMeters != null -> WorkflowReferenceAltitudeSourceState.TECHNICAL_DEFAULT
            else -> WorkflowReferenceAltitudeSourceState.UNAVAILABLE
        }
    }

    fun resolveEffectiveReferenceAltitudeMeters(
        sourceState: WorkflowReferenceAltitudeSourceState,
        technicalReferenceAltitudeMeters: Double?,
        operatorOverrideAltitudeMeters: Double?
    ): Double? {
        return when (sourceState) {
            WorkflowReferenceAltitudeSourceState.OPERATOR_OVERRIDE -> operatorOverrideAltitudeMeters
            WorkflowReferenceAltitudeSourceState.TECHNICAL_DEFAULT -> technicalReferenceAltitudeMeters
            WorkflowReferenceAltitudeSourceState.UNAVAILABLE -> null
        }
    }
}
