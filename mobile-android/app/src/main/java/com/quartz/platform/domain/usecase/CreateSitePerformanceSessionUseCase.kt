package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.PerformanceSession
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.repository.PerformanceSessionRepository
import javax.inject.Inject

class CreateSitePerformanceSessionUseCase @Inject constructor(
    private val repository: PerformanceSessionRepository
) {
    suspend operator fun invoke(
        siteId: String,
        siteCode: String,
        workflowType: PerformanceWorkflowType,
        operatorName: String?,
        technology: String?
    ): PerformanceSession {
        return repository.createSession(
            siteId = siteId,
            siteCode = siteCode,
            workflowType = workflowType,
            operatorName = operatorName,
            technology = technology
        )
    }
}
