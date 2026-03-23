package com.quartz.platform.data.repository

import com.quartz.platform.core.dispatchers.DispatcherProvider
import com.quartz.platform.core.logging.AppLogger
import com.quartz.platform.data.local.dao.SyncJobDao
import com.quartz.platform.data.local.entity.SyncJobEntity
import com.quartz.platform.data.local.mapper.toDomain
import com.quartz.platform.data.remote.SyncGateway
import com.quartz.platform.data.remote.SyncPushResult
import com.quartz.platform.domain.model.SyncAggregateType
import com.quartz.platform.domain.model.SyncOperationType
import com.quartz.platform.domain.model.SyncJobStatus
import com.quartz.platform.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class OfflineFirstSyncRepository @Inject constructor(
    private val syncJobDao: SyncJobDao,
    private val syncGateway: SyncGateway,
    private val dispatcherProvider: DispatcherProvider,
    private val appLogger: AppLogger
) : SyncRepository {

    override fun observePendingJobCount(): Flow<Int> = syncJobDao.observePendingCount()

    override suspend fun enqueueReportUpload(reportId: String) {
        val now = System.currentTimeMillis()
        val aggregateType = SyncAggregateType.REPORT
        val operationType = SyncOperationType.UPLOAD

        val hasExistingActionableJob = withContext(dispatcherProvider.io) {
            syncJobDao.hasActionableJob(
                aggregateType = aggregateType,
                aggregateId = reportId,
                operationType = operationType
            )
        }
        if (hasExistingActionableJob) {
            appLogger.warn(TAG, "Skipped enqueue, actionable job already exists for reportId=$reportId")
            return
        }

        val job = SyncJobEntity(
            id = UUID.randomUUID().toString(),
            aggregateType = aggregateType,
            aggregateId = reportId,
            operationType = operationType,
            payloadReference = reportId,
            status = SyncJobStatus.PENDING,
            retryCount = 0,
            nextAttemptAtEpochMillis = now,
            lastAttemptAtEpochMillis = null,
            lastError = null,
            createdAtEpochMillis = now
        )
        withContext(dispatcherProvider.io) {
            syncJobDao.insert(job)
        }
    }

    override suspend fun processPendingJobs(limit: Int): Int = withContext(dispatcherProvider.io) {
        val now = System.currentTimeMillis()
        val pendingJobs = syncJobDao.getPendingJobs(currentTimeMillis = now, limit = limit)
        if (pendingJobs.isEmpty()) return@withContext 0

        var processed = 0
        pendingJobs.forEach { jobEntity ->
            val inFlightAt = System.currentTimeMillis()
            syncJobDao.updateStatus(
                jobId = jobEntity.id,
                status = SyncJobStatus.IN_FLIGHT,
                retryCount = jobEntity.retryCount,
                nextAttemptAtEpochMillis = null,
                lastAttemptAtEpochMillis = inFlightAt,
                lastError = null
            )

            when (val result = syncGateway.push(jobEntity.toDomain())) {
                SyncPushResult.Success -> {
                    syncJobDao.updateStatus(
                        jobId = jobEntity.id,
                        status = SyncJobStatus.SUCCEEDED,
                        retryCount = jobEntity.retryCount,
                        nextAttemptAtEpochMillis = null,
                        lastAttemptAtEpochMillis = System.currentTimeMillis(),
                        lastError = null
                    )
                    processed += 1
                }

                is SyncPushResult.RetryableFailure -> {
                    val nextRetryCount = jobEntity.retryCount + 1
                    if (nextRetryCount >= MAX_RETRY_ATTEMPTS) {
                        syncJobDao.updateStatus(
                            jobId = jobEntity.id,
                            status = SyncJobStatus.FAILED_TERMINAL,
                            retryCount = nextRetryCount,
                            nextAttemptAtEpochMillis = null,
                            lastAttemptAtEpochMillis = System.currentTimeMillis(),
                            lastError = "Retry budget exhausted: ${result.reason}"
                        )
                        return@forEach
                    }

                    val nextAttempt = System.currentTimeMillis() + backoffForAttempt(nextRetryCount)
                    syncJobDao.updateStatus(
                        jobId = jobEntity.id,
                        status = SyncJobStatus.FAILED_RETRYABLE,
                        retryCount = nextRetryCount,
                        nextAttemptAtEpochMillis = nextAttempt,
                        lastAttemptAtEpochMillis = System.currentTimeMillis(),
                        lastError = result.reason
                    )
                }

                is SyncPushResult.TerminalFailure -> {
                    syncJobDao.updateStatus(
                        jobId = jobEntity.id,
                        status = SyncJobStatus.FAILED_TERMINAL,
                        retryCount = jobEntity.retryCount + 1,
                        nextAttemptAtEpochMillis = null,
                        lastAttemptAtEpochMillis = System.currentTimeMillis(),
                        lastError = result.reason
                    )
                }
            }
        }

        appLogger.info(TAG, "Processed $processed sync jobs over ${pendingJobs.size} candidates")
        processed
    }

    private companion object {
        const val TAG = "OfflineFirstSyncRepository"
        const val BASE_RETRY_BACKOFF_MILLIS = 30_000L
        const val MAX_RETRY_BACKOFF_MILLIS = 10 * 60_000L
        const val MAX_RETRY_ATTEMPTS = 5
    }

    private fun backoffForAttempt(retryAttempt: Int): Long {
        val exponentialFactor = 1L shl (retryAttempt - 1).coerceAtLeast(0)
        return (BASE_RETRY_BACKOFF_MILLIS * exponentialFactor).coerceAtMost(MAX_RETRY_BACKOFF_MILLIS)
    }
}
