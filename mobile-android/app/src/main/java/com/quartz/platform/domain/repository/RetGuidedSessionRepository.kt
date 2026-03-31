package com.quartz.platform.domain.repository

import com.quartz.platform.domain.model.RetGuidedSession
import com.quartz.platform.domain.model.RetReferenceAltitudeSourceState
import com.quartz.platform.domain.model.RetResultOutcome
import com.quartz.platform.domain.model.RetSessionStatus
import com.quartz.platform.domain.model.RetStepCode
import com.quartz.platform.domain.model.RetStepStatus
import com.quartz.platform.domain.model.RetClosureProjection
import kotlinx.coroutines.flow.Flow

interface RetGuidedSessionRepository {
    fun observeSectorSessionHistory(siteId: String, sectorId: String): Flow<List<RetGuidedSession>>
    fun observeLatestSectorSession(siteId: String, sectorId: String): Flow<RetGuidedSession?>
    fun observeSiteClosureProjections(siteId: String): Flow<List<RetClosureProjection>>
    suspend fun createSession(siteId: String, sectorId: String, sectorCode: String): RetGuidedSession
    suspend fun updateStepStatus(
        sessionId: String,
        stepCode: RetStepCode,
        status: RetStepStatus
    )

    suspend fun updateSessionSummary(
        sessionId: String,
        status: RetSessionStatus,
        resultOutcome: RetResultOutcome,
        notes: String,
        resultSummary: String
    )

    suspend fun updateSessionGeospatialContext(
        sessionId: String,
        measurementZoneRadiusMeters: Int,
        measurementZoneExtensionReason: String,
        proximityModeEnabled: Boolean,
        proximityReferenceAltitudeMeters: Double?,
        proximityReferenceAltitudeSource: RetReferenceAltitudeSourceState
    )
}
