package com.quartz.platform.data.bootstrap

import com.quartz.platform.domain.model.SiteDetail
import com.quartz.platform.domain.model.SiteAntenna
import com.quartz.platform.domain.model.SiteCell
import com.quartz.platform.domain.model.SiteSector
import com.quartz.platform.domain.repository.SiteSnapshotBootstrapSource
import javax.inject.Inject

class LocalDemoSiteSnapshotSource @Inject constructor() : SiteSnapshotBootstrapSource {

    override suspend fun loadDemoSnapshot(): List<SiteDetail> {
        val now = System.currentTimeMillis()
        return listOf(
            SiteDetail(
                id = "demo-site-001",
                externalCode = "QRTZ-001",
                name = "Rabat Centre",
                latitude = 34.020882,
                longitude = -6.841650,
                status = "IN_SERVICE",
                sectorsInService = 3,
                sectorsForecast = 0,
                indoorOnly = false,
                updatedAtEpochMillis = now,
                sectors = listOf(
                    demoSector(
                        siteId = "demo-site-001",
                        code = "S0",
                        azimuth = 0,
                        status = "IN_SERVICE",
                        connectedCell = "RB-S0-L800",
                        antennaRef = "ANT-RB-S0",
                        cells = listOf(
                            demoCell(
                                id = "cell-rb-s0-l800",
                                sectorId = "demo-site-001-sector-S0",
                                label = "RB-S0-L800",
                                technology = "4G",
                                operator = "ORANGE",
                                band = "L800",
                                pci = "101",
                                status = "ACTIVE",
                                isConnected = true
                            ),
                            demoCell(
                                id = "cell-rb-s0-nr3500",
                                sectorId = "demo-site-001-sector-S0",
                                label = "RB-S0-NR3500",
                                technology = "5G NSA",
                                operator = "ORANGE",
                                band = "N78",
                                pci = "601",
                                status = "ACTIVE",
                                isConnected = false
                            )
                        )
                    ),
                    demoSector(
                        siteId = "demo-site-001",
                        code = "S1",
                        azimuth = 120,
                        status = "IN_SERVICE",
                        connectedCell = null,
                        antennaRef = "ANT-RB-S1",
                        cells = listOf(
                            demoCell(
                                id = "cell-rb-s1-l1800",
                                sectorId = "demo-site-001-sector-S1",
                                label = "RB-S1-L1800",
                                technology = "4G",
                                operator = "ORANGE",
                                band = "L1800",
                                pci = "122",
                                status = "ACTIVE",
                                isConnected = false
                            )
                        )
                    ),
                    demoSector(
                        siteId = "demo-site-001",
                        code = "S2",
                        azimuth = 240,
                        status = "IN_SERVICE",
                        connectedCell = null,
                        antennaRef = "ANT-RB-S2",
                        cells = listOf(
                            demoCell(
                                id = "cell-rb-s2-l2600",
                                sectorId = "demo-site-001-sector-S2",
                                label = "RB-S2-L2600",
                                technology = "4G",
                                operator = "ORANGE",
                                band = "L2600",
                                pci = "138",
                                status = "ACTIVE",
                                isConnected = false
                            )
                        )
                    )
                )
            ),
            SiteDetail(
                id = "demo-site-002",
                externalCode = "QRTZ-002",
                name = "Casablanca Port",
                latitude = 33.606183,
                longitude = -7.632952,
                status = "MIXED",
                sectorsInService = 3,
                sectorsForecast = 3,
                indoorOnly = false,
                updatedAtEpochMillis = now,
                sectors = listOf(
                    demoSector(
                        siteId = "demo-site-002",
                        code = "S0",
                        azimuth = 15,
                        status = "IN_SERVICE",
                        connectedCell = "CB-S0-L1800",
                        antennaRef = "ANT-CB-S0",
                        cells = listOf(
                            demoCell(
                                id = "cell-cb-s0-l1800",
                                sectorId = "demo-site-002-sector-S0",
                                label = "CB-S0-L1800",
                                technology = "4G",
                                operator = "INWI",
                                band = "L1800",
                                pci = "145",
                                status = "ACTIVE",
                                isConnected = true
                            )
                        )
                    ),
                    demoSector(
                        siteId = "demo-site-002",
                        code = "S1",
                        azimuth = 135,
                        status = "FORECAST",
                        connectedCell = null,
                        antennaRef = "ANT-CB-S1",
                        cells = listOf(
                            demoCell(
                                id = "cell-cb-s1-nr3500",
                                sectorId = "demo-site-002-sector-S1",
                                label = "CB-S1-NR3500",
                                technology = "5G",
                                operator = "INWI",
                                band = "N78",
                                pci = null,
                                status = "FORECAST",
                                isConnected = false
                            )
                        )
                    ),
                    demoSector(
                        siteId = "demo-site-002",
                        code = "S2",
                        azimuth = 255,
                        status = "IN_SERVICE",
                        connectedCell = null,
                        antennaRef = "ANT-CB-S2",
                        cells = listOf(
                            demoCell(
                                id = "cell-cb-s2-l800",
                                sectorId = "demo-site-002-sector-S2",
                                label = "CB-S2-L800",
                                technology = "4G",
                                operator = "INWI",
                                band = "L800",
                                pci = "167",
                                status = "ACTIVE",
                                isConnected = false
                            )
                        )
                    )
                )
            ),
            SiteDetail(
                id = "demo-site-003",
                externalCode = "QRTZ-003",
                name = "Tangier Indoor Hub",
                latitude = 35.759465,
                longitude = -5.833954,
                status = "FORECAST_ONLY",
                sectorsInService = 0,
                sectorsForecast = 3,
                indoorOnly = true,
                updatedAtEpochMillis = now,
                sectors = listOf(
                    demoSector(
                        siteId = "demo-site-003",
                        code = "S0",
                        azimuth = 40,
                        status = "FORECAST",
                        connectedCell = null,
                        antennaRef = "ANT-TG-S0",
                        cells = listOf(
                            demoCell(
                                id = "cell-tg-s0-l2100",
                                sectorId = "demo-site-003-sector-S0",
                                label = "TG-S0-L2100",
                                technology = "4G",
                                operator = "MAROC TELECOM",
                                band = "L2100",
                                pci = null,
                                status = "FORECAST",
                                isConnected = false
                            )
                        )
                    ),
                    demoSector(
                        siteId = "demo-site-003",
                        code = "S1",
                        azimuth = 160,
                        status = "FORECAST",
                        connectedCell = null,
                        antennaRef = "ANT-TG-S1",
                        cells = emptyList()
                    ),
                    demoSector(
                        siteId = "demo-site-003",
                        code = "S2",
                        azimuth = 280,
                        status = "FORECAST",
                        connectedCell = null,
                        antennaRef = "ANT-TG-S2",
                        cells = emptyList()
                    )
                )
            )
        )
    }

