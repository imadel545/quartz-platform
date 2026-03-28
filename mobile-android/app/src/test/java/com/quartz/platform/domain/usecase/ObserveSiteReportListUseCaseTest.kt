package com.quartz.platform.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.ReportSyncTrace
import com.quartz.platform.domain.model.SiteReportListItem
import com.quartz.platform.domain.repository.ReportDraftRepository
import com.quartz.platform.domain.repository.SyncRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveSiteReportListUseCaseTest {

    @Test
    fun `observe use case combines local drafts and sync states`() = runTest {
        val drafts = listOf(
            ReportDraft(
                id = "draft-1",
                siteId = "site-1",
                title = "Rapport 1",
                observation = "",
                revision = 1,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 10L
            ),
            ReportDraft(
                id = "draft-2",
                siteId = "site-1",
                title = "Rapport 2",
                observation = "",
                revision = 2,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 20L
            )
        )
        val reportRepository = FakeReportDraftRepository(drafts)
        val syncRepository = FakeSyncRepository(
            mapOf(
                "draft-1" to ReportSyncTrace(
                    state = ReportSyncState.LOCAL_ONLY,
                    lastAttemptAtEpochMillis = null,
                    retryCount = 0,
                    failureReason = null
                ),
                "draft-2" to ReportSyncTrace(
                    state = ReportSyncState.FAILED,
                    lastAttemptAtEpochMillis = 42L,
                    retryCount = 3,
                    failureReason = "Timeout"
                )
            )
        )
        val useCase = ObserveSiteReportListUseCase(reportRepository, syncRepository)

        val emissions = mutableListOf<List<SiteReportListItem>>()
        val job = launch {
            useCase("site-1").take(2).toList(emissions)
        }

        advanceUntilIdle()
        syncRepository.updateState(
            "draft-2",
            ReportSyncTrace(
                state = ReportSyncState.PENDING,
                lastAttemptAtEpochMillis = 43L,
                retryCount = 3,
                failureReason = null
            )
        )
        advanceUntilIdle()
        job.join()

        assertThat(emissions).hasSize(2)
        assertThat(emissions[0].map { it.syncState }).containsExactly(
            ReportSyncState.LOCAL_ONLY,
            ReportSyncState.FAILED
        ).inOrder()
        assertThat(emissions[1].map { it.syncState }).containsExactly(
            ReportSyncState.LOCAL_ONLY,
            ReportSyncState.PENDING
        ).inOrder()
        assertThat(emissions[0].last().syncTrace.failureReason).isEqualTo("Timeout")
    }

    private class FakeReportDraftRepository(
        initialDrafts: List<ReportDraft>
    ) : ReportDraftRepository {
        private val draftsFlow = MutableStateFlow(initialDrafts)

        override suspend fun createDraft(
            siteId: String,
            originSessionId: String?,
            originSectorId: String?,
            originWorkflowType: ReportDraftOriginWorkflowType?
        ): ReportDraft = draftsFlow.value.first()

        override suspend fun updateDraft(draftId: String, title: String, observation: String): ReportDraft? = null

        override suspend fun findLatestLinkedDraft(
            siteId: String,
            originSessionId: String,
            originWorkflowType: ReportDraftOriginWorkflowType?
        ): ReportDraft? {
            return draftsFlow.value
                .filter { draft ->
                    draft.siteId == siteId &&
                        draft.originSessionId == originSessionId &&
                        draft.originWorkflowType == originWorkflowType
                }
                .maxByOrNull { draft -> draft.updatedAtEpochMillis }
        }

        override fun observeDraft(draftId: String): Flow<ReportDraft?> {
            return draftsFlow.map { drafts -> drafts.firstOrNull { it.id == draftId } }
        }

        override fun listDraftsBySite(siteId: String): Flow<List<ReportDraft>> {
            return draftsFlow.map { drafts -> drafts.filter { it.siteId == siteId } }
        }
    }

    private class FakeSyncRepository(
        initialStates: Map<String, ReportSyncTrace>
    ) : SyncRepository {
        private val stateFlows = initialStates.mapValues { MutableStateFlow(it.value) }

        override suspend fun enqueueReportUpload(reportDraftId: String) = Unit

        override fun observeSyncTrace(reportDraftId: String): Flow<ReportSyncTrace> {
            return stateFlows.getValue(reportDraftId)
        }

        override fun observeSyncState(reportDraftId: String): Flow<ReportSyncState> {
            return stateFlows.getValue(reportDraftId).map { it.state }
        }

        override fun observePendingJobCount(): Flow<Int> = flowOf(0)

        override suspend fun processPendingJobs(limit: Int): Int = 0

        fun updateState(reportDraftId: String, state: ReportSyncTrace) {
            stateFlows.getValue(reportDraftId).value = state
        }
    }
}
