package com.quartz.platform.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "site_sectors",
    foreignKeys = [
        ForeignKey(
            entity = SiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["siteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["siteId"]),
        Index(value = ["siteId", "displayOrder"])
    ]
)
data class SiteSectorEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val code: String,
    val azimuthDegrees: Int?,
    val status: String,
    val hasConnectedCell: Boolean,
    val displayOrder: Int
)
