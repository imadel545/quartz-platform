package com.quartz.platform.presentation.report.list

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.quartz.platform.MainDispatcherRule
import com.quartz.platform.TestUiStrings
import com.quartz.platform.domain.model.GuidedSessionClosureProjection
import com.quartz.platform.domain.model.PerformanceSession
import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.PerformanceStepCode
import com.quartz.platform.domain.model.PerformanceStepStatus
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.model.QosRunSummary
import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import com.quartz.platform.domain.model.RetClosureProjection
import com.quartz.platform.domain.model.RetReferenceAltitudeSourceState
import com.quartz.platform.domain.model.RetResultOutcome
import com.quartz.platform.domain.model.RetSessionStatus
import com.quartz.platform.domain.model.RetStepCode
import com.quartz.platform.domain.model.RetStepStatus
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.ReportSyncTrace
import com.quartz.platform.domain.model.RetGuidedSession
import com.quartz.platform.domain.model.RetGuidedStep
import com.quartz.platform.domain.model.XfeederClosureEvidence
import com.quartz.platform.domain.model.XfeederGeospatialPolicy
import com.quartz.platform.domain.model.XfeederGuidedSession
import com.quartz.platform.domain.model.XfeederGuidedStep
import com.quartz.platform.domain.model.XfeederReferenceAltitudeSourceState
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederSessionStatus
import com.quartz.platform.domain.model.XfeederStepCode
import com.quartz.platform.domain.model.XfeederStepStatus
import com.quartz.platform.domain.repository.ReportDraftRepository
import com.quartz.platform.domain.repository.PerformanceSessionRepository
import com.quartz.platform.domain.repository.RetGuidedSessionRepository
import com.quartz.platform.domain.repository.SyncRepository
import com.quartz.platform.domain.repository.XfeederGuidedSessionRepository
import com.quartz.platform.domain.usecase.ObserveSiteReportClosureProjectionsUseCase
import com.quartz.platform.domain.usecase.ObserveSiteReportListUseCase
import com.quartz.platform.domain.usecase.RetryFailedReportDraftSyncUseCase
import com.quartz.platform.presentation.navigation.QuartzDestination
import com.quartz.platform.domain.model.ThroughputMetrics
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
            observeSiteReportListUseCase = observeSiteReportListUseCase(
                reportDraftRepository = reportDraftRepository,
                syncRepository = syncRepository
            ),
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
            observeSiteReportListUseCase = observeSiteReportListUseCase(
                reportDraftRepository = reportDraftRepository,
                syncRepository = syncRepository
            ),
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

    @Test
    fun selecting_filter_keeps_only_matching_workflow_rows() = runTest {
        val reportDraftRepository = FakeReportDraftRepository(
            initial = listOf(
                ReportDraft(
                    id = "draft-xf",
                    siteId = "site-1",
                    originWorkflowType = ReportDraftOriginWorkflowType.XFEEDER,
                    title = "XFeeder",
                    observation = "",
                    revision = 1,
                    createdAtEpochMillis = 1L,
                    updatedAtEpochMillis = 2L
                ),
                ReportDraft(
                    id = "draft-ret",
                    siteId = "site-1",
                    originWorkflowType = ReportDraftOriginWorkflowType.RET,
                    title = "RET",
                    observation = "",
                    revision = 1,
                    createdAtEpochMillis = 1L,
                    updatedAtEpochMillis = 3L
                ),
                ReportDraft(
                    id = "draft-local",
                    siteId = "site-1",
                    title = "Non guidé",
                    observation = "",
                    revision = 1,
                    createdAtEpochMillis = 1L,
                    updatedAtEpochMillis = 4L
                )
            )
        )
        val syncRepository = FakeSyncRepository(
            initial = mapOf(
                "draft-xf" to ReportSyncTrace.localOnly(),
                "draft-ret" to ReportSyncTrace.localOnly(),
                "draft-local" to ReportSyncTrace.localOnly()
            )
        )
        val viewModel = ReportListViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(QuartzDestination.ReportList.ARG_SITE_ID to "site-1")
            ),
            observeSiteReportListUseCase = observeSiteReportListUseCase(
                reportDraftRepository = reportDraftRepository,
                syncRepository = syncRepository
            ),
            retryFailedReportDraftSyncUseCase = RetryFailedReportDraftSyncUseCase(syncRepository),
            uiStrings = TestUiStrings()
        )

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.filteredReports.map { it.draftId })
            .containsExactly("draft-xf", "draft-ret", "draft-local")
            .inOrder()

        viewModel.onFilterSelected(ReportListFilter.RET)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.filteredReports.map { it.draftId })
            .containsExactly("draft-ret")

        viewModel.onFilterSelected(ReportListFilter.NON_GUIDED)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.filteredReports.map { it.draftId })
            .containsExactly("draft-local")
    }

    @Test
    fun restores_selected_filter_from_saved_state_handle() = runTest {
        val reportDraftRepository = FakeReportDraftRepository(initial = emptyList())
        val syncRepository = FakeSyncRepository(initial = emptyMap())

        val viewModel = ReportListViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    QuartzDestination.ReportList.ARG_SITE_ID to "site-1",
                    STATE_SELECTED_FILTER to ReportListFilter.RET.name
                )
            ),
            observeSiteReportListUseCase = observeSiteReportListUseCase(
                reportDraftRepository = reportDraftRepository,
                syncRepository = syncRepository
            ),
            retryFailedReportDraftSyncUseCase = RetryFailedReportDraftSyncUseCase(syncRepository),
            uiStrings = TestUiStrings()
        )

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedFilter).isEqualTo(ReportListFilter.RET)
    }

    @Test
    fun unknown_saved_filter_falls_back_to_all() = runTest {
        val reportDraftRepository = FakeReportDraftRepository(initial = emptyList())
        val syncRepository = FakeSyncRepository(initial = emptyMap())

        val viewModel = ReportListViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    QuartzDestination.ReportList.ARG_SITE_ID to "site-1",
                    STATE_SELECTED_FILTER to "NOT_A_REAL_FILTER"
                )
            ),
            observeSiteReportListUseCase = observeSiteReportListUseCase(
                reportDraftRepository = reportDraftRepository,
                syncRepository = syncRepository
            ),
            retryFailedReportDraftSyncUseCase = RetryFailedReportDraftSyncUseCase(syncRepository),
            uiStrings = TestUiStrings()
        )

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedFilter).isEqualTo(ReportListFilter.ALL)
    }

    @Test
    fun selecting_filter_persists_value_in_saved_state_handle() = runTest {
        val reportDraftRepository = FakeReportDraftRepository(initial = emptyList())
        val syncRepository = FakeSyncRepository(initial = emptyMap())
        val savedStateHandle = SavedStateHandle(
            mapOf(QuartzDestination.ReportList.ARG_SITE_ID to "site-1")
        )

        val viewModel = ReportListViewModel(
            savedStateHandle = savedStateHandle,
            observeSiteReportListUseCase = observeSiteReportListUseCase(
                reportDraftRepository = reportDraftRepository,
                syncRepository = syncRepository
            ),
            retryFailedReportDraftSyncUseCase = RetryFailedReportDraftSyncUseCase(syncRepository),
            uiStrings = TestUiStrings()
        )

        advanceUntilIdle()
        viewModel.onFilterSelected(ReportListFilter.XFEEDER)
        advanceUntilIdle()

        assertThat(savedStateHandle.get<String>(STATE_SELECTED_FILTER))
            .isEqualTo(ReportListFilter.XFEEDER.name)
    }

    @Test
    fun workflow_type_to_filter_mapping_stays_bounded() {
        assertThat(ReportListFilter.forWorkflowTypeOrDefault(ReportDraftOriginWorkflowType.XFEEDER))
            .isEqualTo(ReportListFilter.XFEEDER)
        assertThat(ReportListFilter.forWorkflowTypeOrDefault(ReportDraftOriginWorkflowType.RET))
            .isEqualTo(ReportListFilter.RET)
        assertThat(ReportListFilter.forWorkflowTypeOrDefault(ReportDraftOriginWorkflowType.PERFORMANCE))
            .isEqualTo(ReportListFilter.PERFORMANCE)
        assertThat(ReportListFilter.forWorkflowTypeOrDefault(null))
            .isEqualTo(ReportListFilter.NON_GUIDED)
    }

    private fun observeSiteReportListUseCase(
        reportDraftRepository: ReportDraftRepository,
        syncRepository: SyncRepository
    ): ObserveSiteReportListUseCase {
        return ObserveSiteReportListUseCase(
            reportDraftRepository = reportDraftRepository,
            syncRepository = syncRepository,
            observeSiteReportClosureProjectionsUseCase = ObserveSiteReportClosureProjectionsUseCase(
                xfeederRepository = FakeXfeederGuidedSessionRepository(),
                retRepository = FakeRetGuidedSessionRepository(),
                performanceSessionRepository = FakePerformanceSessionRepository()
            )
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

    private class FakeXfeederGuidedSessionRepository : XfeederGuidedSessionRepository {
        override fun observeSectorSessionHistory(
            siteId: String,
            sectorId: String
        ): Flow<List<XfeederGuidedSession>> = flowOf(emptyList())

        override fun observeLatestSectorSession(siteId: String, sectorId: String): Flow<XfeederGuidedSession?> {
            return flowOf(null)
        }

        override fun observeSiteClosureProjections(siteId: String): Flow<List<GuidedSessionClosureProjection>> {
            return flowOf(emptyList())
        }

        override suspend fun createSession(
            siteId: String,
            sectorId: String,
            sectorCode: String
        ): XfeederGuidedSession {
            return XfeederGuidedSession(
                id = "xf",
                siteId = siteId,
                sectorId = sectorId,
                sectorCode = sectorCode,
                measurementZoneRadiusMeters = XfeederGeospatialPolicy.DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS,
                measurementZoneExtensionReason = "",
                proximityModeEnabled = false,
                proximityReferenceAltitudeMeters = null,
                proximityReferenceAltitudeSource = XfeederReferenceAltitudeSourceState.UNAVAILABLE,
                status = XfeederSessionStatus.CREATED,
                sectorOutcome = XfeederSectorOutcome.NOT_TESTED,
                closureEvidence = XfeederClosureEvidence("", null, null),
                notes = "",
                resultSummary = "",
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L,
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

        override suspend fun updateSessionGeospatialContext(
            sessionId: String,
            measurementZoneRadiusMeters: Int,
            measurementZoneExtensionReason: String,
            proximityModeEnabled: Boolean,
            proximityReferenceAltitudeMeters: Double?,
            proximityReferenceAltitudeSource: XfeederReferenceAltitudeSourceState
        ) = Unit
    }

    private class FakeRetGuidedSessionRepository : RetGuidedSessionRepository {
        override fun observeSectorSessionHistory(
            siteId: String,
            sectorId: String
        ): Flow<List<RetGuidedSession>> = flowOf(emptyList())

        override fun observeLatestSectorSession(siteId: String, sectorId: String): Flow<RetGuidedSession?> {
            return flowOf(null)
        }

        override fun observeSiteClosureProjections(siteId: String): Flow<List<RetClosureProjection>> {
            return flowOf(emptyList())
        }

        override suspend fun createSession(
            siteId: String,
            sectorId: String,
            sectorCode: String
        ): RetGuidedSession {
            return RetGuidedSession(
                id = "ret",
                siteId = siteId,
                sectorId = sectorId,
                sectorCode = sectorCode,
                measurementZoneRadiusMeters = 70,
                measurementZoneExtensionReason = "",
                proximityModeEnabled = false,
                proximityReferenceAltitudeMeters = null,
                proximityReferenceAltitudeSource = RetReferenceAltitudeSourceState.UNAVAILABLE,
                status = RetSessionStatus.CREATED,
                resultOutcome = RetResultOutcome.NOT_RUN,
                notes = "",
                resultSummary = "",
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L,
                completedAtEpochMillis = null,
                steps = listOf(
                    RetGuidedStep(
                        code = RetStepCode.CALIBRATION_PRECHECK,
                        required = true,
                        status = RetStepStatus.TODO
                    )
                )
            )
        }

        override suspend fun updateStepStatus(
            sessionId: String,
            stepCode: RetStepCode,
            status: RetStepStatus
        ) = Unit

        override suspend fun updateSessionSummary(
            sessionId: String,
            status: RetSessionStatus,
            resultOutcome: RetResultOutcome,
            notes: String,
            resultSummary: String
        ) = Unit

        override suspend fun updateSessionGeospatialContext(
            sessionId: String,
            measurementZoneRadiusMeters: Int,
            measurementZoneExtensionReason: String,
            proximityModeEnabled: Boolean,
            proximityReferenceAltitudeMeters: Double?,
            proximityReferenceAltitudeSource: RetReferenceAltitudeSourceState
        ) = Unit
    }

    private class FakePerformanceSessionRepository : PerformanceSessionRepository {
        override fun observeSiteSessionHistory(siteId: String): Flow<List<PerformanceSession>> = flowOf(emptyList())

        override fun observeLatestSiteSession(
            siteId: String,
            workflowType: PerformanceWorkflowType
        ): Flow<PerformanceSession?> = flowOf(null)

        override suspend fun createSession(
            siteId: String,
            siteCode: String,
            workflowType: PerformanceWorkflowType,
            operatorName: String?,
            technology: String?
        ): PerformanceSession {
            return PerformanceSession(
                id = "perf",
                siteId = siteId,
                siteCode = siteCode,
                workflowType = workflowType,
                operatorName = operatorName,
                technology = technology,
                status = PerformanceSessionStatus.CREATED,
                prerequisiteNetworkReady = false,
                prerequisiteBatterySufficient = false,
                prerequisiteLocationReady = false,
                throughputMetrics = ThroughputMetrics(),
                qosRunSummary = QosRunSummary(),
                notes = "",
                resultSummary = "",
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L,
                completedAtEpochMillis = null,
                steps = listOf(
                    com.quartz.platform.domain.model.PerformanceGuidedStep(
                        code = PerformanceStepCode.PRECONDITIONS_CHECK,
                        required = true,
                        status = PerformanceStepStatus.TODO
                    )
                )
            )
        }

        override suspend fun updateStepStatus(
            sessionId: String,
            stepCode: PerformanceStepCode,
            status: PerformanceStepStatus
        ) = Unit

        override suspend fun updateSessionExecution(
            sessionId: String,
            status: PerformanceSessionStatus,
            prerequisiteNetworkReady: Boolean,
            prerequisiteBatterySufficient: Boolean,
            prerequisiteLocationReady: Boolean,
            throughputMetrics: ThroughputMetrics,
            qosRunSummary: QosRunSummary,
            notes: String,
            resultSummary: String
        ) = Unit
    }
}
