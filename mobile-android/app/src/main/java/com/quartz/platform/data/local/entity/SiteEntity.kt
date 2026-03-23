package com.quartz.platform.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey val id: String,
    val externalCode: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val sectorsInService: Int,
    val sectorsForecast: Int,
    val indoorOnly: Boolean,
    val updatedAtEpochMillis: Long
)
