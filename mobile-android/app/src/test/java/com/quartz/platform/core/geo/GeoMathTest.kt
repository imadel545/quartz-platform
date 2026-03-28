package com.quartz.platform.core.geo

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.domain.model.GeoCoordinate
import org.junit.Test

class GeoMathTest {

    @Test
    fun `distanceMeters returns zero for identical coordinates`() {
        val point = GeoCoordinate(latitude = 34.0, longitude = -6.8)

        val distance = GeoMath.distanceMeters(point, point)

        assertThat(distance).isEqualTo(0)
    }

    @Test
    fun `offset generates target close to requested distance`() {
        val origin = GeoCoordinate(latitude = 34.0, longitude = -6.8)

        val target = GeoMath.offset(
            origin = origin,
            bearingDegrees = 90.0,
            distanceMeters = 100.0
        )
        val distance = GeoMath.distanceMeters(origin, target)

        assertThat(distance).isAtLeast(95)
        assertThat(distance).isAtMost(105)
    }
}
