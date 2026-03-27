package com.quartz.platform.data.repository

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.domain.model.SiteDetail
import com.quartz.platform.domain.model.SiteSector
import org.junit.Test

class SiteSectorProjectionTest {

    @Test
    fun `projectSiteSectorCounts uses explicit technical sectors when available`() {
        val site = SiteDetail(
            id = "site-1",
            externalCode = "QRTZ-001",
            name = "Rabat Centre",
            latitude = 34.0,
            longitude = -6.8,
            status = "MIXED",
            sectorsInService = 0,
            sectorsForecast = 0,
            indoorOnly = false,
            updatedAtEpochMillis = 1L,
            sectors = listOf(
                sampleSector("site-1", "S0", "IN_SERVICE"),
                sampleSector("site-1", "S1", "IN_SERVICE"),
                sampleSector("site-1", "S2", "FORECAST")
            )
        )

        val counts = projectSiteSectorCounts(site)
        assertThat(counts.inServiceCount).isEqualTo(2)
        assertThat(counts.forecastCount).isEqualTo(1)
    }

    @Test
    fun `projectSiteSectorCounts falls back to site summary when sector list is empty`() {
        val site = SiteDetail(
            id = "site-1",
            externalCode = "QRTZ-001",
            name = "Rabat Centre",
            latitude = 34.0,
            longitude = -6.8,
            status = "IN_SERVICE",
            sectorsInService = 3,
            sectorsForecast = 1,
            indoorOnly = false,
            updatedAtEpochMillis = 1L,
            sectors = emptyList()
        )

        val counts = projectSiteSectorCounts(site)
        assertThat(counts.inServiceCount).isEqualTo(3)
        assertThat(counts.forecastCount).isEqualTo(1)
    }

    private fun sampleSector(siteId: String, code: String, status: String): SiteSector {
        return SiteSector(
            id = "$siteId-sector-$code",
            siteId = siteId,
            code = code,
            azimuthDegrees = null,
            status = status,
            hasConnectedCell = false
        )
    }
}
