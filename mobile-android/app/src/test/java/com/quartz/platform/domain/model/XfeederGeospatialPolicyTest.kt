package com.quartz.platform.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class XfeederGeospatialPolicyTest {

    @Test
    fun `clampMeasurementZoneRadius keeps value inside supported range`() {
        assertThat(XfeederGeospatialPolicy.clampMeasurementZoneRadius(10))
            .isEqualTo(XfeederGeospatialPolicy.DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS)
        assertThat(XfeederGeospatialPolicy.clampMeasurementZoneRadius(500))
            .isEqualTo(XfeederGeospatialPolicy.MAX_MEASUREMENT_ZONE_RADIUS_METERS)
    }

    @Test
    fun `isProximityEligible follows configured distance threshold`() {
        assertThat(
            XfeederGeospatialPolicy.isProximityEligible(
                XfeederGeospatialPolicy.PROXIMITY_MAX_DISTANCE_METERS
            )
        ).isTrue()
        assertThat(
            XfeederGeospatialPolicy.isProximityEligible(
                XfeederGeospatialPolicy.PROXIMITY_MAX_DISTANCE_METERS + 1
            )
        ).isFalse()
        assertThat(XfeederGeospatialPolicy.isProximityEligible(null)).isFalse()
    }
}
