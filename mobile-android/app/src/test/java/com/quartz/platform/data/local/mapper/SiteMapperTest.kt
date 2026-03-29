package com.quartz.platform.data.local.mapper

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.data.local.entity.SiteAntennaEntity
import com.quartz.platform.data.local.entity.SiteCellEntity
import com.quartz.platform.data.local.entity.SiteEntity
import com.quartz.platform.data.local.entity.SiteSectorEntity
import com.quartz.platform.domain.model.SiteAntenna
import com.quartz.platform.domain.model.SiteCell
import com.quartz.platform.domain.model.SiteDetail
import com.quartz.platform.domain.model.SiteSector
import org.junit.Test

class SiteMapperTest {

    @Test
    fun `toSiteDetail groups technical structure by sector`() {
        val site = SiteEntity(
            id = "site-1",
            externalCode = "QRTZ-001",
            name = "Rabat Centre",
            latitude = 34.02,
            longitude = -6.84,
            status = "IN_SERVICE",
            sectorsInService = 3,
            sectorsForecast = 0,
            indoorOnly = false,
            updatedAtEpochMillis = 10L
        )
        val sector = SiteSectorEntity(
            id = "site-1-sector-S0",
            siteId = "site-1",
            code = "S0",
            azimuthDegrees = 0,
            status = "IN_SERVICE",
            hasConnectedCell = true,
            displayOrder = 0
        )
        val antenna = SiteAntennaEntity(
            id = "site-1-antenna-S0-0",
            siteId = "site-1",
            sectorId = "site-1-sector-S0",
            reference = "ANT-RB-S0",
            referenceAltitudeMeters = 118.0,
            installedState = "INSTALLED",
            forecastState = null,
            tiltConfiguredDegrees = 4.0,
            tiltObservedDegrees = 3.7,
            documentationRef = "DOC-ANT-RB-S0",
            displayOrder = 0
        )
        val cell = SiteCellEntity(
            id = "site-1-cell-S0-L800",
            siteId = "site-1",
            sectorId = "site-1-sector-S0",
            antennaId = null,
            label = "RB-S0-L800",
            technology = "4G",
            operatorName = "ORANGE",
            band = "L800",
            pci = "101",
            status = "ACTIVE",
            isConnected = true,
            displayOrder = 0
        )

        val detail = site.toSiteDetail(
            sectors = listOf(sector),
            antennas = listOf(antenna),
            cells = listOf(cell)
        )

        assertThat(detail.sectors).hasSize(1)
        assertThat(detail.sectors.first().antennas).hasSize(1)
        assertThat(detail.sectors.first().cells).hasSize(1)
        assertThat(detail.sectors.first().cells.first().isConnected).isTrue()
    }

    @Test
    fun `toEntity applies projected sector counts when provided`() {
        val detail = SiteDetail(
            id = "site-1",
            externalCode = "QRTZ-001",
            name = "Rabat Centre",
            latitude = 34.02,
            longitude = -6.84,
            status = "MIXED",
            sectorsInService = 0,
            sectorsForecast = 0,
            indoorOnly = false,
            updatedAtEpochMillis = 10L,
            sectors = listOf(
                SiteSector(
                    id = "site-1-sector-S0",
                    siteId = "site-1",
                    code = "S0",
                    azimuthDegrees = 0,
                    status = "IN_SERVICE",
                    hasConnectedCell = false,
                    antennas = listOf(
                        SiteAntenna(
                            id = "site-1-antenna-S0-0",
                            sectorId = "site-1-sector-S0",
                            reference = "ANT-RB-S0",
                            referenceAltitudeMeters = 118.0,
                            installedState = "INSTALLED",
                            forecastState = null,
                            tiltConfiguredDegrees = null,
                            tiltObservedDegrees = null,
                            documentationRef = null
                        )
                    ),
                    cells = listOf(
                        SiteCell(
                            id = "site-1-cell-S0-L800",
                            sectorId = "site-1-sector-S0",
                            antennaId = null,
                            label = "RB-S0-L800",
                            technology = "4G",
                            operatorName = "ORANGE",
                            band = "L800",
                            pci = null,
                            status = "ACTIVE",
                            isConnected = false
                        )
                    )
                )
            )
        )

        val entity = detail.toEntity(
            projectedSectorsInService = 1,
            projectedSectorsForecast = 0
        )

        assertThat(entity.sectorsInService).isEqualTo(1)
        assertThat(entity.sectorsForecast).isEqualTo(0)
    }
}
