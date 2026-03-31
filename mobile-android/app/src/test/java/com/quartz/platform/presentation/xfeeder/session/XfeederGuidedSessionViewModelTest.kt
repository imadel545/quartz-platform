package com.quartz.platform.presentation.xfeeder.session

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.quartz.platform.MainDispatcherRule
import com.quartz.platform.TestUiStrings
import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import com.quartz.platform.domain.model.SiteAntenna
import com.quartz.platform.domain.model.SiteDetail
import com.quartz.platform.domain.model.SiteSector
import com.quartz.platform.domain.model.SiteSummary
import com.quartz.platform.domain.model.UserLocation
import com.quartz.platform.domain.model.XfeederGeospatialPolicy
import com.quartz.platform.domain.model.XfeederGuidedSession
import com.quartz.platform.domain.model.XfeederGuidedStep
import com.quartz.platform.domain.model.GuidedSessionClosureProjection
import com.quartz.platform.domain.model.XfeederProximityEligibilityState
import com.quartz.platform.domain.model.XfeederReferenceAltitudeSourceState
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederSessionStatus
import com.quartz.platform.domain.model.XfeederStepCode
import com.quartz.platform.domain.model.XfeederStepStatus
import com.quartz.platform.domain.model.XfeederClosureEvidence
import com.quartz.platform.domain.repository.ReportDraftRepository
import com.quartz.platform.domain.repository.LocationRepository
import com.quartz.platform.domain.repository.SiteRepository
import com.quartz.platform.domain.repository.XfeederGuidedSessionRepository
import com.quartz.platform.domain.usecase.CreateSectorXfeederSessionUseCase
import com.quartz.platform.domain.usecase.GetLastKnownUserLocationUseCase
import com.quartz.platform.domain.usecase.OpenOrCreateGuidedSessionReportDraftUseCase
import com.quartz.platform.domain.usecase.ObserveSectorXfeederSessionHistoryUseCase
import com.quartz.platform.domain.usecase.ObserveSiteDetailUseCase
import com.quartz.platform.domain.usecase.UpdateXfeederSessionSummaryUseCase
import com.quartz.platform.domain.usecase.UpdateXfeederStepStatusUseCase
import com.quartz.platform.domain.usecase.UpdateXfeederSessionGeospatialContextUseCase
import com.quartz.platform.presentation.navigation.QuartzDestination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class XfeederGuidedSessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `create session from selected sector exposes guided checklist`() = runTest {
        val siteRepository = FakeSiteRepository(site = sampleSiteDetail())
        val guidedRepository = FakeXfeederRepository()
        val viewModel = buildViewModel(siteRepository, guidedRepository)

        advanceUntilIdle()
        viewModel.onCreateSessionClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.session).isNotNull()
        assertThat(state.session!!.steps).isNotEmpty()
        assertThat(state.sessionHistory).hasSize(1)
        assertThat(state.session!!.status).isEqualTo(XfeederSessionStatus.CREATED)
    }

    @Test
    fun `save summary updates local sector outcome and status`() = runTest {
        val siteRepository = FakeSiteRepository(site = sampleSiteDetail())
        val guidedRepository = FakeXfeederRepository()
        val viewModel = buildViewModel(siteRepository, guidedRepository)

        advanceUntilIdle()
        viewModel.onCreateSessionClicked()
        advanceUntilIdle()

        viewModel.onSessionStatusSelected(XfeederSessionStatus.IN_PROGRESS)
        viewModel.onSectorOutcomeSelected(XfeederSectorOutcome.CROSSED)
        viewModel.onNotesChanged("Crossing observed on S0-S1")
        viewModel.onResultSummaryChanged("Manual guidance shell completed")
        viewModel.onSaveSummaryClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.session).isNotNull()
        assertThat(state.session!!.status).isEqualTo(XfeederSessionStatus.IN_PROGRESS)
        assertThat(state.session!!.sectorOutcome).isEqualTo(XfeederSectorOutcome.CROSSED)
        assertThat(state.session!!.notes).contains("Crossing observed")
    }

    @Test
    fun `completion guard blocks completed status when required steps are incomplete`() = runTest {
        val siteRepository = FakeSiteRepository(site = sampleSiteDetail())
        val guidedRepository = FakeXfeederRepository()
        val viewModel = buildViewModel(siteRepository, guidedRepository)

        advanceUntilIdle()
        viewModel.onCreateSessionClicked()
        advanceUntilIdle()

        viewModel.onSessionStatusSelected(XfeederSessionStatus.COMPLETED)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.selectedStatus).isEqualTo(XfeederSessionStatus.CREATED)
        assertThat(state.completionGuardMessage).contains("Impossible de clôturer")
    }

    @Test
    fun `zone extension requires explicit reason before persisting`() = runTest {
        val siteRepository = FakeSiteRepository(site = sampleSiteDetail())
        val guidedRepository = FakeXfeederRepository()
        val viewModel = buildViewModel(siteRepository, guidedRepository)

        advanceUntilIdle()
        viewModel.onCreateSessionClicked()
        advanceUntilIdle()

        viewModel.onExtendMeasurementZoneClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.errorMessage).isNotNull()
        assertThat(state.session?.measurementZoneRadiusMeters)
            .isEqualTo(XfeederGeospatialPolicy.DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS)
    }

    @Test
    fun `proximity stays supported until operator provides reference altitude`() = runTest {
        val siteRepository = FakeSiteRepository(site = sampleSiteDetail(withTechnicalReferenceAltitude = false))
        val guidedRepository = FakeXfeederRepository()
        val viewModel = buildViewModel(siteRepository, guidedRepository)

        advanceUntilIdle()
        viewModel.onCreateSessionClicked()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.proximityEligibilityState)
            .isEqualTo(XfeederProximityEligibilityState.SUPPORTED)
    }

    @Test
    fun `proximity mode requires reference altitude before enabling`() = runTest {
        val siteRepository = FakeSiteRepository(site = sampleSiteDetail(withTechnicalReferenceAltitude = false))
        val guidedRepository = FakeXfeederRepository()
        val viewModel = buildViewModel(siteRepository, guidedRepository)

        advanceUntilIdle()
        viewModel.onCreateSessionClicked()
        advanceUntilIdle()

        viewModel.onToggleProximityModeClicked(true)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.session?.proximityModeEnabled).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).contains("altitude antenne")
    }

    @Test
    fun `proximity mode can be enabled when distance and altitude are eligible`() = runTest {
        val siteRepository = FakeSiteRepository(site = sampleSiteDetail())
        val guidedRepository = FakeXfeederRepository()
        val viewModel = buildViewModel(siteRepository, guidedRepository)

        advanceUntilIdle()
        viewModel.onCreateSessionClicked()
        advanceUntilIdle()

        viewModel.onProximityReferenceAltitudeChanged("110")
        advanceUntilIdle()

        viewModel.onToggleProximityModeClicked(true)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.session?.proximityModeEnabled).isTrue()
        assertThat(viewModel.uiState.value.proximityEligibilityState)
            .isEqualTo(XfeederProximityEligibilityState.ELIGIBLE)
    }

    @Test
    fun `technical reference altitude is used by default when available`() = runTest {
        val siteRepository = FakeSiteRepository(site = sampleSiteDetail(withTechnicalReferenceAltitude = true))
        val guidedRepository = FakeXfeederRepository()
        val viewModel = buildViewModel(siteRepository, guidedRepository)

        advanceUntilIdle()
        viewModel.onCreateSessionClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.technicalReferenceAltitudeMeters).isEqualTo(118.0)
        assertThat(state.effectiveReferenceAltitudeMeters).isEqualTo(118.0)
        assertThat(state.proximityReferenceAltitudeSource)
            .isEqualTo(XfeederReferenceAltitudeSourceState.TECHNICAL_DEFAULT)
        assertThat(state.proximityReferenceAltitudeInput).isEmpty()
    }

    @Test
    fun `history keeps latest first and allows opening previous session`() = runTest {
        val siteRepository = FakeSiteRepository(site = sampleSiteDetail())
        val guidedRepository = FakeXfeederRepository()
        val viewModel = buildViewModel(siteRepository, guidedRepository)

        advanceUntilIdle()
        viewModel.onCreateSessionClicked()
        advanceUntilIdle()
        val latestId = viewModel.uiState.value.session!!.id

        viewModel.onCreateSessionClicked()
        advanceUntilIdle()
        val newerId = viewModel.uiState.value.session!!.id
        assertThat(viewModel.uiState.value.sessionHistory).hasSize(2)
        assertThat(viewModel.uiState.value.sessionHistory.first().id).isEqualTo(newerId)

        viewModel.onSelectHistorySessionClicked(latestId)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.session!!.id).isEqualTo(latestId)
    }

    @Test
    fun `completion guard blocks crossed outcome without related sector evidence`() = runTest {
        val siteRepository = FakeSiteRepository(site = sampleSiteDetail())
        val guidedRepository = FakeXfeederRepository()
        val viewModel = buildViewModel(siteRepository, guidedRepository)

        advanceUntilIdle()
        viewModel.onCreateSessionClicked()
        advanceUntilIdle()

        XfeederStepCode.entries
            .filterNot { it == XfeederStepCode.FINALIZE_SECTOR_SUMMARY }
            .forEach { code ->
                viewModel.onStepStatusSelected(code, XfeederStepStatus.DONE)
            }
        advanceUntilIdle()

        viewModel.onSectorOutcomeSelected(XfeederSectorOutcome.CROSSED)
        viewModel.onSessionStatusSelected(XfeederSessionStatus.COMPLETED)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.selectedStatus).isNotEqualTo(XfeederSessionStatus.COMPLETED)
        assertThat(state.completionGuardMessage).contains("secteur lié")
    }

    @Test
    fun `completed crossed session is saved when related sector evidence is provided`() = runTest {
        val siteRepository = FakeSiteRepository(site = sampleSiteDetail())
        val guidedRepository = FakeXfeederRepository()
        val viewModel = buildViewModel(siteRepository, guidedRepository)

        advanceUntilIdle()
        viewModel.onCreateSessionClicked()
        advanceUntilIdle()

        XfeederStepCode.entries
            .filterNot { it == XfeederStepCode.FINALIZE_SECTOR_SUMMARY }
            .forEach { code ->
                viewModel.onStepStatusSelected(code, XfeederStepStatus.DONE)
            }
        advanceUntilIdle()

        viewModel.onSectorOutcomeSelected(XfeederSectorOutcome.CROSSED)
        viewModel.onRelatedSectorCodeChanged("S1")
        viewModel.onSessionStatusSelected(XfeederSessionStatus.COMPLETED)
        viewModel.onSaveSummaryClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.session?.status).isEqualTo(XfeederSessionStatus.COMPLETED)
        assertThat(state.session?.closureEvidence?.relatedSectorCode).isEqualTo("S1")
    }

    @Test
    fun `create report draft from guided session wires origin session and sector`() = runTest {
        val siteRepository = FakeSiteRepository(site = sampleSiteDetail())
        val guidedRepository = FakeXfeederRepository()
        val reportDraftRepository = FakeReportDraftRepository()
        val viewModel = buildViewModel(
            siteRepository = siteRepository,
            guidedRepository = guidedRepository,
            reportDraftRepository = reportDraftRepository
        )

        advanceUntilIdle()
        viewModel.onCreateSessionClicked()
        advanceUntilIdle()

        var openedDraftId: String? = null
        val collectJob = launch {
            viewModel.events.collect { event ->
                if (event is XfeederGuidedSessionEvent.OpenDraft) {
                    openedDraftId = event.draftId
                    cancel()
                }
            }
        }
        viewModel.onCreateReportDraftClicked()
        advanceUntilIdle()
        if (collectJob.isActive) {
            collectJob.cancel()
        }

        val latestSession = viewModel.uiState.value.session
        assertThat(latestSession).isNotNull()
        assertThat(reportDraftRepository.lastCreateSiteId).isEqualTo("site-1")
        assertThat(reportDraftRepository.lastOriginSessionId).isEqualTo(latestSession!!.id)
        assertThat(reportDraftRepository.lastOriginSectorId).isEqualTo(latestSession.sectorId)
        assertThat(reportDraftRepository.lastOriginWorkflowType).isEqualTo(ReportDraftOriginWorkflowType.XFEEDER)
        assertThat(viewModel.uiState.value.isCreatingDraft).isFalse()
        assertThat(openedDraftId).isEqualTo("draft-1")
        assertThat(reportDraftRepository.createDraftCalls).isEqualTo(1)
    }

    @Test
    fun `open linked draft from guided session when already existing`() = runTest {
        val siteRepository = FakeSiteRepository(site = sampleSiteDetail())
        val guidedRepository = FakeXfeederRepository()
        val reportDraftRepository = FakeReportDraftRepository()
        val viewModel = buildViewModel(
            siteRepository = siteRepository,
            guidedRepository = guidedRepository,
            reportDraftRepository = reportDraftRepository
        )

        advanceUntilIdle()
        viewModel.onCreateSessionClicked()
        advanceUntilIdle()
        val session = requireNotNull(viewModel.uiState.value.session)
        reportDraftRepository.seedDraft(
            ReportDraft(
                id = "draft-existing",
                siteId = session.siteId,
                originSessionId = session.id,
                originSectorId = session.sectorId,
                originWorkflowType = ReportDraftOriginWorkflowType.XFEEDER,
                title = "Draft existant",
                observation = "Observation",
                revision = 2,
                createdAtEpochMillis = 5L,
                updatedAtEpochMillis = 20L
            )
        )

        var openedDraftId: String? = null
        val collectJob = launch {
            viewModel.events.collect { event ->
                if (event is XfeederGuidedSessionEvent.OpenDraft) {
                    openedDraftId = event.draftId
                    cancel()
                }
            }
        }
        viewModel.onCreateReportDraftClicked()
        advanceUntilIdle()
        if (collectJob.isActive) {
            collectJob.cancel()
        }

        assertThat(openedDraftId).isEqualTo("draft-existing")
        assertThat(reportDraftRepository.createDraftCalls).isEqualTo(0)
    }

    private fun buildViewModel(
        siteRepository: FakeSiteRepository,
        guidedRepository: FakeXfeederRepository,
        reportDraftRepository: FakeReportDraftRepository = FakeReportDraftRepository()
    ): XfeederGuidedSessionViewModel {
        return XfeederGuidedSessionViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    QuartzDestination.XfeederGuidedSession.ARG_SITE_ID to "site-1",
                    QuartzDestination.XfeederGuidedSession.ARG_SECTOR_ID to "site-1-sector-S0"
                )
            ),
            observeSiteDetailUseCase = ObserveSiteDetailUseCase(siteRepository),
            observeSectorXfeederSessionHistoryUseCase = ObserveSectorXfeederSessionHistoryUseCase(guidedRepository),
            createSectorXfeederSessionUseCase = CreateSectorXfeederSessionUseCase(guidedRepository),
            getLastKnownUserLocationUseCase = GetLastKnownUserLocationUseCase(
                locationRepository = object : LocationRepository {
                    override suspend fun getLastKnownLocation(): UserLocation? {
                        return UserLocation(
                            latitude = 34.0005,
                            longitude = -6.8002,
                            capturedAtEpochMillis = 10L,
                            altitudeMeters = 120.0f.toDouble(),
                            verticalAccuracyMeters = 4f
                        )
                    }
                }
            ),
            updateXfeederStepStatusUseCase = UpdateXfeederStepStatusUseCase(guidedRepository),
            updateXfeederSessionGeospatialContextUseCase = UpdateXfeederSessionGeospatialContextUseCase(
                guidedRepository
            ),
            updateXfeederSessionSummaryUseCase = UpdateXfeederSessionSummaryUseCase(guidedRepository),
            openOrCreateGuidedSessionReportDraftUseCase = OpenOrCreateGuidedSessionReportDraftUseCase(reportDraftRepository),
            uiStrings = TestUiStrings()
        )
    }

    private class FakeSiteRepository(
        site: SiteDetail
    ) : SiteRepository {
        private val siteFlow = MutableStateFlow(site)

        override fun observeSiteList(): Flow<List<SiteSummary>> = flowOf(emptyList())

        override fun observeSiteDetail(siteId: String): Flow<SiteDetail?> {
            return siteFlow.map { if (it.id == siteId) it else null }
        }

        override suspend fun replaceSitesSnapshot(sites: List<SiteDetail>) {
            siteFlow.value = sites.first()
        }
    }

    private class FakeXfeederRepository : XfeederGuidedSessionRepository {
        private val historyBySector = mutableMapOf<String, MutableStateFlow<List<XfeederGuidedSession>>>()
        private var sequence = 0L

        override fun observeSectorSessionHistory(siteId: String, sectorId: String): Flow<List<XfeederGuidedSession>> {
            return historyBySector.getOrPut(key(siteId, sectorId)) { MutableStateFlow(emptyList()) }
        }

        override fun observeLatestSectorSession(siteId: String, sectorId: String): Flow<XfeederGuidedSession?> {
            return observeSectorSessionHistory(siteId, sectorId).map { history -> history.firstOrNull() }
        }

        override fun observeSiteClosureProjections(siteId: String): Flow<List<GuidedSessionClosureProjection>> {
            return flowOf(emptyList())
        }

        override suspend fun createSession(
            siteId: String,
            sectorId: String,
            sectorCode: String
        ): XfeederGuidedSession {
            sequence += 1
            val now = sequence
            val created = XfeederGuidedSession(
                id = "session-$sequence",
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
                closureEvidence = XfeederClosureEvidence(
                    relatedSectorCode = "",
                    unreliableReason = null,
                    observedSectorCount = null
                ),
                notes = "",
                resultSummary = "",
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
                completedAtEpochMillis = null,
                steps = XfeederStepCode.entries.map { code ->
                    XfeederGuidedStep(
                        code = code,
                        required = code != XfeederStepCode.FINALIZE_SECTOR_SUMMARY,
                        status = XfeederStepStatus.TODO
                    )
                }
            )
            val flow = historyBySector.getOrPut(key(siteId, sectorId)) { MutableStateFlow(emptyList()) }
            flow.value = listOf(created) + flow.value
            return created
        }

        override suspend fun updateStepStatus(
            sessionId: String,
            stepCode: XfeederStepCode,
            status: XfeederStepStatus
        ) {
            historyBySector.values.forEach { flow ->
                flow.value = flow.value.map { session ->
                    if (session.id != sessionId) {
                        session
                    } else {
                        session.copy(
                            status = if (session.status == XfeederSessionStatus.CREATED && status != XfeederStepStatus.TODO) {
                                XfeederSessionStatus.IN_PROGRESS
                            } else {
                                session.status
                            },
                            updatedAtEpochMillis = session.updatedAtEpochMillis + 1,
                            steps = session.steps.map { step ->
                                if (step.code == stepCode) step.copy(status = status) else step
                            }
                        )
                    }
                }.sortedByDescending { it.createdAtEpochMillis }
            }
        }

        override suspend fun updateSessionSummary(
            sessionId: String,
            status: XfeederSessionStatus,
            sectorOutcome: XfeederSectorOutcome,
            closureEvidence: XfeederClosureEvidence,
            notes: String,
            resultSummary: String
        ) {
            historyBySector.values.forEach { flow ->
                flow.value = flow.value.map { session ->
                    if (session.id != sessionId) {
                        session
                    } else {
                        session.copy(
                            status = status,
                            sectorOutcome = sectorOutcome,
                            closureEvidence = closureEvidence,
                            notes = notes,
                            resultSummary = resultSummary,
                            updatedAtEpochMillis = session.updatedAtEpochMillis + 1,
                            completedAtEpochMillis = if (status == XfeederSessionStatus.COMPLETED) {
                                session.updatedAtEpochMillis + 1
                            } else {
                                null
                            }
                        )
                    }
                }.sortedByDescending { it.createdAtEpochMillis }
            }
        }

        override suspend fun updateSessionGeospatialContext(
            sessionId: String,
            measurementZoneRadiusMeters: Int,
            measurementZoneExtensionReason: String,
            proximityModeEnabled: Boolean,
            proximityReferenceAltitudeMeters: Double?,
            proximityReferenceAltitudeSource: XfeederReferenceAltitudeSourceState
        ) {
            historyBySector.values.forEach { flow ->
                flow.value = flow.value.map { session ->
                    if (session.id != sessionId) {
                        session
                    } else {
                        session.copy(
                            measurementZoneRadiusMeters = measurementZoneRadiusMeters,
                            measurementZoneExtensionReason = measurementZoneExtensionReason,
                            proximityModeEnabled = proximityModeEnabled,
                            proximityReferenceAltitudeMeters = proximityReferenceAltitudeMeters,
                            proximityReferenceAltitudeSource = proximityReferenceAltitudeSource,
                            updatedAtEpochMillis = session.updatedAtEpochMillis + 1
                        )
                    }
                }.sortedByDescending { it.createdAtEpochMillis }
            }
        }

        private fun key(siteId: String, sectorId: String): String = "$siteId::$sectorId"
    }

    private class FakeReportDraftRepository : ReportDraftRepository {
        private val drafts = MutableStateFlow<List<ReportDraft>>(emptyList())
        var createDraftCalls: Int = 0
        var lastCreateSiteId: String? = null
        var lastOriginSessionId: String? = null
        var lastOriginSectorId: String? = null
        var lastOriginWorkflowType: ReportDraftOriginWorkflowType? = null

        override suspend fun createDraft(
            siteId: String,
            originSessionId: String?,
            originSectorId: String?,
            originWorkflowType: ReportDraftOriginWorkflowType?
        ): ReportDraft {
            createDraftCalls += 1
            lastCreateSiteId = siteId
            lastOriginSessionId = originSessionId
            lastOriginSectorId = originSectorId
            lastOriginWorkflowType = originWorkflowType
            val created = ReportDraft(
                id = "draft-1",
                siteId = siteId,
                originSessionId = originSessionId,
                originSectorId = originSectorId,
                originWorkflowType = originWorkflowType,
                title = "Brouillon session",
                observation = "",
                revision = 1,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L
            )
            drafts.value = listOf(created) + drafts.value
            return created
        }

        override suspend fun updateDraft(draftId: String, title: String, observation: String): ReportDraft? = null

        override suspend fun findLatestLinkedDraft(
            siteId: String,
            originSessionId: String,
            originWorkflowType: ReportDraftOriginWorkflowType?
        ): ReportDraft? {
            return drafts.value
                .filter { draft ->
                    draft.siteId == siteId &&
                        draft.originSessionId == originSessionId &&
                        draft.originWorkflowType == originWorkflowType
                }
                .maxByOrNull { draft -> draft.updatedAtEpochMillis }
        }

        override fun observeDraft(draftId: String): Flow<ReportDraft?> = flowOf(null)

        override fun listAllDrafts(): Flow<List<ReportDraft>> = drafts

        override fun listDraftsBySite(siteId: String): Flow<List<ReportDraft>> =
            drafts.map { list -> list.filter { draft -> draft.siteId == siteId } }

        fun seedDraft(draft: ReportDraft) {
            drafts.value = listOf(draft) + drafts.value
        }
    }

    private fun sampleSiteDetail(withTechnicalReferenceAltitude: Boolean = true): SiteDetail {
        return SiteDetail(
            id = "site-1",
            externalCode = "QRTZ-001",
            name = "Rabat Centre",
            latitude = 34.0,
            longitude = -6.8,
            status = "IN_SERVICE",
            sectorsInService = 3,
            sectorsForecast = 0,
            indoorOnly = false,
            updatedAtEpochMillis = 1L,
            sectors = listOf(
                SiteSector(
                    id = "site-1-sector-S0",
                    siteId = "site-1",
                    code = "S0",
                    azimuthDegrees = 0,
                    status = "IN_SERVICE",
                    hasConnectedCell = true,
                    antennas = listOf(
                        SiteAntenna(
                            id = "site-1-antenna-S0-0",
                            sectorId = "site-1-sector-S0",
                            reference = "ANT-RB-S0",
                            referenceAltitudeMeters = if (withTechnicalReferenceAltitude) 118.0 else null,
                            installedState = "INSTALLED",
                            forecastState = null,
                            tiltConfiguredDegrees = 4.0,
                            tiltObservedDegrees = 3.5,
                            documentationRef = "DOC-ANT-RB-S0"
                        )
                    )
                )
            )
        )
    }
}
