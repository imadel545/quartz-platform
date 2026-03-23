package com.quartz.platform.data.repository

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.core.dispatchers.DispatcherProvider
import com.quartz.platform.core.logging.AppLogger
import com.quartz.platform.data.local.dao.SyncJobDao
import com.quartz.platform.data.local.entity.SyncJobEntity
import com.quartz.platform.data.remote.SyncGateway
import com.quartz.platform.data.remote.SyncPushResult
import com.quartz.platform.domain.model.SyncAggregateType
import com.quartz.platform.domain.model.SyncJobStatus
import com.quartz.platform.domain.model.SyncOperationType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstSyncRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `enqueue skips duplicate actionable report upload jobs`() = runTest {
        val syncJobDao = InMemorySyncJobDao()
        val repository = OfflineFirstSyncRepository(
            syncJobDao = syncJobDao,
            syncGateway = FakeGateway(SyncPushResult.Success),
            dispatcherProvider = TestDispatchers(testDispatcher),
            appLogger = NoOpLogger()
        )

        repository.enqueueReportUpload("report-1")
        repository.enqueueReportUpload("report-1")

        assertThat(syncJobDao.snapshot().size).isEqualTo(1)
    }

    @Test
    fun `process marks sync job success when gateway succeeds`() = runTest {
        val syncJobDao = InMemorySyncJobDao()
        val repository = OfflineFirstSyncRepository(
            syncJobDao = syncJobDao,
            syncGateway = FakeGateway(SyncPushResult.Success),
            dispatcherProvider = TestDispatchers(testDispatcher),
            appLogger = NoOpLogger()
        )

        repository.enqueueReportUpload("report-success")
        val processed = repository.processPendingJobs(limit = 10)

        assertThat(processed).isEqualTo(1)
        assertThat(syncJobDao.snapshot().single().status).isEqualTo(SyncJobStatus.SUCCEEDED)
    }

    @Test
    fun `retryable failure becomes terminal when retry budget is exhausted`() = runTest {
        val syncJobDao = InMemorySyncJobDao(
            initialJobs = listOf(
                SyncJobEntity(
                    id = UUID.randomUUID().toString(),
                    aggregateType = SyncAggregateType.REPORT,
                    aggregateId = "report-retry-budget",
                    operationType = SyncOperationType.UPLOAD,
                    payloadReference = "report-retry-budget",
                    status = SyncJobStatus.PENDING,
                    retryCount = 4,
                    nextAttemptAtEpochMillis = System.currentTimeMillis(),
                    lastAttemptAtEpochMillis = null,
                    lastError = null,
                    createdAtEpochMillis = System.currentTimeMillis()
                )
            )
        )

        val repository = OfflineFirstSyncRepository(
            syncJobDao = syncJobDao,
            syncGateway = FakeGateway(SyncPushResult.RetryableFailure("temporary")),
            dispatcherProvider = TestDispatchers(testDispatcher),
            appLogger = NoOpLogger()
        )

        repository.processPendingJobs(limit = 10)

        val job = syncJobDao.snapshot().single()
        assertThat(job.status).isEqualTo(SyncJobStatus.FAILED_TERMINAL)
        assertThat(job.retryCount).isEqualTo(5)
    }

    private class TestDispatchers(
        private val dispatcher: CoroutineDispatcher
    ) : DispatcherProvider {
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = Dispatchers.Main
    }

    private class NoOpLogger : AppLogger {
        override fun info(tag: String, message: String) = Unit
        override fun warn(tag: String, message: String) = Unit
        override fun error(tag: String, message: String, throwable: Throwable?) = Unit
    }

    private class FakeGateway(
        private val result: SyncPushResult
    ) : SyncGateway {
        override suspend fun push(job: com.quartz.platform.domain.model.SyncJob): SyncPushResult = result
    }

    private class InMemorySyncJobDao(
        initialJobs: List<SyncJobEntity> = emptyList()
    ) : SyncJobDao {
        private val jobs = MutableStateFlow(initialJobs.associateBy { it.id })

        override fun observePendingCount(): Flow<Int> {
            return jobs.map { jobMap ->
                jobMap.values.count {
                    it.status == SyncJobStatus.PENDING ||
                        it.status == SyncJobStatus.IN_FLIGHT ||
                        it.status == SyncJobStatus.FAILED_RETRYABLE
                }
            }
        }

        override suspend fun getPendingJobs(currentTimeMillis: Long, limit: Int): List<SyncJobEntity> {
            return jobs.value.values
                .filter { entity ->
                    entity.status == SyncJobStatus.PENDING ||
                        (entity.status == SyncJobStatus.FAILED_RETRYABLE &&
                            (entity.nextAttemptAtEpochMillis == null || entity.nextAttemptAtEpochMillis <= currentTimeMillis))
                }
                .sortedBy { it.createdAtEpochMillis }
                .take(limit)
        }

        override suspend fun insert(job: SyncJobEntity) {
            jobs.value = jobs.value + (job.id to job)
        }

        override suspend fun hasActionableJob(
            aggregateType: SyncAggregateType,
            aggregateId: String,
            operationType: SyncOperationType
        ): Boolean {
            return jobs.value.values.any { entity ->
                entity.aggregateType == aggregateType &&
                    entity.aggregateId == aggregateId &&
                    entity.operationType == operationType &&
                    (entity.status == SyncJobStatus.PENDING ||
                        entity.status == SyncJobStatus.IN_FLIGHT ||
                        entity.status == SyncJobStatus.FAILED_RETRYABLE)
            }
        }

        override suspend fun updateStatus(
            jobId: String,
            status: SyncJobStatus,
            retryCount: Int,
            nextAttemptAtEpochMillis: Long?,
            lastAttemptAtEpochMillis: Long?,
            lastError: String?
        ) {
            val existing = jobs.value[jobId] ?: return
            val updated = existing.copy(
                status = status,
                retryCount = retryCount,
                nextAttemptAtEpochMillis = nextAttemptAtEpochMillis,
                lastAttemptAtEpochMillis = lastAttemptAtEpochMillis,
                lastError = lastError
            )
            jobs.value = jobs.value + (jobId to updated)
        }

        fun snapshot(): List<SyncJobEntity> = jobs.value.values.toList()
    }
}