    private fun demoSector(
        siteId: String,
        code: String,
        azimuth: Int,
        status: String,
        connectedCell: String?,
        antennaRef: String,
        cells: List<SiteCell>
    ): SiteSector {
        return SiteSector(
            id = "$siteId-sector-$code",
            siteId = siteId,
            code = code,
            azimuthDegrees = azimuth,
            status = status,
            hasConnectedCell = connectedCell != null,
            antennas = listOf(
                SiteAntenna(
                    id = "$siteId-antenna-$code-0",
                    sectorId = "$siteId-sector-$code",
                    reference = antennaRef,
                    installedState = if (status == "FORECAST") "PLANNED" else "INSTALLED",
                    forecastState = if (status == "FORECAST") "FORECAST" else null,
                    tiltConfiguredDegrees = if (status == "FORECAST") null else 4.0,
                    tiltObservedDegrees = if (status == "FORECAST") null else 3.5,
                    documentationRef = "DOC-$antennaRef"
                )
            ),
            cells = cells
        )
    }

    private fun demoCell(
        id: String,
        sectorId: String,
        label: String,
        technology: String,
        operator: String,
        band: String,
        pci: String?,
        status: String,
        isConnected: Boolean
    ): SiteCell {
        return SiteCell(
            id = id,
            sectorId = sectorId,
            antennaId = null,
            label = label,
            technology = technology,
            operatorName = operator,
            band = band,
            pci = pci,
            status = status,
            isConnected = isConnected
        )
    }
}
