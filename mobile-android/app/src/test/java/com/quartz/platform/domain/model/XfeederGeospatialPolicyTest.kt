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
    fun `evaluateProximityEligibility returns supported when altitude reference is missing`() {
        val state = XfeederGeospatialPolicy.evaluateProximityEligibility(
            distanceMeters = XfeederGeospatialPolicy.PROXIMITY_MAX_DISTANCE_METERS,
            userAltitudeMeters = 120.0,
            userVerticalAccuracyMeters = 3f,
            referenceAltitudeMeters = null
        )

        assertThat(state).isEqualTo(XfeederProximityEligibilityState.SUPPORTED)
    }

    @Test
    fun `evaluateProximityEligibility returns eligible only with distance and altitude both valid`() {
        val eligible = XfeederGeospatialPolicy.evaluateProximityEligibility(
            distanceMeters = XfeederGeospatialPolicy.PROXIMITY_MAX_DISTANCE_METERS,
            userAltitudeMeters = 120.0,
            userVerticalAccuracyMeters = 5f,
            referenceAltitudeMeters = 118.5
        )
        val ineligibleDistance = XfeederGeospatialPolicy.evaluateProximityEligibility(
            distanceMeters = XfeederGeospatialPolicy.PROXIMITY_MAX_DISTANCE_METERS + 1,
            userAltitudeMeters = 120.0,
            userVerticalAccuracyMeters = 5f,
            referenceAltitudeMeters = 118.5
        )
        val ineligibleAltitude = XfeederGeospatialPolicy.evaluateProximityEligibility(
            distanceMeters = XfeederGeospatialPolicy.PROXIMITY_MAX_DISTANCE_METERS,
            userAltitudeMeters = 110.0,
            userVerticalAccuracyMeters = 5f,
            referenceAltitudeMeters = 118.5
        )
        val unavailableAccuracy = XfeederGeospatialPolicy.evaluateProximityEligibility(
            distanceMeters = XfeederGeospatialPolicy.PROXIMITY_MAX_DISTANCE_METERS,
            userAltitudeMeters = 120.0,
            userVerticalAccuracyMeters = XfeederGeospatialPolicy.PROXIMITY_MAX_VERTICAL_ACCURACY_METERS + 0.5f,
            referenceAltitudeMeters = 118.5
        )

        assertThat(eligible).isEqualTo(XfeederProximityEligibilityState.ELIGIBLE)
        assertThat(ineligibleDistance).isEqualTo(XfeederProximityEligibilityState.INELIGIBLE)
        assertThat(ineligibleAltitude).isEqualTo(XfeederProximityEligibilityState.INELIGIBLE)
        assertThat(unavailableAccuracy).isEqualTo(XfeederProximityEligibilityState.UNAVAILABLE)
    }

    @Test
    fun `resolveReferenceAltitudeSource keeps technical default and explicit override distinct`() {
        val technicalSource = XfeederGeospatialPolicy.resolveReferenceAltitudeSource(
            technicalReferenceAltitudeMeters = 118.0,
            operatorOverrideAltitudeMeters = null
        )
        val overrideSource = XfeederGeospatialPolicy.resolveReferenceAltitudeSource(
            technicalReferenceAltitudeMeters = 118.0,
            operatorOverrideAltitudeMeters = 123.0
        )
        val unavailableSource = XfeederGeospatialPolicy.resolveReferenceAltitudeSource(
            technicalReferenceAltitudeMeters = null,
            operatorOverrideAltitudeMeters = null
        )

        assertThat(technicalSource).isEqualTo(XfeederReferenceAltitudeSourceState.TECHNICAL_DEFAULT)
        assertThat(overrideSource).isEqualTo(XfeederReferenceAltitudeSourceState.OPERATOR_OVERRIDE)
        assertThat(unavailableSource).isEqualTo(XfeederReferenceAltitudeSourceState.UNAVAILABLE)
    }

    @Test
    fun `resolveEffectiveReferenceAltitudeMeters follows selected source state`() {
        val fromTechnical = XfeederGeospatialPolicy.resolveEffectiveReferenceAltitudeMeters(
            sourceState = XfeederReferenceAltitudeSourceState.TECHNICAL_DEFAULT,
            technicalReferenceAltitudeMeters = 118.0,
            operatorOverrideAltitudeMeters = 125.0
        )
        val fromOverride = XfeederGeospatialPolicy.resolveEffectiveReferenceAltitudeMeters(
            sourceState = XfeederReferenceAltitudeSourceState.OPERATOR_OVERRIDE,
            technicalReferenceAltitudeMeters = 118.0,
            operatorOverrideAltitudeMeters = 125.0
        )
        val unavailable = XfeederGeospatialPolicy.resolveEffectiveReferenceAltitudeMeters(
            sourceState = XfeederReferenceAltitudeSourceState.UNAVAILABLE,
            technicalReferenceAltitudeMeters = 118.0,
            operatorOverrideAltitudeMeters = 125.0
        )

        assertThat(fromTechnical).isEqualTo(118.0)
        assertThat(fromOverride).isEqualTo(125.0)
        assertThat(unavailable).isNull()
    }
}
