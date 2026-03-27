package com.quartz.platform.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "site_antennas",
    foreignKeys = [
        ForeignKey(
            entity = SiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["siteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SiteSectorEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["siteId"]),
        Index(value = ["sectorId"]),
        Index(value = ["sectorId", "displayOrder"])
    ]
)
data class SiteAntennaEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val sectorId: String,
    val reference: String,
    val installedState: String,
    val forecastState: String?,
    val tiltConfiguredDegrees: Double?,
    val tiltObservedDegrees: Double?,
    val documentationRef: String?,
    val displayOrder: Int
)
