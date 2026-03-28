package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.RetResultOutcome
import com.quartz.platform.domain.model.RetSessionStatus
import com.quartz.platform.domain.repository.RetGuidedSessionRepository
import javax.inject.Inject

class UpdateRetSessionSummaryUseCase @Inject constructor(
    private val repository: RetGuidedSessionRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        status: RetSessionStatus,
        resultOutcome: RetResultOutcome,
        notes: String,
        resultSummary: String
    ) {
        repository.updateSessionSummary(
            sessionId = sessionId,
            status = status,
            resultOutcome = resultOutcome,
            notes = notes,
            resultSummary = resultSummary
        )
    }
}
