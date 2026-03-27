package com.quartz.platform.domain.model

data class GuidedSessionClosureProjection(
    val sessionId: String,
    val siteId: String,
    val sectorId: String,
    val sectorCode: String,
    val sectorOutcome: XfeederSectorOutcome,
    val relatedSectorCode: String?,
    val unreliableReason: XfeederUnreliableReason?,
    val observedSectorCount: Int?,
    val updatedAtEpochMillis: Long
)
