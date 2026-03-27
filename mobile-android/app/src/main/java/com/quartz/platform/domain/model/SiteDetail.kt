package com.quartz.platform.domain.model

data class SiteDetail(
    val id: String,
    val externalCode: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val sectorsInService: Int,
    val sectorsForecast: Int,
    val indoorOnly: Boolean,
    val updatedAtEpochMillis: Long,
    val sectors: List<SiteSector> = emptyList()
)

data class SiteSector(
    val id: String,
    val siteId: String,
    val code: String,
    val azimuthDegrees: Int?,
    val status: String,
    val hasConnectedCell: Boolean,
    val antennas: List<SiteAntenna> = emptyList(),
    val cells: List<SiteCell> = emptyList()
)

data class SiteAntenna(
    val id: String,
    val sectorId: String,
    val reference: String,
    val installedState: String,
    val forecastState: String?,
    val tiltConfiguredDegrees: Double?,
    val tiltObservedDegrees: Double?,
    val documentationRef: String?
)

data class SiteCell(
    val id: String,
    val sectorId: String,
    val antennaId: String?,
    val label: String,
    val technology: String,
    val operatorName: String,
    val band: String,
    val pci: String?,
    val status: String,
    val isConnected: Boolean
)
