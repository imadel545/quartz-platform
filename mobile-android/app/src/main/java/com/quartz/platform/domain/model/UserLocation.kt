package com.quartz.platform.domain.model

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val capturedAtEpochMillis: Long?,
    val altitudeMeters: Double? = null,
    val verticalAccuracyMeters: Float? = null
)
