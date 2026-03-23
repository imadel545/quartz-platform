package com.quartz.platform.data.local.mapper

import com.quartz.platform.data.local.entity.SiteEntity
import com.quartz.platform.domain.model.Site

fun SiteEntity.toDomain(): Site {
    return Site(
        id = id,
        externalCode = externalCode,
        name = name,
        latitude = latitude,
        longitude = longitude,
        status = status,
        sectorsInService = sectorsInService,
        sectorsForecast = sectorsForecast,
        indoorOnly = indoorOnly
    )
}

fun Site.toEntity(updatedAtEpochMillis: Long): SiteEntity {
    return SiteEntity(
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
