package com.quartz.platform.domain.model

enum class XfeederProximityEligibilityState {
    SUPPORTED,
    UNAVAILABLE,
    INELIGIBLE,
    ELIGIBLE
}

enum class XfeederReferenceAltitudeSourceState {
    TECHNICAL_DEFAULT,
    OPERATOR_OVERRIDE,
    UNAVAILABLE
}

object XfeederGeospatialPolicy {
    const val DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS = 120
    const val MEASUREMENT_ZONE_EXTENSION_STEP_METERS = 30
    const val MAX_MEASUREMENT_ZONE_RADIUS_METERS = 300
    const val TARGET_OFFSET_FROM_SITE_METERS = 90.0
    const val PROXIMITY_MAX_DISTANCE_METERS = 70
    const val PROXIMITY_MAX_VERTICAL_ACCURACY_METERS = 15f

    fun clampMeasurementZoneRadius(radiusMeters: Int): Int {
        return radiusMeters.coerceIn(
            DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS,
            MAX_MEASUREMENT_ZONE_RADIUS_METERS
        )
    }

    fun isExtensionReasonRequired(radiusMeters: Int): Boolean {
        return radiusMeters > DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS
    }

    fun evaluateProximityEligibility(
        distanceMeters: Int?,
        userAltitudeMeters: Double?,
        userVerticalAccuracyMeters: Float?,
        referenceAltitudeMeters: Double?
    ): XfeederProximityEligibilityState {
        if (distanceMeters == null) return XfeederProximityEligibilityState.UNAVAILABLE
        if (distanceMeters > PROXIMITY_MAX_DISTANCE_METERS) {
            return XfeederProximityEligibilityState.INELIGIBLE
        }
        if (userAltitudeMeters == null) return XfeederProximityEligibilityState.UNAVAILABLE
        if (
            userVerticalAccuracyMeters != null &&
            userVerticalAccuracyMeters > PROXIMITY_MAX_VERTICAL_ACCURACY_METERS
        ) {
            return XfeederProximityEligibilityState.UNAVAILABLE
        }
        if (referenceAltitudeMeters == null) return XfeederProximityEligibilityState.SUPPORTED
        return if (userAltitudeMeters >= referenceAltitudeMeters) {
            XfeederProximityEligibilityState.ELIGIBLE
        } else {
            XfeederProximityEligibilityState.INELIGIBLE
        }
    }

    fun resolveReferenceAltitudeSource(
        technicalReferenceAltitudeMeters: Double?,
        operatorOverrideAltitudeMeters: Double?
    ): XfeederReferenceAltitudeSourceState {
        return when {
            operatorOverrideAltitudeMeters != null -> XfeederReferenceAltitudeSourceState.OPERATOR_OVERRIDE
            technicalReferenceAltitudeMeters != null -> XfeederReferenceAltitudeSourceState.TECHNICAL_DEFAULT
            else -> XfeederReferenceAltitudeSourceState.UNAVAILABLE
        }
    }

    fun resolveEffectiveReferenceAltitudeMeters(
        sourceState: XfeederReferenceAltitudeSourceState,
        technicalReferenceAltitudeMeters: Double?,
        operatorOverrideAltitudeMeters: Double?
    ): Double? {
        return when (sourceState) {
            XfeederReferenceAltitudeSourceState.OPERATOR_OVERRIDE -> operatorOverrideAltitudeMeters
            XfeederReferenceAltitudeSourceState.TECHNICAL_DEFAULT -> technicalReferenceAltitudeMeters
            XfeederReferenceAltitudeSourceState.UNAVAILABLE -> null
        }
    }
}
