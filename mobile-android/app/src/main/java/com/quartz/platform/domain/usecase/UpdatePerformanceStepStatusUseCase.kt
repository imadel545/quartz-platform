package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.PerformanceStepCode
import com.quartz.platform.domain.model.PerformanceStepStatus
import com.quartz.platform.domain.repository.PerformanceSessionRepository
import javax.inject.Inject

class UpdatePerformanceStepStatusUseCase @Inject constructor(
    private val repository: PerformanceSessionRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        stepCode: PerformanceStepCode,
        status: PerformanceStepStatus
    ) {
        repository.updateStepStatus(
            sessionId = sessionId,
            stepCode = stepCode,
            status = status
        )
    }
}
