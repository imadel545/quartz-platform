package com.quartz.platform.domain.repository

import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    fun observePendingJobCount(): Flow<Int>
    suspend fun enqueueReportUpload(reportId: String)
    suspend fun processPendingJobs(limit: Int = 10): Int
}
