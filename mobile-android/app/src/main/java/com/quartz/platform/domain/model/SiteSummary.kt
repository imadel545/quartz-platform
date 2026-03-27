package com.quartz.platform.domain.model

data class SiteSummary(
    val id: String,
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
