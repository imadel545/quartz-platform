package com.quartz.platform.presentation.report.draft

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.quartz.platform.MainDispatcherRule
import com.quartz.platform.TestUiStrings
import com.quartz.platform.data.remote.simulation.SyncSimulationControl
import com.quartz.platform.data.remote.simulation.SyncSimulationMode
import com.quartz.platform.domain.model.GuidedSessionClosureProjection
import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.ReportSyncTrace
import com.quartz.platform.domain.model.XfeederClosureEvidence
import com.quartz.platform.domain.model.XfeederGuidedSession
import com.quartz.platform.domain.model.XfeederGuidedStep
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederSessionStatus
import com.quartz.platform.domain.model.XfeederStepCode
import com.quartz.platform.domain.model.XfeederStepStatus
import com.quartz.platform.domain.model.XfeederUnreliableReason
import com.quartz.platform.domain.repository.ReportDraftRepository
import com.quartz.platform.domain.repository.SyncRepository
import com.quartz.platform.domain.repository.XfeederGuidedSessionRepository
import com.quartz.platform.domain.usecase.EnqueueReportDraftSyncUseCase
import com.quartz.platform.domain.usecase.ObserveReportDraftSyncTraceUseCase
import com.quartz.platform.domain.usecase.ObserveReportDraftUseCase
import com.quartz.platform.domain.usecase.ObserveSiteClosureProjectionsUseCase
import com.quartz.platform.domain.usecase.UpdateReportDraftUseCase
import com.quartz.platform.presentation.navigation.QuartzDestination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportDraftViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun save_and_enqueue_sync_updates_state() = runTest {
        val draftRepository = FakeReportDraftRepository(
            ReportDraft(
                id = "draft-1",
                siteId = "site-1",
                title = "Initial",
                observation = "Initial obs",
                revision = 1,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L
            )
        )
        val syncRepository = FakeSyncRepository()
        val xfeederRepository = FakeXfeederGuidedSessionRepository()

        val viewModel = ReportDraftViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(QuartzDestination.ReportDraft.ARG_DRAFT_ID to "draft-1")
            ),
            observeReportDraftUseCase = ObserveReportDraftUseCase(draftRepository),
            updateReportDraftUseCase = UpdateReportDraftUseCase(draftRepository),
            enqueueReportDraftSyncUseCase = EnqueueReportDraftSyncUseCase(syncRepository),
            observeReportDraftSyncTraceUseCase = ObserveReportDraftSyncTraceUseCase(syncRepository),
            observeSiteClosureProjectionsUseCase = ObserveSiteClosureProjectionsUseCase(xfeederRepository),
            syncSimulationControls = emptySet(),
            uiStrings = TestUiStrings()
        )

        advanceUntilIdle()

        viewModel.onTitleChanged("Updated title")
        viewModel.onObservationChanged("Updated observation")
        viewModel.onSaveClicked()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.draft?.revision).isEqualTo(2)
        assertThat(viewModel.uiState.value.titleInput).isEqualTo("Updated title")
        assertThat(viewModel.uiState.value.hasUnsavedChanges).isFalse()

        viewModel.onQueueSyncClicked()
        advanceUntilIdle()

        assertThat(syncRepository.enqueuedDraftIds).containsExactly("draft-1")
        assertThat(viewModel.uiState.value.syncState).isEqualTo(ReportSyncState.PENDING)
        assertThat(viewModel.uiState.value.syncTrace.retryCount).isEqualTo(0)
    }

    @Test
    fun enqueue_requires_saved_changes_first() = runTest {
        val draftRepository = FakeReportDraftRepository(
            ReportDraft(
                id = "draft-2",
                siteId = "site-1",
                title = "Initial",
                observation = "Initial obs",
                revision = 1,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L
            )
        )
        val syncRepository = FakeSyncRepository()
        val xfeederRepository = FakeXfeederGuidedSessionRepository()

        val viewModel = ReportDraftViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(QuartzDestination.ReportDraft.ARG_DRAFT_ID to "draft-2")
            ),
            observeReportDraftUseCase = ObserveReportDraftUseCase(draftRepository),
            updateReportDraftUseCase = UpdateReportDraftUseCase(draftRepository),
            enqueueReportDraftSyncUseCase = EnqueueReportDraftSyncUseCase(syncRepository),
            observeReportDraftSyncTraceUseCase = ObserveReportDraftSyncTraceUseCase(syncRepository),
            observeSiteClosureProjectionsUseCase = ObserveSiteClosureProjectionsUseCase(xfeederRepository),
            syncSimulationControls = emptySet(),
            uiStrings = TestUiStrings()
        )

        advanceUntilIdle()
        viewModel.onObservationChanged("Unsaved local changes")
        viewModel.onQueueSyncClicked()
        advanceUntilIdle()

        assertThat(syncRepository.enqueuedDraftIds).isEmpty()
        assertThat(viewModel.uiState.value.errorMessage).contains("Enregistrez le brouillon")
    }

    @Test
    fun debug_sync_simulation_mode_is_visible_and_changeable_when_control_is_available() = runTest {
        val draftRepository = FakeReportDraftRepository(
            ReportDraft(
                id = "draft-3",
                siteId = "site-1",
                title = "Initial",
                observation = "Initial obs",
                revision = 1,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L
            )
        )
        val syncRepository = FakeSyncRepository()
        val simulationControl = FakeSyncSimulationControl()
        val xfeederRepository = FakeXfeederGuidedSessionRepository()

        val viewModel = ReportDraftViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(QuartzDestination.ReportDraft.ARG_DRAFT_ID to "draft-3")
            ),
            observeReportDraftUseCase = ObserveReportDraftUseCase(draftRepository),
            updateReportDraftUseCase = UpdateReportDraftUseCase(draftRepository),
            enqueueReportDraftSyncUseCase = EnqueueReportDraftSyncUseCase(syncRepository),
            observeReportDraftSyncTraceUseCase = ObserveReportDraftSyncTraceUseCase(syncRepository),
            observeSiteClosureProjectionsUseCase = ObserveSiteClosureProjectionsUseCase(xfeederRepository),
            syncSimulationControls = setOf(simulationControl),
            uiStrings = TestUiStrings()
        )

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.isSyncSimulationControlVisible).isTrue()
        assertThat(viewModel.uiState.value.syncSimulationMode).isEqualTo(SyncSimulationMode.NORMAL_SUCCESS)

        viewModel.onSyncSimulationModeSelected(SyncSimulationMode.FAIL_NEXT_TERMINAL)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.syncSimulationMode).isEqualTo(SyncSimulationMode.FAIL_NEXT_TERMINAL)
    }

    @Test
    fun draft_origin_session_prefers_matching_closure_projection() = runTest {
        val draftRepository = FakeReportDraftRepository(
            ReportDraft(
                id = "draft-4",
                siteId = "site-1",
                originSessionId = "session-origin",
                title = "Rapport",
                observation = "",
                revision = 1,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L
            )
        )
        val syncRepository = FakeSyncRepository()
        val xfeederRepository = FakeXfeederGuidedSessionRepository(
            projections = listOf(
                GuidedSessionClosureProjection(
                    sessionId = "session-other",
                    siteId = "site-1",
                    sectorId = "sector-s1",
                    sectorCode = "S1",
                    sectorOutcome = XfeederSectorOutcome.OK,
                    relatedSectorCode = null,
                    unreliableReason = null,
                    observedSectorCount = null,
                    updatedAtEpochMillis = 5L
                ),
                GuidedSessionClosureProjection(
                    sessionId = "session-1",
                    siteId = "site-1",
                    sectorId = "sector-s0",
                    sectorCode = "S0",
                    sectorOutcome = XfeederSectorOutcome.CROSSED,
                    relatedSectorCode = "S1",
                    unreliableReason = null,
                    observedSectorCount = null,
                    updatedAtEpochMillis = 10L
                ),
                GuidedSessionClosureProjection(
                    sessionId = "session-origin",
                    siteId = "site-1",
                    sectorId = "sector-s2",
                    sectorCode = "S2",
                    sectorOutcome = XfeederSectorOutcome.UNRELIABLE,
                    relatedSectorCode = null,
                    unreliableReason = XfeederUnreliableReason.NO_MAJORITY_SECTOR,
                    observedSectorCount = 3,
                    updatedAtEpochMillis = 15L
                )
            )
        )

        val viewModel = ReportDraftViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(QuartzDestination.ReportDraft.ARG_DRAFT_ID to "draft-4")
            ),
            observeReportDraftUseCase = ObserveReportDraftUseCase(draftRepository),
            updateReportDraftUseCase = UpdateReportDraftUseCase(draftRepository),
            enqueueReportDraftSyncUseCase = EnqueueReportDraftSyncUseCase(syncRepository),
            observeReportDraftSyncTraceUseCase = ObserveReportDraftSyncTraceUseCase(syncRepository),
            observeSiteClosureProjectionsUseCase = ObserveSiteClosureProjectionsUseCase(xfeederRepository),
            syncSimulationControls = emptySet(),
            uiStrings = TestUiStrings()
        )

        advanceUntilIdle()

        assertThat(viewModel.uiState.value.closureProjections).hasSize(1)
        assertThat(viewModel.uiState.value.closureProjections.first().sessionId)
            .isEqualTo("session-origin")
    }

    @Test
    fun draft_without_origin_keeps_site_level_closure_projection_fallback() = runTest {
        val draftRepository = FakeReportDraftRepository(
            ReportDraft(
                id = "draft-5",
                siteId = "site-1",
                title = "Rapport sans origine",
                observation = "",
                revision = 1,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L
            )
        )
        val syncRepository = FakeSyncRepository()
        val xfeederRepository = FakeXfeederGuidedSessionRepository(
            projections = listOf(
                GuidedSessionClosureProjection(
                    sessionId = "session-a",
                    siteId = "site-1",
                    sectorId = "sector-s0",
                    sectorCode = "S0",
                    sectorOutcome = XfeederSectorOutcome.OK,
                    relatedSectorCode = null,
                    unreliableReason = null,
                    observedSectorCount = null,
                    updatedAtEpochMillis = 10L
                ),
                GuidedSessionClosureProjection(
                    sessionId = "session-b",
                    siteId = "site-1",
                    sectorId = "sector-s1",
                    sectorCode = "S1",
                    sectorOutcome = XfeederSectorOutcome.CROSSED,
                    relatedSectorCode = "S0",
                    unreliableReason = null,
                    observedSectorCount = null,
                    updatedAtEpochMillis = 11L
                )
            )
        )

        val viewModel = ReportDraftViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(QuartzDestination.ReportDraft.ARG_DRAFT_ID to "draft-5")
            ),
            observeReportDraftUseCase = ObserveReportDraftUseCase(draftRepository),
            updateReportDraftUseCase = UpdateReportDraftUseCase(draftRepository),
            enqueueReportDraftSyncUseCase = EnqueueReportDraftSyncUseCase(syncRepository),
            observeReportDraftSyncTraceUseCase = ObserveReportDraftSyncTraceUseCase(syncRepository),
            observeSiteClosureProjectionsUseCase = ObserveSiteClosureProjectionsUseCase(xfeederRepository),
            syncSimulationControls = emptySet(),
            uiStrings = TestUiStrings()
        )

        advanceUntilIdle()

        assertThat(viewModel.uiState.value.closureProjections).hasSize(2)
    }

    private class FakeReportDraftRepository(
        initial: ReportDraft
    ) : ReportDraftRepository {
        private val state = MutableStateFlow(initial)

        override suspend fun createDraft(
            siteId: String,
            originSessionId: String?,
            originSectorId: String?
        ): ReportDraft = state.value

        override suspend fun updateDraft(draftId: String, title: String, observation: String): ReportDraft {
            val updated = state.value.copy(
                title = title,
                observation = observation,
                revision = state.value.revision + 1,
                updatedAtEpochMillis = state.value.updatedAtEpochMillis + 1
            )
            state.value = updated
            return updated
        }

        override suspend fun findLatestLinkedDraft(siteId: String, originSessionId: String): ReportDraft? {
            val draft = state.value
            return if (draft.siteId == siteId && draft.originSessionId == originSessionId) draft else null
        }

        override fun observeDraft(draftId: String): Flow<ReportDraft?> = state

        override fun listDraftsBySite(siteId: String): Flow<List<ReportDraft>> = flowOf(emptyList())
    }

    private class FakeSyncRepository : SyncRepository {
        private val syncFlow = MutableStateFlow(ReportSyncTrace.localOnly())
        val enqueuedDraftIds = mutableListOf<String>()

        override suspend fun enqueueReportUpload(reportDraftId: String) {
            enqueuedDraftIds += reportDraftId
            syncFlow.value = ReportSyncTrace(
                state = ReportSyncState.PENDING,
                lastAttemptAtEpochMillis = null,
                retryCount = 0,
                failureReason = null
            )
        }

        override fun observeSyncTrace(reportDraftId: String): Flow<ReportSyncTrace> = syncFlow

        override fun observeSyncState(reportDraftId: String): Flow<ReportSyncState> {
            return syncFlow.map { it.state }
        }

        override fun observePendingJobCount(): Flow<Int> = flowOf(0)

        override suspend fun processPendingJobs(limit: Int): Int = 0
    }

    private class FakeSyncSimulationControl : SyncSimulationControl {
        private val modeFlow = MutableStateFlow(SyncSimulationMode.NORMAL_SUCCESS)

        override fun observeMode(): Flow<SyncSimulationMode> = modeFlow

        override suspend fun setMode(mode: SyncSimulationMode) {
            modeFlow.value = mode
        }
    }

    private class FakeXfeederGuidedSessionRepository(
        private val projections: List<GuidedSessionClosureProjection> = emptyList()
    ) : XfeederGuidedSessionRepository {
        override fun observeSectorSessionHistory(
            siteId: String,
            sectorId: String
        ): Flow<List<XfeederGuidedSession>> = flowOf(emptyList())

        override fun observeLatestSectorSession(siteId: String, sectorId: String): Flow<XfeederGuidedSession?> {
            return flowOf(null)
        }

        override fun observeSiteClosureProjections(siteId: String): Flow<List<GuidedSessionClosureProjection>> {
            return flowOf(projections.filter { it.siteId == siteId })
        }

        override suspend fun createSession(
            siteId: String,
            sectorId: String,
            sectorCode: String
        ): XfeederGuidedSession {
            return XfeederGuidedSession(
                id = "unused",
                siteId = siteId,
                sectorId = sectorId,
                sectorCode = sectorCode,
                status = XfeederSessionStatus.CREATED,
                sectorOutcome = XfeederSectorOutcome.NOT_TESTED,
                closureEvidence = XfeederClosureEvidence(
                    relatedSectorCode = "",
                    unreliableReason = null,
                    observedSectorCount = null
                ),
                notes = "",
                resultSummary = "",
                createdAtEpochMillis = 0L,
                updatedAtEpochMillis = 0L,
                completedAtEpochMillis = null,
                steps = listOf(
                    XfeederGuidedStep(
                        code = XfeederStepCode.PRECONDITION_NETWORK_READY,
                        required = true,
                        status = XfeederStepStatus.TODO
                    )
                )
            )
        }

        override suspend fun updateStepStatus(
            sessionId: String,
            stepCode: XfeederStepCode,
            status: XfeederStepStatus
        ) = Unit

        override suspend fun updateSessionSummary(
            sessionId: String,
            status: XfeederSessionStatus,
            sectorOutcome: XfeederSectorOutcome,
            closureEvidence: XfeederClosureEvidence,
            notes: String,
            resultSummary: String
        ) = Unit
    }
}
