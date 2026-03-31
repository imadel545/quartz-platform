package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.QosRunSummary
import com.quartz.platform.domain.model.ThroughputMetrics
import com.quartz.platform.domain.repository.PerformanceSessionRepository
import javax.inject.Inject

class UpdatePerformanceSessionExecutionUseCase @Inject constructor(
    private val repository: PerformanceSessionRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        status: PerformanceSessionStatus,
        prerequisiteNetworkReady: Boolean,
        prerequisiteBatterySufficient: Boolean,
        prerequisiteLocationReady: Boolean,
        throughputMetrics: ThroughputMetrics,
        qosRunSummary: QosRunSummary,
        notes: String,
        resultSummary: String
    ) {
        repository.updateSessionExecution(
            sessionId = sessionId,
            status = status,
            prerequisiteNetworkReady = prerequisiteNetworkReady,
            prerequisiteBatterySufficient = prerequisiteBatterySufficient,
            prerequisiteLocationReady = prerequisiteLocationReady,
            throughputMetrics = throughputMetrics,
            qosRunSummary = qosRunSummary,
            notes = notes,
            resultSummary = resultSummary
        )
    }
}
