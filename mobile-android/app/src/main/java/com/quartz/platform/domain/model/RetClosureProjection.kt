package com.quartz.platform.domain.model

data class RetClosureProjection(
    val sessionId: String,
    val siteId: String,
    val sectorId: String,
    val sectorCode: String,
    val sessionStatus: RetSessionStatus,
    val resultOutcome: RetResultOutcome,
    val requiredStepCount: Int,
    val completedRequiredStepCount: Int,
    val measurementZoneRadiusMeters: Int,
    val proximityModeEnabled: Boolean,
    val resultSummary: String?,
    val updatedAtEpochMillis: Long
)
