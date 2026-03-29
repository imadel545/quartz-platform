package com.quartz.platform.data.local.mapper

import com.quartz.platform.data.local.entity.SiteAntennaEntity
import com.quartz.platform.data.local.entity.SiteCellEntity
import com.quartz.platform.data.local.entity.SiteEntity
import com.quartz.platform.data.local.entity.SiteSectorEntity
import com.quartz.platform.domain.model.SiteAntenna
import com.quartz.platform.domain.model.SiteCell
import com.quartz.platform.domain.model.SiteDetail
import com.quartz.platform.domain.model.SiteSector
import com.quartz.platform.domain.model.SiteSummary

fun SiteEntity.toSiteSummary(): SiteSummary {
    return SiteSummary(
        id = id,
        externalCode = externalCode,
        name = name,
        latitude = latitude,
        longitude = longitude,
        status = status,
        sectorsInService = sectorsInService,
        sectorsForecast = sectorsForecast,
        indoorOnly = indoorOnly,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}

fun SiteEntity.toSiteDetail(
    sectors: List<SiteSectorEntity>,
    antennas: List<SiteAntennaEntity>,
    cells: List<SiteCellEntity>
): SiteDetail {
    val antennasBySector = antennas
        .sortedBy { it.displayOrder }
        .groupBy(keySelector = { it.sectorId }, valueTransform = { it.toDomainModel() })
    val cellsBySector = cells
        .sortedBy { it.displayOrder }
        .groupBy(keySelector = { it.sectorId }, valueTransform = { it.toDomainModel() })
    val technicalSectors = sectors
        .sortedBy { it.displayOrder }
        .map { sector ->
            SiteSector(
                id = sector.id,
                siteId = sector.siteId,
                code = sector.code,
                azimuthDegrees = sector.azimuthDegrees,
                status = sector.status,
                hasConnectedCell = sector.hasConnectedCell,
                antennas = antennasBySector[sector.id].orEmpty(),
                cells = cellsBySector[sector.id].orEmpty()
            )
        }

    return SiteDetail(
        id = id,
        externalCode = externalCode,
        name = name,
        latitude = latitude,
        longitude = longitude,
        status = status,
        sectorsInService = sectorsInService,
        sectorsForecast = sectorsForecast,
        indoorOnly = indoorOnly,
        updatedAtEpochMillis = updatedAtEpochMillis,
        sectors = technicalSectors
    )
}

fun SiteDetail.toEntity(
    projectedSectorsInService: Int = sectorsInService,
    projectedSectorsForecast: Int = sectorsForecast
): SiteEntity {
    return SiteEntity(
        id = id,
        externalCode = externalCode,
        name = name,
        latitude = latitude,
        longitude = longitude,
        status = status,
        sectorsInService = projectedSectorsInService,
        sectorsForecast = projectedSectorsForecast,
        indoorOnly = indoorOnly,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}

fun SiteSector.toEntity(
    siteId: String,
    displayOrder: Int,
    fallbackSectorId: String
): SiteSectorEntity {
    return SiteSectorEntity(
        id = id.ifBlank { fallbackSectorId },
        siteId = siteId,
        code = code,
        azimuthDegrees = azimuthDegrees,
        status = status,
        hasConnectedCell = hasConnectedCell,
        displayOrder = displayOrder
    )
}

fun SiteAntenna.toEntity(
    siteId: String,
    sectorId: String,
    displayOrder: Int,
    fallbackAntennaId: String
): SiteAntennaEntity {
    return SiteAntennaEntity(
        id = id.ifBlank { fallbackAntennaId },
        siteId = siteId,
        sectorId = sectorId,
        reference = reference,
        referenceAltitudeMeters = referenceAltitudeMeters,
        installedState = installedState,
        forecastState = forecastState,
        tiltConfiguredDegrees = tiltConfiguredDegrees,
        tiltObservedDegrees = tiltObservedDegrees,
        documentationRef = documentationRef,
        displayOrder = displayOrder
    )
}

fun SiteCell.toEntity(
    siteId: String,
    sectorId: String,
    displayOrder: Int,
    fallbackCellId: String
): SiteCellEntity {
    return SiteCellEntity(
        id = id.ifBlank { fallbackCellId },
        siteId = siteId,
        sectorId = sectorId,
        antennaId = antennaId,
        label = label,
        technology = technology,
        operatorName = operatorName,
        band = band,
        pci = pci,
        status = status,
        isConnected = isConnected,
        displayOrder = displayOrder
    )
}

private fun SiteAntennaEntity.toDomainModel(): SiteAntenna {
    return SiteAntenna(
        id = id,
        sectorId = sectorId,
        reference = reference,
        referenceAltitudeMeters = referenceAltitudeMeters,
        installedState = installedState,
        forecastState = forecastState,
        tiltConfiguredDegrees = tiltConfiguredDegrees,
        tiltObservedDegrees = tiltObservedDegrees,
        documentationRef = documentationRef
    )
}

private fun SiteCellEntity.toDomainModel(): SiteCell {
    return SiteCell(
        id = id,
        sectorId = sectorId,
        antennaId = antennaId,
        label = label,
        technology = technology,
        operatorName = operatorName,
        band = band,
        pci = pci,
        status = status,
        isConnected = isConnected
    )
}
