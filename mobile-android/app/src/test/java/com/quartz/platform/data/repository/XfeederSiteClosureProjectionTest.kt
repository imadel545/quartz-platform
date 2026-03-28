package com.quartz.platform.data.repository

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.data.local.entity.XfeederSessionEntity
import com.quartz.platform.domain.model.XfeederGeospatialPolicy
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederSessionStatus
import com.quartz.platform.domain.model.XfeederUnreliableReason
import org.junit.Test

class XfeederSiteClosureProjectionTest {

    @Test
    fun `toSiteClosureProjections keeps latest completed per sector and ignores non completed`() {
        val sessions = listOf(
            XfeederSessionEntity(
                id = "s0-old",
                siteId = "site-1",
                sectorId = "sector-s0",
                sectorCode = "S0",
                measurementZoneRadiusMeters = XfeederGeospatialPolicy.DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS,
                measurementZoneExtensionReason = "",
                proximityModeEnabled = false,
                status = XfeederSessionStatus.COMPLETED.name,
                sectorOutcome = XfeederSectorOutcome.CROSSED.name,
                closureRelatedSectorCode = "S1",
                closureUnreliableReason = null,
                closureObservedSectorCount = null,
                notes = "",
                resultSummary = "",
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 10L,
                completedAtEpochMillis = 10L
            ),
            XfeederSessionEntity(
                id = "s0-latest-completed",
                siteId = "site-1",
                sectorId = "sector-s0",
                sectorCode = "S0",
                measurementZoneRadiusMeters = XfeederGeospatialPolicy.DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS,
                measurementZoneExtensionReason = "",
                proximityModeEnabled = false,
                status = XfeederSessionStatus.COMPLETED.name,
                sectorOutcome = XfeederSectorOutcome.UNRELIABLE.name,
                closureRelatedSectorCode = "",
                closureUnreliableReason = XfeederUnreliableReason.NO_MAJORITY_SECTOR.name,
                closureObservedSectorCount = 3,
                notes = "",
                resultSummary = "",
                createdAtEpochMillis = 2L,
                updatedAtEpochMillis = 20L,
                completedAtEpochMillis = 20L
            ),
            XfeederSessionEntity(
                id = "s0-in-progress",
                siteId = "site-1",
                sectorId = "sector-s0",
                sectorCode = "S0",
                measurementZoneRadiusMeters = XfeederGeospatialPolicy.DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS,
                measurementZoneExtensionReason = "",
                proximityModeEnabled = false,
                status = XfeederSessionStatus.IN_PROGRESS.name,
                sectorOutcome = XfeederSectorOutcome.OK.name,
                closureRelatedSectorCode = "",
                closureUnreliableReason = null,
                closureObservedSectorCount = null,
                notes = "",
                resultSummary = "",
                createdAtEpochMillis = 3L,
                updatedAtEpochMillis = 30L,
                completedAtEpochMillis = null
            ),
            XfeederSessionEntity(
                id = "s1-completed",
                siteId = "site-1",
                sectorId = "sector-s1",
                sectorCode = "S1",
                measurementZoneRadiusMeters = XfeederGeospatialPolicy.DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS,
                measurementZoneExtensionReason = "",
                proximityModeEnabled = false,
                status = XfeederSessionStatus.COMPLETED.name,
                sectorOutcome = XfeederSectorOutcome.MIXFEEDER.name,
                closureRelatedSectorCode = "S0",
                closureUnreliableReason = null,
                closureObservedSectorCount = null,
                notes = "",
                resultSummary = "",
                createdAtEpochMillis = 4L,
                updatedAtEpochMillis = 40L,
                completedAtEpochMillis = 40L
            ),
            XfeederSessionEntity(
                id = "s2-created-only",
                siteId = "site-1",
                sectorId = "sector-s2",
                sectorCode = "S2",
                measurementZoneRadiusMeters = XfeederGeospatialPolicy.DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS,
                measurementZoneExtensionReason = "",
                proximityModeEnabled = false,
                status = XfeederSessionStatus.CREATED.name,
                sectorOutcome = XfeederSectorOutcome.NOT_TESTED.name,
                closureRelatedSectorCode = "",
                closureUnreliableReason = null,
                closureObservedSectorCount = null,
                notes = "",
                resultSummary = "",
                createdAtEpochMillis = 5L,
                updatedAtEpochMillis = 50L,
                completedAtEpochMillis = null
            )
        )

        val projections = toSiteClosureProjections(sessions)

        assertThat(projections).hasSize(2)
        assertThat(projections.map { it.sectorCode }).containsExactly("S0", "S1").inOrder()
        assertThat(projections.first().sectorOutcome).isEqualTo(XfeederSectorOutcome.UNRELIABLE)
        assertThat(projections.first().unreliableReason).isEqualTo(XfeederUnreliableReason.NO_MAJORITY_SECTOR)
        assertThat(projections.first().observedSectorCount).isEqualTo(3)
        assertThat(projections.last().relatedSectorCode).isEqualTo("S0")
    }
}
