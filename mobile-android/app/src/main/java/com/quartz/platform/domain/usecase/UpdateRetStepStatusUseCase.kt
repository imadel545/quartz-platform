package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.RetStepCode
import com.quartz.platform.domain.model.RetStepStatus
import com.quartz.platform.domain.repository.RetGuidedSessionRepository
import javax.inject.Inject

class UpdateRetStepStatusUseCase @Inject constructor(
    private val repository: RetGuidedSessionRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        stepCode: RetStepCode,
        status: RetStepStatus
    ) {
        repository.updateStepStatus(
            sessionId = sessionId,
            stepCode = stepCode,
            status = status
        )
    }
}
