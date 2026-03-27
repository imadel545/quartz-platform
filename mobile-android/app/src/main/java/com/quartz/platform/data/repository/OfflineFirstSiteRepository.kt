package com.quartz.platform.data.repository

import androidx.room.withTransaction
import com.quartz.platform.data.local.QuartzDatabase
import com.quartz.platform.data.local.dao.SiteAntennaDao
import com.quartz.platform.data.local.dao.SiteCellDao
import com.quartz.platform.data.local.dao.SiteDao
import com.quartz.platform.data.local.dao.SiteSectorDao
import com.quartz.platform.data.local.mapper.toEntity
import com.quartz.platform.data.local.mapper.toSiteDetail
import com.quartz.platform.data.local.mapper.toSiteSummary
import com.quartz.platform.domain.model.SiteDetail
import com.quartz.platform.domain.model.SiteSummary
import com.quartz.platform.domain.repository.SiteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class OfflineFirstSiteRepository @Inject constructor(
    private val siteDao: SiteDao,
    private val siteSectorDao: SiteSectorDao,
    private val siteAntennaDao: SiteAntennaDao,
    private val siteCellDao: SiteCellDao,
    private val database: QuartzDatabase
) : SiteRepository {

    override fun observeSiteList(): Flow<List<SiteSummary>> {
        return siteDao.observeAll().map { entities -> entities.map { it.toSiteSummary() } }
    }

    override fun observeSiteDetail(siteId: String): Flow<SiteDetail?> {
        return combine(
            siteDao.observeById(siteId),
            siteSectorDao.observeBySiteId(siteId),
            siteAntennaDao.observeBySiteId(siteId),
            siteCellDao.observeBySiteId(siteId)
        ) { site, sectors, antennas, cells ->
            site?.toSiteDetail(
                sectors = sectors,
                antennas = antennas,
                cells = cells
            )
        }
    }

    override suspend fun replaceSitesSnapshot(sites: List<SiteDetail>) {
        val siteEntities = mutableListOf<com.quartz.platform.data.local.entity.SiteEntity>()
        val sectorEntities = mutableListOf<com.quartz.platform.data.local.entity.SiteSectorEntity>()
        val antennaEntities = mutableListOf<com.quartz.platform.data.local.entity.SiteAntennaEntity>()
        val cellEntities = mutableListOf<com.quartz.platform.data.local.entity.SiteCellEntity>()

        sites.forEach { site ->
            val counts = projectSiteSectorCounts(site)
            siteEntities += site.toEntity(
                projectedSectorsInService = counts.inServiceCount,
                projectedSectorsForecast = counts.forecastCount
            )

            site.sectors.forEachIndexed { sectorIndex, sector ->
                val sectorId = sector.id.ifBlank { "${site.id}-sector-$sectorIndex" }
                val normalizedSector = sector.copy(
                    hasConnectedCell = sector.hasConnectedCell || sector.cells.any { it.isConnected }
                )
                sectorEntities += normalizedSector.toEntity(
                    siteId = site.id,
                    displayOrder = sectorIndex,
                    fallbackSectorId = sectorId
                )

                sector.antennas.forEachIndexed { antennaIndex, antenna ->
                    antennaEntities += antenna.toEntity(
                        siteId = site.id,
                        sectorId = sectorId,
                        displayOrder = antennaIndex,
                        fallbackAntennaId = "$sectorId-antenna-$antennaIndex"
                    )
                }

                sector.cells.forEachIndexed { cellIndex, cell ->
                    cellEntities += cell.toEntity(
                        siteId = site.id,
                        sectorId = sectorId,
                        displayOrder = cellIndex,
                        fallbackCellId = "$sectorId-cell-$cellIndex"
                    )
                }
            }
        }

        database.withTransaction {
            siteCellDao.deleteAll()
            siteAntennaDao.deleteAll()
            siteSectorDao.deleteAll()
            siteDao.deleteAll()
            siteDao.upsertAll(siteEntities)
            siteSectorDao.upsertAll(sectorEntities)
            siteAntennaDao.upsertAll(antennaEntities)
            siteCellDao.upsertAll(cellEntities)
        }
    }
}

internal fun projectSiteSectorCounts(site: SiteDetail): SectorCountProjection {
    if (site.sectors.isEmpty()) {
        return SectorCountProjection(
            inServiceCount = site.sectorsInService,
            forecastCount = site.sectorsForecast
        )
    }

    val inService = site.sectors.count { it.status.equals("IN_SERVICE", ignoreCase = true) }
    val forecast = site.sectors.count { it.status.equals("FORECAST", ignoreCase = true) }
    return SectorCountProjection(
        inServiceCount = inService,
        forecastCount = forecast
    )
}

internal data class SectorCountProjection(
    val inServiceCount: Int,
    val forecastCount: Int
)
