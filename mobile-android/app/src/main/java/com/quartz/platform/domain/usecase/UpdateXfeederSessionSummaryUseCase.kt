package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederClosureEvidence
import com.quartz.platform.domain.model.XfeederSessionStatus
import com.quartz.platform.domain.repository.XfeederGuidedSessionRepository
import javax.inject.Inject

class UpdateXfeederSessionSummaryUseCase @Inject constructor(
    private val repository: XfeederGuidedSessionRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        status: XfeederSessionStatus,
        sectorOutcome: XfeederSectorOutcome,
        closureEvidence: XfeederClosureEvidence,
        notes: String,
        resultSummary: String
    ) {
        repository.updateSessionSummary(
            sessionId = sessionId,
            status = status,
            sectorOutcome = sectorOutcome,
            closureEvidence = closureEvidence,
            notes = notes,
            resultSummary = resultSummary
        )
    }
}
