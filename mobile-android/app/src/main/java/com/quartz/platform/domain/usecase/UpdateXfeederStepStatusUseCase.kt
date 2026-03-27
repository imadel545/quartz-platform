package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.XfeederStepCode
import com.quartz.platform.domain.model.XfeederStepStatus
import com.quartz.platform.domain.repository.XfeederGuidedSessionRepository
import javax.inject.Inject

class UpdateXfeederStepStatusUseCase @Inject constructor(
    private val repository: XfeederGuidedSessionRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        stepCode: XfeederStepCode,
        status: XfeederStepStatus
    ) {
        repository.updateStepStatus(
            sessionId = sessionId,
            stepCode = stepCode,
            status = status
        )
    }
}
