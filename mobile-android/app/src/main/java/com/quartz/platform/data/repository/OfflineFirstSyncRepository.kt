package com.quartz.platform.data.repository

import com.quartz.platform.core.dispatchers.DispatcherProvider
import com.quartz.platform.core.logging.AppLogger
import com.quartz.platform.data.local.dao.ReportDraftDao
import com.quartz.platform.data.local.dao.SyncJobDao
import com.quartz.platform.data.local.entity.SyncJobEntity
import com.quartz.platform.data.local.mapper.toDomain
import com.quartz.platform.data.remote.SyncGateway
import com.quartz.platform.data.remote.SyncPushResult
import com.quartz.platform.data.sync.SyncOrchestrator
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.ReportSyncTrace
import com.quartz.platform.domain.model.SyncAggregateType
import com.quartz.platform.domain.model.SyncJobStatus
import com.quartz.platform.domain.model.SyncOperationType
import com.quartz.platform.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class OfflineFirstSyncRepository @Inject constructor(
    private val syncJobDao: SyncJobDao,
    private val reportDraftDao: ReportDraftDao,
    private val syncGateway: SyncGateway,
    private val syncOrchestrator: SyncOrchestrator,
    private val dispatcherProvider: DispatcherProvider,
    private val appLogger: AppLogger
) : SyncRepository {

    override fun observePendingJobCount(): Flow<Int> = syncJobDao.observePendingCount()

    override fun observeSyncTrace(reportDraftId: String): Flow<ReportSyncTrace> {
        val aggregateType = SyncAggregateType.REPORT
        val operationType = SyncOperationType.UPLOAD

        return combine(
            reportDraftDao.observeById(reportDraftId),
            syncJobDao.observeLatestJob(aggregateType, reportDraftId, operationType)
        ) { draft, latestJob ->
            if (draft == null) return@combine ReportSyncTrace.localOnly()
            if (latestJob == null) return@combine ReportSyncTrace.localOnly()

            val jobRevision = extractRevision(latestJob.payloadReference)
            if (jobRevision == null || jobRevision != draft.revision) {
                return@combine ReportSyncTrace.localOnly()
            }

            val state = when (latestJob.status) {
                SyncJobStatus.PENDING,
                SyncJobStatus.IN_FLIGHT,
                SyncJobStatus.FAILED_RETRYABLE -> ReportSyncState.PENDING

                SyncJobStatus.SUCCEEDED -> ReportSyncState.SYNCED
                SyncJobStatus.FAILED_TERMINAL -> ReportSyncState.FAILED
            }

            ReportSyncTrace(
                state = state,
                lastAttemptAtEpochMillis = latestJob.lastAttemptAtEpochMillis,
                retryCount = latestJob.retryCount,
                failureReason = summarizeFailureReason(latestJob.lastError)
            )
        }
    }

    override fun observeSyncState(reportDraftId: String): Flow<ReportSyncState> {
        return observeSyncTrace(reportDraftId).map { trace -> trace.state }
    }

    override suspend fun enqueueReportUpload(reportDraftId: String) {
        val now = System.currentTimeMillis()
        val aggregateType = SyncAggregateType.REPORT
        val operationType = SyncOperationType.UPLOAD

        val revision = withContext(dispatcherProvider.io) { reportDraftDao.getRevision(reportDraftId) }
        if (revision == null) {
            appLogger.warn(TAG, "Skipped enqueue, report draft not found: $reportDraftId")
            return
        }

        val payloadReference = buildPayloadReference(reportDraftId, revision)

        val latestJob = withContext(dispatcherProvider.io) {
            syncJobDao.getLatestJob(
                aggregateType = aggregateType,
                aggregateId = reportDraftId,
                operationType = operationType
            )
        }

        if (latestJob != null &&
            latestJob.payloadReference == payloadReference &&
            latestJob.status in ACTIONABLE_STATES
        ) {
            appLogger.info(TAG, "Skipped enqueue, same revision already actionable for reportDraftId=$reportDraftId")
            return
        }

        withContext(dispatcherProvider.io) {
            syncJobDao.markActionableAsSuperseded(
                aggregateType = aggregateType,
                aggregateId = reportDraftId,
                operationType = operationType,
                reason = "Superseded by new draft revision",
                updatedAtEpochMillis = now
            )

            syncJobDao.insert(
                SyncJobEntity(
                    id = UUID.randomUUID().toString(),
                    aggregateType = aggregateType,
                    aggregateId = reportDraftId,
                    operationType = operationType,
                    payloadReference = payloadReference,
                    status = SyncJobStatus.PENDING,
                    retryCount = 0,
                    nextAttemptAtEpochMillis = now,
                    lastAttemptAtEpochMillis = null,
                    lastError = null,
                    createdAtEpochMillis = now
                )
            )
        }

        // Trigger an immediate background pass for demo/runtime responsiveness.
        syncOrchestrator.scheduleImmediateSync()
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
                            lastError = summarizeFailureReason("Retry budget exhausted: ${result.reason}")
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
                        lastError = summarizeFailureReason(result.reason)
                    )
                }

                is SyncPushResult.TerminalFailure -> {
                    syncJobDao.updateStatus(
                        jobId = jobEntity.id,
                        status = SyncJobStatus.FAILED_TERMINAL,
                        retryCount = jobEntity.retryCount + 1,
                        nextAttemptAtEpochMillis = null,
                        lastAttemptAtEpochMillis = System.currentTimeMillis(),
                        lastError = summarizeFailureReason(result.reason)
                    )
                }
            }
        }

        appLogger.info(TAG, "Processed $processed sync jobs over ${pendingJobs.size} candidates")
        processed
    }

    private fun buildPayloadReference(reportId: String, revision: Int): String {
        return "report_draft:$reportId:rev:$revision"
    }

    private fun extractRevision(payloadReference: String?): Int? {
        if (payloadReference == null) return null
        val revisionToken = payloadReference.substringAfterLast(':', missingDelimiterValue = "")
        return revisionToken.toIntOrNull()
    }

    private fun backoffForAttempt(retryAttempt: Int): Long {
        val exponentialFactor = 1L shl (retryAttempt - 1).coerceAtLeast(0)
        return (BASE_RETRY_BACKOFF_MILLIS * exponentialFactor).coerceAtMost(MAX_RETRY_BACKOFF_MILLIS)
    }

    private fun summarizeFailureReason(rawReason: String?): String? {
        if (rawReason.isNullOrBlank()) return null
        return rawReason.trim().replace(Regex("\\s+"), " ").take(MAX_ERROR_LENGTH)
    }

    private companion object {
        const val TAG = "OfflineFirstSyncRepository"
        const val BASE_RETRY_BACKOFF_MILLIS = 30_000L
        const val MAX_RETRY_BACKOFF_MILLIS = 10 * 60_000L
        const val MAX_RETRY_ATTEMPTS = 5
        const val MAX_ERROR_LENGTH = 140
        val ACTIONABLE_STATES = setOf(
            SyncJobStatus.PENDING,
            SyncJobStatus.IN_FLIGHT,
            SyncJobStatus.FAILED_RETRYABLE
        )
    }
}
