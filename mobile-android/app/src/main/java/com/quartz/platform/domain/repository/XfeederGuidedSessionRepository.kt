package com.quartz.platform.domain.repository

import com.quartz.platform.domain.model.XfeederGuidedSession
import com.quartz.platform.domain.model.GuidedSessionClosureProjection
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederClosureEvidence
import com.quartz.platform.domain.model.XfeederSessionStatus
import com.quartz.platform.domain.model.XfeederStepCode
import com.quartz.platform.domain.model.XfeederStepStatus
import kotlinx.coroutines.flow.Flow

interface XfeederGuidedSessionRepository {
    fun observeSectorSessionHistory(siteId: String, sectorId: String): Flow<List<XfeederGuidedSession>>
    fun observeLatestSectorSession(siteId: String, sectorId: String): Flow<XfeederGuidedSession?>
    fun observeSiteClosureProjections(siteId: String): Flow<List<GuidedSessionClosureProjection>>
    suspend fun createSession(siteId: String, sectorId: String, sectorCode: String): XfeederGuidedSession
    suspend fun updateStepStatus(
        sessionId: String,
        stepCode: XfeederStepCode,
        status: XfeederStepStatus
    )

    suspend fun updateSessionSummary(
        sessionId: String,
        status: XfeederSessionStatus,
        sectorOutcome: XfeederSectorOutcome,
        closureEvidence: XfeederClosureEvidence,
        notes: String,
        resultSummary: String
    )
}
