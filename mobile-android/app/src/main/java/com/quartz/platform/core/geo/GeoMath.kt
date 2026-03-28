package com.quartz.platform.core.geo

import com.quartz.platform.domain.model.GeoCoordinate
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object GeoMath {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun distanceMeters(a: GeoCoordinate, b: GeoCoordinate): Int {
        val lat1 = Math.toRadians(a.latitude)
        val lon1 = Math.toRadians(a.longitude)
        val lat2 = Math.toRadians(b.latitude)
        val lon2 = Math.toRadians(b.longitude)

        val deltaLat = lat2 - lat1
        val deltaLon = lon2 - lon1

        val haversine = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
        val angularDistance = 2 * asin(sqrt(haversine))
        return (EARTH_RADIUS_METERS * angularDistance).roundToInt()
    }

    fun offset(
        origin: GeoCoordinate,
        bearingDegrees: Double,
        distanceMeters: Double
    ): GeoCoordinate {
        val bearing = Math.toRadians(bearingDegrees)
        val angularDistance = distanceMeters / EARTH_RADIUS_METERS
        val lat1 = Math.toRadians(origin.latitude)
        val lon1 = Math.toRadians(origin.longitude)

        val lat2 = asin(
            sin(lat1) * cos(angularDistance) +
                cos(lat1) * sin(angularDistance) * cos(bearing)
        )

        val lon2 = lon1 + atan2(
            sin(bearing) * sin(angularDistance) * cos(lat1),
            cos(angularDistance) - sin(lat1) * sin(lat2)
        )

        return GeoCoordinate(
            latitude = Math.toDegrees(lat2),
            longitude = Math.toDegrees(lon2)
        )
    }
}
