package com.quartz.platform.presentation.report.list

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.quartz.platform.MainDispatcherRule
import com.quartz.platform.TestUiStrings
import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.ReportSyncTrace
import com.quartz.platform.domain.repository.ReportDraftRepository
import com.quartz.platform.domain.repository.SyncRepository
import com.quartz.platform.domain.usecase.ObserveSiteReportListUseCase
import com.quartz.platform.domain.usecase.RetryFailedReportDraftSyncUseCase
import com.quartz.platform.presentation.navigation.QuartzDestination
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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun retry_failed_report_enqueues_sync() = runTest {
        val reportDraftRepository = FakeReportDraftRepository(
            initial = listOf(
                ReportDraft(
                    id = "draft-1",
                    siteId = "site-1",
                    title = "Rapport terrain",
                    observation = "Obs",
                    revision = 1,
                    createdAtEpochMillis = 1L,
                    updatedAtEpochMillis = 2L
                )
            )
        )
        val syncRepository = FakeSyncRepository(
            initial = mapOf(
                "draft-1" to ReportSyncTrace(
                    state = ReportSyncState.FAILED,
                    lastAttemptAtEpochMillis = 10L,
                    retryCount = 2,
                    failureReason = "Timeout"
                )
            )
        )

        val viewModel = ReportListViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(QuartzDestination.ReportList.ARG_SITE_ID to "site-1")
            ),
            observeSiteReportListUseCase = ObserveSiteReportListUseCase(reportDraftRepository, syncRepository),
            retryFailedReportDraftSyncUseCase = RetryFailedReportDraftSyncUseCase(syncRepository),
            uiStrings = TestUiStrings()
        )

        advanceUntilIdle()
        viewModel.onRetryFailedSyncClicked("draft-1")
        advanceUntilIdle()

        assertThat(syncRepository.enqueuedDraftIds).containsExactly("draft-1")
        assertThat(viewModel.uiState.value.reports.single().syncState).isEqualTo(ReportSyncState.PENDING)
        assertThat(viewModel.uiState.value.infoMessage).contains("Synchronisation relancée")
    }

    @Test
    fun open_draft_emits_navigation_event() = runTest {
        val reportDraftRepository = FakeReportDraftRepository(initial = emptyList())
        val syncRepository = FakeSyncRepository(initial = emptyMap())

        val viewModel = ReportListViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(QuartzDestination.ReportList.ARG_SITE_ID to "site-42")
            ),
            observeSiteReportListUseCase = ObserveSiteReportListUseCase(reportDraftRepository, syncRepository),
            retryFailedReportDraftSyncUseCase = RetryFailedReportDraftSyncUseCase(syncRepository),
            uiStrings = TestUiStrings()
        )

        val events = mutableListOf<ReportListEvent>()
        val job = launch {
            viewModel.events.take(1).toList(events)
        }
        advanceUntilIdle()
        viewModel.onOpenDraftClicked("draft-42")
        advanceUntilIdle()
        job.join()

        assertThat(events).containsExactly(ReportListEvent.OpenDraft("draft-42"))
    }

    private class FakeReportDraftRepository(
        initial: List<ReportDraft>
    ) : ReportDraftRepository {
        private val draftsFlow = MutableStateFlow(initial)

        override suspend fun createDraft(
            siteId: String,
            originSessionId: String?,
            originSectorId: String?
        ): ReportDraft = draftsFlow.value.first()

        override suspend fun updateDraft(draftId: String, title: String, observation: String): ReportDraft? = null

        override suspend fun findLatestLinkedDraft(siteId: String, originSessionId: String): ReportDraft? {
            return draftsFlow.value
                .filter { draft -> draft.siteId == siteId && draft.originSessionId == originSessionId }
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
        initial: Map<String, ReportSyncTrace>
    ) : SyncRepository {
        private val states = initial.mapValues { MutableStateFlow(it.value) }.toMutableMap()
        val enqueuedDraftIds = mutableListOf<String>()

        override suspend fun enqueueReportUpload(reportDraftId: String) {
            enqueuedDraftIds += reportDraftId
            states[reportDraftId]?.value = ReportSyncTrace(
                state = ReportSyncState.PENDING,
                lastAttemptAtEpochMillis = null,
                retryCount = 0,
                failureReason = null
            )
        }

        override fun observeSyncTrace(reportDraftId: String): Flow<ReportSyncTrace> {
            val flow = states.getOrPut(reportDraftId) { MutableStateFlow(ReportSyncTrace.localOnly()) }
            return flow
        }

        override fun observeSyncState(reportDraftId: String): Flow<ReportSyncState> {
            val flow = states.getOrPut(reportDraftId) { MutableStateFlow(ReportSyncTrace.localOnly()) }
            return flow.map { it.state }
        }

        override fun observePendingJobCount(): Flow<Int> = flowOf(0)

        override suspend fun processPendingJobs(limit: Int): Int = 0
    }
}
