package com.quartz.platform.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "site_cells",
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
data class SiteCellEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val sectorId: String,
    val antennaId: String?,
    val label: String,
    val technology: String,
    val operatorName: String,
    val band: String,
    val pci: String?,
    val status: String,
    val isConnected: Boolean,
    val displayOrder: Int
)
