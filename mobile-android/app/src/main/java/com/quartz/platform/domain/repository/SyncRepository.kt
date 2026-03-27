package com.quartz.platform.domain.repository

import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.ReportSyncTrace
import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    suspend fun enqueueReportUpload(reportDraftId: String)
    fun observeSyncTrace(reportDraftId: String): Flow<ReportSyncTrace>
    fun observeSyncState(reportDraftId: String): Flow<ReportSyncState>
    fun observePendingJobCount(): Flow<Int>
    suspend fun processPendingJobs(limit: Int = 10): Int
}
