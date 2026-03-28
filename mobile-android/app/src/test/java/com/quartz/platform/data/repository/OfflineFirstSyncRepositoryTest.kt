package com.quartz.platform.data.repository

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.core.dispatchers.DispatcherProvider
import com.quartz.platform.core.logging.AppLogger
import com.quartz.platform.data.local.dao.ReportDraftDao
import com.quartz.platform.data.local.dao.SyncJobDao
import com.quartz.platform.data.local.entity.ReportDraftEntity
import com.quartz.platform.data.local.entity.SyncJobEntity
import com.quartz.platform.data.remote.SyncGateway
import com.quartz.platform.data.remote.SyncPushResult
import com.quartz.platform.data.sync.SyncOrchestrator
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.SyncAggregateType
import com.quartz.platform.domain.model.SyncJobStatus
import com.quartz.platform.domain.model.SyncOperationType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstSyncRepositoryTest {

    @Test
    fun `enqueue skips duplicate actionable report upload jobs`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val syncJobDao = InMemorySyncJobDao()
        val syncOrchestrator = CountingSyncOrchestrator()
        val repository = OfflineFirstSyncRepository(
            syncJobDao = syncJobDao,
            reportDraftDao = InMemoryReportDraftDao(
                initialDrafts = listOf(
                    ReportDraftEntity(
                        id = "report-1",
                        siteId = "site-1",
                        title = "Draft",
                        observation = "",
                        revision = 1,
                        createdAtEpochMillis = 1L,
                        updatedAtEpochMillis = 1L,
                        originSessionId = null,
                        originSectorId = null,
                        originWorkflowType = null
                    )
                )
            ),
            syncGateway = FakeGateway(SyncPushResult.Success),
            syncOrchestrator = syncOrchestrator,
            dispatcherProvider = TestDispatchers(testDispatcher),
            appLogger = NoOpLogger()
        )

        repository.enqueueReportUpload("report-1")
        repository.enqueueReportUpload("report-1")

        assertThat(syncJobDao.snapshot().size).isEqualTo(1)
        assertThat(syncOrchestrator.immediateScheduleCount).isEqualTo(1)
    }

    @Test
    fun `process marks sync job success when gateway succeeds`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val syncJobDao = InMemorySyncJobDao()
        val repository = OfflineFirstSyncRepository(
            syncJobDao = syncJobDao,
            reportDraftDao = InMemoryReportDraftDao(
                initialDrafts = listOf(
                    ReportDraftEntity(
                        id = "report-success",
                        siteId = "site-1",
                        title = "Draft",
                        observation = "",
                        revision = 1,
                        createdAtEpochMillis = 1L,
                        updatedAtEpochMillis = 1L,
                        originSessionId = null,
                        originSectorId = null,
                        originWorkflowType = null
                    )
                )
            ),
            syncGateway = FakeGateway(SyncPushResult.Success),
            syncOrchestrator = NoOpSyncOrchestrator(),
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
        val testDispatcher = StandardTestDispatcher(testScheduler)
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
            reportDraftDao = InMemoryReportDraftDao(
                initialDrafts = listOf(
                    ReportDraftEntity(
                        id = "report-retry-budget",
                        siteId = "site-1",
                        title = "Draft",
                        observation = "",
                        revision = 5,
                        createdAtEpochMillis = 1L,
                        updatedAtEpochMillis = 1L,
                        originSessionId = null,
                        originSectorId = null,
                        originWorkflowType = null
                    )
                )
            ),
            syncGateway = FakeGateway(SyncPushResult.RetryableFailure("temporary")),
            syncOrchestrator = NoOpSyncOrchestrator(),
            dispatcherProvider = TestDispatchers(testDispatcher),
            appLogger = NoOpLogger()
        )

        repository.processPendingJobs(limit = 10)

        val job = syncJobDao.snapshot().single()
        assertThat(job.status).isEqualTo(SyncJobStatus.FAILED_TERMINAL)
        assertThat(job.retryCount).isEqualTo(5)
    }

    @Test
    fun `sync state transitions from local to pending to synced`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val syncJobDao = InMemorySyncJobDao()
        val repository = OfflineFirstSyncRepository(
            syncJobDao = syncJobDao,
            reportDraftDao = InMemoryReportDraftDao(
                initialDrafts = listOf(
                    ReportDraftEntity(
                        id = "draft-sync-state",
                        siteId = "site-1",
                        title = "Draft",
                        observation = "",
                        revision = 1,
                        createdAtEpochMillis = 1L,
                        updatedAtEpochMillis = 1L,
                        originSessionId = null,
                        originSectorId = null,
                        originWorkflowType = null
                    )
                )
            ),
            syncGateway = FakeGateway(SyncPushResult.Success),
            syncOrchestrator = NoOpSyncOrchestrator(),
            dispatcherProvider = TestDispatchers(testDispatcher),
            appLogger = NoOpLogger()
        )

        assertThat(repository.observeSyncState("draft-sync-state").first()).isEqualTo(ReportSyncState.LOCAL_ONLY)

        repository.enqueueReportUpload("draft-sync-state")
        assertThat(repository.observeSyncState("draft-sync-state").first()).isEqualTo(ReportSyncState.PENDING)

        repository.processPendingJobs(limit = 10)
        assertThat(repository.observeSyncState("draft-sync-state").first()).isEqualTo(ReportSyncState.SYNCED)
    }

    @Test
    fun `terminal failure exposes sync traceability metadata`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val syncJobDao = InMemorySyncJobDao()
        val repository = OfflineFirstSyncRepository(
            syncJobDao = syncJobDao,
            reportDraftDao = InMemoryReportDraftDao(
                initialDrafts = listOf(
                    ReportDraftEntity(
                        id = "draft-failure-trace",
                        siteId = "site-1",
                        title = "Draft",
                        observation = "",
                        revision = 1,
                        createdAtEpochMillis = 1L,
                        updatedAtEpochMillis = 1L,
                        originSessionId = null,
                        originSectorId = null,
                        originWorkflowType = null
                    )
                )
            ),
            syncGateway = FakeGateway(
                SyncPushResult.TerminalFailure(
                    "Very long failure reason that should remain concise and useful for field operators and reviewers."
                )
            ),
            syncOrchestrator = NoOpSyncOrchestrator(),
            dispatcherProvider = TestDispatchers(testDispatcher),
            appLogger = NoOpLogger()
        )

        repository.enqueueReportUpload("draft-failure-trace")
        repository.processPendingJobs(limit = 10)

        val trace = repository.observeSyncTrace("draft-failure-trace").first()
        assertThat(trace.state).isEqualTo(ReportSyncState.FAILED)
        assertThat(trace.retryCount).isEqualTo(1)
        assertThat(trace.lastAttemptAtEpochMillis).isNotNull()
        assertThat(trace.failureReason).contains("Very long failure reason")
        assertThat(trace.failureReason!!.length).isAtMost(140)
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

    private class NoOpSyncOrchestrator : SyncOrchestrator {
        override fun scheduleRecurringSync() = Unit
        override fun scheduleImmediateSync() = Unit
    }

    private class CountingSyncOrchestrator : SyncOrchestrator {
        var immediateScheduleCount: Int = 0

        override fun scheduleRecurringSync() = Unit

        override fun scheduleImmediateSync() {
            immediateScheduleCount += 1
        }
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

        override fun observeLatestJob(
            aggregateType: SyncAggregateType,
            aggregateId: String,
            operationType: SyncOperationType
        ): Flow<SyncJobEntity?> {
            return jobs.map { jobMap ->
                jobMap.values
                    .filter { it.aggregateType == aggregateType && it.aggregateId == aggregateId && it.operationType == operationType }
                    .maxByOrNull { it.createdAtEpochMillis }
            }
        }

        override suspend fun getLatestJob(
            aggregateType: SyncAggregateType,
            aggregateId: String,
            operationType: SyncOperationType
        ): SyncJobEntity? {
            return jobs.value.values
                .filter { it.aggregateType == aggregateType && it.aggregateId == aggregateId && it.operationType == operationType }
                .maxByOrNull { it.createdAtEpochMillis }
        }

        override suspend fun markActionableAsSuperseded(
            aggregateType: SyncAggregateType,
            aggregateId: String,
            operationType: SyncOperationType,
            reason: String,
            updatedAtEpochMillis: Long
        ) {
            val updated = jobs.value.mapValues { (_, entity) ->
                if (
                    entity.aggregateType == aggregateType &&
                    entity.aggregateId == aggregateId &&
                    entity.operationType == operationType &&
                    (entity.status == SyncJobStatus.PENDING ||
                        entity.status == SyncJobStatus.IN_FLIGHT ||
                        entity.status == SyncJobStatus.FAILED_RETRYABLE)
                ) {
                    entity.copy(
                        status = SyncJobStatus.FAILED_TERMINAL,
                        nextAttemptAtEpochMillis = null,
                        lastAttemptAtEpochMillis = updatedAtEpochMillis,
                        lastError = reason
                    )
                } else {
                    entity
                }
            }
            jobs.value = updated
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

    private class InMemoryReportDraftDao(
        initialDrafts: List<ReportDraftEntity> = emptyList()
    ) : ReportDraftDao {
        private val drafts = MutableStateFlow(initialDrafts.associateBy { it.id })

        override suspend fun insert(entity: ReportDraftEntity) {
            drafts.value = drafts.value + (entity.id to entity)
        }

        override fun observeById(draftId: String): Flow<ReportDraftEntity?> {
            return drafts.map { draftMap -> draftMap[draftId] }
        }

        override fun listBySite(siteId: String): Flow<List<ReportDraftEntity>> {
            return drafts.map { draftMap ->
                draftMap.values.filter { it.siteId == siteId }.sortedByDescending { it.updatedAtEpochMillis }
            }
        }

        override suspend fun getById(draftId: String): ReportDraftEntity? = drafts.value[draftId]

        override suspend fun findLatestLinkedBySession(
            siteId: String,
            originSessionId: String,
            originWorkflowType: String?
        ): ReportDraftEntity? {
            return drafts.value.values
                .asSequence()
                .filter { entity ->
                    entity.siteId == siteId &&
                        entity.originSessionId == originSessionId &&
                        entity.originWorkflowType == originWorkflowType
                }
                .maxByOrNull { entity -> entity.updatedAtEpochMillis }
        }

        override suspend fun getRevision(draftId: String): Int? = drafts.value[draftId]?.revision

        override suspend fun updateDraft(
            draftId: String,
            title: String,
            observation: String,
            revision: Int,
            updatedAtEpochMillis: Long
        ) {
            val existing = drafts.value[draftId] ?: return
            drafts.value = drafts.value + (
                draftId to existing.copy(
                    title = title,
                    observation = observation,
                    revision = revision,
                    updatedAtEpochMillis = updatedAtEpochMillis
                )
            )
        }
    }
}
