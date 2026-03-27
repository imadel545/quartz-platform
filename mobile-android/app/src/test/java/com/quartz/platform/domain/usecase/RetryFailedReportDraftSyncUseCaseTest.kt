package com.quartz.platform.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.ReportSyncTrace
import com.quartz.platform.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RetryFailedReportDraftSyncUseCaseTest {

    @Test
    fun `retry is ignored when state is not FAILED`() = runTest {
        val syncRepository = FakeSyncRepository(
            initial = mapOf(
                "draft-1" to ReportSyncTrace(
                    state = ReportSyncState.LOCAL_ONLY,
                    lastAttemptAtEpochMillis = null,
                    retryCount = 0,
                    failureReason = null
                )
            )
        )
        val useCase = RetryFailedReportDraftSyncUseCase(syncRepository)

        val queued = useCase("draft-1")

        assertThat(queued).isFalse()
        assertThat(syncRepository.enqueuedDraftIds).isEmpty()
    }

    @Test
    fun `retry enqueues upload when state is FAILED`() = runTest {
        val syncRepository = FakeSyncRepository(
            initial = mapOf(
                "draft-2" to ReportSyncTrace(
                    state = ReportSyncState.FAILED,
                    lastAttemptAtEpochMillis = 100L,
                    retryCount = 2,
                    failureReason = "No network"
                )
            )
        )
        val useCase = RetryFailedReportDraftSyncUseCase(syncRepository)

        val queued = useCase("draft-2")

        assertThat(queued).isTrue()
        assertThat(syncRepository.enqueuedDraftIds).containsExactly("draft-2")
    }

    private class FakeSyncRepository(
        initial: Map<String, ReportSyncTrace>
    ) : SyncRepository {
        private val syncStateFlows = initial.mapValues { MutableStateFlow(it.value) }
        val enqueuedDraftIds = mutableListOf<String>()

        override suspend fun enqueueReportUpload(reportDraftId: String) {
            enqueuedDraftIds += reportDraftId
            syncStateFlows[reportDraftId]?.value = ReportSyncTrace(
                state = ReportSyncState.PENDING,
                lastAttemptAtEpochMillis = null,
                retryCount = 0,
                failureReason = null
            )
        }

        override fun observeSyncTrace(reportDraftId: String): Flow<ReportSyncTrace> {
            return syncStateFlows.getValue(reportDraftId)
        }

        override fun observeSyncState(reportDraftId: String): Flow<ReportSyncState> {
            return syncStateFlows.getValue(reportDraftId).map { it.state }
        }

        override fun observePendingJobCount(): Flow<Int> = flowOf(0)

        override suspend fun processPendingJobs(limit: Int): Int = 0
    }
}
