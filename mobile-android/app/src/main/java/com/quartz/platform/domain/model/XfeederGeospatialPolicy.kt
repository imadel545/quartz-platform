package com.quartz.platform.domain.model

object XfeederGeospatialPolicy {
    const val DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS = 120
    const val MEASUREMENT_ZONE_EXTENSION_STEP_METERS = 30
    const val MAX_MEASUREMENT_ZONE_RADIUS_METERS = 300
    const val TARGET_OFFSET_FROM_SITE_METERS = 90.0
    const val PROXIMITY_MAX_DISTANCE_METERS = 70

    fun clampMeasurementZoneRadius(radiusMeters: Int): Int {
        return radiusMeters.coerceIn(
            DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS,
            MAX_MEASUREMENT_ZONE_RADIUS_METERS
        )
    }

    fun isExtensionReasonRequired(radiusMeters: Int): Boolean {
        return radiusMeters > DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS
    }

    fun isProximityEligible(distanceMeters: Int?): Boolean {
        return distanceMeters != null && distanceMeters <= PROXIMITY_MAX_DISTANCE_METERS
    }
}
