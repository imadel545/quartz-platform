package com.quartz.platform.domain.repository

import com.quartz.platform.domain.model.PerformanceGuidedStep
import com.quartz.platform.domain.model.PerformanceSession
import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.PerformanceStepCode
import com.quartz.platform.domain.model.PerformanceStepStatus
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.model.QosRunSummary
import com.quartz.platform.domain.model.ThroughputMetrics
import kotlinx.coroutines.flow.Flow

interface PerformanceSessionRepository {
    fun observeSiteSessionHistory(siteId: String): Flow<List<PerformanceSession>>
    fun observeLatestSiteSession(siteId: String, workflowType: PerformanceWorkflowType): Flow<PerformanceSession?>
    suspend fun createSession(
        siteId: String,
        siteCode: String,
        workflowType: PerformanceWorkflowType,
        operatorName: String?,
        technology: String?
    ): PerformanceSession

    suspend fun updateStepStatus(
        sessionId: String,
        stepCode: PerformanceStepCode,
        status: PerformanceStepStatus
    )

    suspend fun updateSessionExecution(
        sessionId: String,
        status: PerformanceSessionStatus,
        prerequisiteNetworkReady: Boolean,
        prerequisiteBatterySufficient: Boolean,
        prerequisiteLocationReady: Boolean,
        throughputMetrics: ThroughputMetrics,
        qosRunSummary: QosRunSummary,
        notes: String,
        resultSummary: String
    )
}
