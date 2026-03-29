package com.quartz.platform.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "xfeeder_sessions",
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
        Index(value = ["siteId", "sectorId", "createdAtEpochMillis"])
    ]
)
data class XfeederSessionEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val sectorId: String,
    val sectorCode: String,
    val measurementZoneRadiusMeters: Int,
    val measurementZoneExtensionReason: String,
    val proximityModeEnabled: Boolean,
    val proximityReferenceAltitudeMeters: Double? = null,
    val proximityReferenceAltitudeSource: String,
    val status: String,
    val sectorOutcome: String,
    val closureRelatedSectorCode: String,
    val closureUnreliableReason: String?,
    val closureObservedSectorCount: Int?,
    val notes: String,
    val resultSummary: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val completedAtEpochMillis: Long?
)
