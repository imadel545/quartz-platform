package com.quartz.platform.presentation.reviewer.controltower

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.quartz.platform.MainDispatcherRule
import com.quartz.platform.TestUiStrings
import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.ReportSyncTrace
import com.quartz.platform.domain.model.SiteSummary
import com.quartz.platform.domain.repository.PerformanceSessionRepository
import com.quartz.platform.domain.repository.ReportDraftRepository
import com.quartz.platform.domain.repository.RetGuidedSessionRepository
import com.quartz.platform.domain.repository.SiteRepository
import com.quartz.platform.domain.repository.SyncRepository
import com.quartz.platform.domain.repository.XfeederGuidedSessionRepository
import com.quartz.platform.domain.usecase.ObserveReviewerControlTowerUseCase
import com.quartz.platform.domain.usecase.ObserveSiteListUseCase
import com.quartz.platform.domain.usecase.ObserveSiteReportClosureProjectionsUseCase
import com.quartz.platform.domain.usecase.ObserveSiteReportListUseCase
import com.quartz.platform.domain.usecase.RetryControlTowerFailedSyncUseCase
import com.quartz.platform.domain.usecase.RetryFailedReportDraftSyncUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewerControlTowerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun restores_filter_from_saved_state_handle() = runTest {
        val viewModel = buildViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    STATE_CONTROL_TOWER_SELECTED_FILTER to ReviewerControlTowerFilter.SYNC_FAILED.name
                )
            )
        )

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedFilter).isEqualTo(ReviewerControlTowerFilter.SYNC_FAILED)
    }

    @Test
    fun restores_grouping_from_saved_state_handle() = runTest {
        val viewModel = buildViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    STATE_CONTROL_TOWER_SELECTED_GROUPING to ReviewerControlTowerGrouping.WORKFLOW.name
                )
            )
        )

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedGrouping).isEqualTo(ReviewerControlTowerGrouping.WORKFLOW)
    }

    @Test
    fun restores_unknown_grouping_to_default_attention() = runTest {
        val viewModel = buildViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    STATE_CONTROL_TOWER_SELECTED_GROUPING to "UNKNOWN_GROUPING"
                )
            )
        )

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedGrouping).isEqualTo(ReviewerControlTowerGrouping.ATTENTION)
    }

    @Test
    fun restores_preset_and_progressed_queue_from_saved_state_handle() = runTest {
        val viewModel = buildViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    STATE_CONTROL_TOWER_SELECTED_PRESET to ReviewerQueuePreset.SYNC_FAILURES_FIRST.name,
                    STATE_CONTROL_TOWER_PROGRESS_DRAFT_IDS to listOf("draft-1")
                )
            )
        )

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedPreset).isEqualTo(ReviewerQueuePreset.SYNC_FAILURES_FIRST)
        assertThat(viewModel.uiState.value.progressedDraftIds).containsExactly("draft-1")
    }

    @Test
    fun selecting_preset_persists_and_unknown_falls_back_to_attention_now() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(STATE_CONTROL_TOWER_SELECTED_PRESET to "INVALID_PRESET")
        )
        val viewModel = buildViewModel(savedStateHandle = savedStateHandle)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedPreset).isEqualTo(ReviewerQueuePreset.NEEDS_ATTENTION_NOW)

        viewModel.onPresetSelected(ReviewerQueuePreset.QOS_RISK_FIRST)
        advanceUntilIdle()
        assertThat(savedStateHandle.get<String>(STATE_CONTROL_TOWER_SELECTED_PRESET))
            .isEqualTo(ReviewerQueuePreset.QOS_RISK_FIRST.name)
    }

    @Test
    fun selecting_filter_persists_and_open_draft_emits_event() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = buildViewModel(savedStateHandle = savedStateHandle)
        val events = mutableListOf<ReviewerControlTowerEvent>()
        val job = launch {
            viewModel.events.take(1).toList(events)
        }

        advanceUntilIdle()
        viewModel.onFilterSelected(ReviewerControlTowerFilter.QOS_RISK)
        viewModel.onOpenDraftClicked("draft-1")
        advanceUntilIdle()
        job.join()

        assertThat(savedStateHandle.get<String>(STATE_CONTROL_TOWER_SELECTED_FILTER))
            .isEqualTo(ReviewerControlTowerFilter.QOS_RISK.name)
        assertThat(events).containsExactly(ReviewerControlTowerEvent.OpenDraft("draft-1"))
    }

    @Test
    fun selecting_grouping_persists_and_open_top_priority_emits_event() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = buildViewModel(savedStateHandle = savedStateHandle)
        val events = mutableListOf<ReviewerControlTowerEvent>()
        val job = launch {
            viewModel.events.take(1).toList(events)
        }

        advanceUntilIdle()
        viewModel.onGroupingSelected(ReviewerControlTowerGrouping.WORKFLOW)
        viewModel.onOpenTopPriorityClicked()
        advanceUntilIdle()
        job.join()

        assertThat(savedStateHandle.get<String>(STATE_CONTROL_TOWER_SELECTED_GROUPING))
            .isEqualTo(ReviewerControlTowerGrouping.WORKFLOW.name)
        assertThat(events).containsExactly(ReviewerControlTowerEvent.OpenDraft("draft-1"))
        assertThat(savedStateHandle.get<List<String>>(STATE_CONTROL_TOWER_PROGRESS_DRAFT_IDS))
            .containsExactly("draft-1")
    }

    @Test
    fun reset_queue_progress_clears_progressed_ids() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = buildViewModel(savedStateHandle = savedStateHandle)
        advanceUntilIdle()

        viewModel.onOpenTopPriorityClicked()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.progressedDraftIds).containsExactly("draft-1")

        viewModel.onResetQueueProgressClicked()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.progressedDraftIds).isEmpty()
        assertThat(savedStateHandle.get<List<String>>(STATE_CONTROL_TOWER_PROGRESS_DRAFT_IDS)).isEmpty()
    }

    @Test
    fun selecting_preset_clears_existing_queue_progress() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = buildViewModel(savedStateHandle = savedStateHandle)
        advanceUntilIdle()

        viewModel.onOpenTopPriorityClicked()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.progressedDraftIds).containsExactly("draft-1")

        viewModel.onPresetSelected(ReviewerQueuePreset.QOS_RISK_FIRST)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.progressedDraftIds).isEmpty()
    }

    private fun buildViewModel(
        savedStateHandle: SavedStateHandle
    ): ReviewerControlTowerViewModel {
        val reportRepository = FakeReportDraftRepository(
            initial = listOf(
                ReportDraft(
                    id = "draft-1",
                    siteId = "site-1",
                    originWorkflowType = ReportDraftOriginWorkflowType.XFEEDER,
                    title = "Draft 1",
                    observation = "",
                    revision = 1,
                    createdAtEpochMillis = 1L,
                    updatedAtEpochMillis = 2L
                )
            )
        )
        val syncRepository = FakeSyncRepository(
            mapOf("draft-1" to ReportSyncTrace.localOnly())
        )
        val observeSiteReportListUseCase = ObserveSiteReportListUseCase(
            reportDraftRepository = reportRepository,
            syncRepository = syncRepository,
            observeSiteReportClosureProjectionsUseCase = ObserveSiteReportClosureProjectionsUseCase(
                xfeederRepository = EmptyXfeederRepository,
                retRepository = EmptyRetRepository,
                performanceSessionRepository = EmptyPerformanceRepository
            )
        )
        val observeReviewerControlTowerUseCase = ObserveReviewerControlTowerUseCase(
            reportDraftRepository = reportRepository,
            observeSiteReportListUseCase = observeSiteReportListUseCase,
            observeSiteListUseCase = ObserveSiteListUseCase(
                FakeSiteRepository(
                    listOf(
                        SiteSummary(
                            id = "site-1",
                            externalCode = "QRTZ-001",
                            name = "Rabat",
                            latitude = 34.0,
                            longitude = -6.8,
                            status = "IN_SERVICE",
                            sectorsInService = 3,
                            sectorsForecast = 0,
                            indoorOnly = false,
                            updatedAtEpochMillis = 1L
                        )
                    )
                )
            )
        )
        return ReviewerControlTowerViewModel(
            savedStateHandle = savedStateHandle,
            observeReviewerControlTowerUseCase = observeReviewerControlTowerUseCase,
            retryControlTowerFailedSyncUseCase = RetryControlTowerFailedSyncUseCase(
                RetryFailedReportDraftSyncUseCase(syncRepository)
            ),
            uiStrings = TestUiStrings()
        )
    }

    private class FakeReportDraftRepository(
        initial: List<ReportDraft>
    ) : ReportDraftRepository {
        private val draftsFlow = MutableStateFlow(initial)

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
        ): ReportDraft? = null

        override fun observeDraft(draftId: String): Flow<ReportDraft?> {
            return draftsFlow.map { drafts -> drafts.firstOrNull { it.id == draftId } }
        }

        override fun listAllDrafts(): Flow<List<ReportDraft>> = draftsFlow

        override fun listDraftsBySite(siteId: String): Flow<List<ReportDraft>> {
            return draftsFlow.map { drafts -> drafts.filter { it.siteId == siteId } }
        }
    }

    private class FakeSyncRepository(
        initial: Map<String, ReportSyncTrace>
    ) : SyncRepository {
        private val states = initial.mapValues { MutableStateFlow(it.value) }.toMutableMap()
        override suspend fun enqueueReportUpload(reportDraftId: String) = Unit

        override fun observeSyncTrace(reportDraftId: String): Flow<ReportSyncTrace> {
            return states.getOrPut(reportDraftId) { MutableStateFlow(ReportSyncTrace.localOnly()) }
        }

        override fun observeSyncState(reportDraftId: String): Flow<ReportSyncState> {
            return observeSyncTrace(reportDraftId).map { it.state }
        }

        override fun observePendingJobCount(): Flow<Int> = flowOf(0)

        override suspend fun processPendingJobs(limit: Int): Int = 0
    }

    private class FakeSiteRepository(
        sites: List<SiteSummary>
    ) : SiteRepository {
        private val sitesFlow = MutableStateFlow(sites)
        override fun observeSiteList(): Flow<List<SiteSummary>> = sitesFlow
        override fun observeSiteDetail(siteId: String) = flowOf(null)
        override suspend fun replaceSitesSnapshot(sites: List<com.quartz.platform.domain.model.SiteDetail>) = Unit
    }

    private object EmptyPerformanceRepository : PerformanceSessionRepository {
        override fun observeSiteSessionHistory(siteId: String) = flowOf(emptyList<com.quartz.platform.domain.model.PerformanceSession>())
        override fun observeLatestSiteSession(
            siteId: String,
            workflowType: com.quartz.platform.domain.model.PerformanceWorkflowType
        ) = flowOf<com.quartz.platform.domain.model.PerformanceSession?>(null)

        override suspend fun createSession(
            siteId: String,
            siteCode: String,
            workflowType: com.quartz.platform.domain.model.PerformanceWorkflowType,
            operatorName: String?,
            technology: String?
        ) = throw UnsupportedOperationException()

        override suspend fun updateStepStatus(
            sessionId: String,
            stepCode: com.quartz.platform.domain.model.PerformanceStepCode,
            status: com.quartz.platform.domain.model.PerformanceStepStatus
        ) = Unit

        override suspend fun updateSessionExecution(
            sessionId: String,
            status: com.quartz.platform.domain.model.PerformanceSessionStatus,
            prerequisiteNetworkReady: Boolean,
            prerequisiteBatterySufficient: Boolean,
            prerequisiteLocationReady: Boolean,
            observedNetworkStatus: com.quartz.platform.domain.model.NetworkStatus?,
            observedBatteryLevelPercent: Int?,
            observedLocationAvailable: Boolean?,
            observedSignalsCapturedAtEpochMillis: Long?,
            throughputMetrics: com.quartz.platform.domain.model.ThroughputMetrics,
            qosRunSummary: com.quartz.platform.domain.model.QosRunSummary,
            notes: String,
            resultSummary: String
        ) = Unit
    }

    private object EmptyXfeederRepository : XfeederGuidedSessionRepository {
        override fun observeSectorSessionHistory(siteId: String, sectorId: String) = flowOf(emptyList<com.quartz.platform.domain.model.XfeederGuidedSession>())
        override fun observeLatestSectorSession(siteId: String, sectorId: String) = flowOf<com.quartz.platform.domain.model.XfeederGuidedSession?>(null)
        override fun observeSiteClosureProjections(siteId: String) = flowOf(emptyList<com.quartz.platform.domain.model.GuidedSessionClosureProjection>())
        override suspend fun createSession(siteId: String, sectorId: String, sectorCode: String) = throw UnsupportedOperationException()
        override suspend fun updateStepStatus(sessionId: String, stepCode: com.quartz.platform.domain.model.XfeederStepCode, status: com.quartz.platform.domain.model.XfeederStepStatus) = Unit
        override suspend fun updateSessionSummary(
            sessionId: String,
            status: com.quartz.platform.domain.model.XfeederSessionStatus,
            sectorOutcome: com.quartz.platform.domain.model.XfeederSectorOutcome,
            closureEvidence: com.quartz.platform.domain.model.XfeederClosureEvidence,
            notes: String,
            resultSummary: String
        ) = Unit

        override suspend fun updateSessionGeospatialContext(
            sessionId: String,
            measurementZoneRadiusMeters: Int,
            measurementZoneExtensionReason: String,
            proximityModeEnabled: Boolean,
            proximityReferenceAltitudeMeters: Double?,
            proximityReferenceAltitudeSource: com.quartz.platform.domain.model.XfeederReferenceAltitudeSourceState
        ) = Unit
    }

    private object EmptyRetRepository : RetGuidedSessionRepository {
        override fun observeSectorSessionHistory(siteId: String, sectorId: String) = flowOf(emptyList<com.quartz.platform.domain.model.RetGuidedSession>())
        override fun observeLatestSectorSession(siteId: String, sectorId: String) = flowOf<com.quartz.platform.domain.model.RetGuidedSession?>(null)
        override fun observeSiteClosureProjections(siteId: String) = flowOf(emptyList<com.quartz.platform.domain.model.RetClosureProjection>())
        override suspend fun createSession(siteId: String, sectorId: String, sectorCode: String) = throw UnsupportedOperationException()
        override suspend fun updateStepStatus(sessionId: String, stepCode: com.quartz.platform.domain.model.RetStepCode, status: com.quartz.platform.domain.model.RetStepStatus) = Unit
        override suspend fun updateSessionSummary(
            sessionId: String,
            status: com.quartz.platform.domain.model.RetSessionStatus,
            resultOutcome: com.quartz.platform.domain.model.RetResultOutcome,
            notes: String,
            resultSummary: String
        ) = Unit

        override suspend fun updateSessionGeospatialContext(
            sessionId: String,
            measurementZoneRadiusMeters: Int,
            measurementZoneExtensionReason: String,
            proximityModeEnabled: Boolean,
            proximityReferenceAltitudeMeters: Double?,
            proximityReferenceAltitudeSource: com.quartz.platform.domain.model.RetReferenceAltitudeSourceState
        ) = Unit
    }
}
