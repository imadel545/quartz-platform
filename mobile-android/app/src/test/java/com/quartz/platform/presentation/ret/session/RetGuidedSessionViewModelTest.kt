package com.quartz.platform.presentation.ret.session

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.quartz.platform.MainDispatcherRule
import com.quartz.platform.TestUiStrings
import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import com.quartz.platform.domain.model.RetClosureProjection
import com.quartz.platform.domain.model.RetGeospatialPolicy
import com.quartz.platform.domain.model.RetGuidedSession
import com.quartz.platform.domain.model.RetGuidedStep
import com.quartz.platform.domain.model.RetProximityEligibilityState
import com.quartz.platform.domain.model.RetReferenceAltitudeSourceState
import com.quartz.platform.domain.model.RetResultOutcome
import com.quartz.platform.domain.model.RetSessionStatus
import com.quartz.platform.domain.model.RetStepCode
import com.quartz.platform.domain.model.RetStepStatus
import com.quartz.platform.domain.model.SiteAntenna
import com.quartz.platform.domain.model.SiteDetail
import com.quartz.platform.domain.model.SiteSector
import com.quartz.platform.domain.model.SiteSummary
import com.quartz.platform.domain.model.UserLocation
import com.quartz.platform.domain.repository.LocationRepository
import com.quartz.platform.domain.repository.ReportDraftRepository
import com.quartz.platform.domain.repository.RetGuidedSessionRepository
import com.quartz.platform.domain.repository.SiteRepository
import com.quartz.platform.domain.usecase.CreateSectorRetSessionUseCase
import com.quartz.platform.domain.usecase.GetLastKnownUserLocationUseCase
import com.quartz.platform.domain.usecase.OpenOrCreateGuidedSessionReportDraftUseCase
import com.quartz.platform.domain.usecase.ObserveSectorRetSessionHistoryUseCase
import com.quartz.platform.domain.usecase.ObserveSiteDetailUseCase
import com.quartz.platform.domain.usecase.UpdateRetSessionGeospatialContextUseCase
import com.quartz.platform.domain.usecase.UpdateRetSessionSummaryUseCase
import com.quartz.platform.domain.usecase.UpdateRetStepStatusUseCase
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
class RetGuidedSessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun createSession_exposesChecklistAndHistory() = runTest {
        val siteRepository = FakeSiteRepository(sampleSiteDetail())
        val retRepository = FakeRetRepository()
        val viewModel = buildViewModel(siteRepository, retRepository)

        advanceUntilIdle()
        viewModel.onCreateSessionClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.session).isNotNull()
        assertThat(state.session!!.steps).hasSize(3)
        assertThat(state.sessionHistory).hasSize(1)
        assertThat(state.session!!.status).isEqualTo(RetSessionStatus.CREATED)
    }

    @Test
    fun proximity_requires_reference_altitude_before_enabling() = runTest {
        val siteRepository = FakeSiteRepository(sampleSiteDetail(withTechnicalReferenceAltitude = false))
        val retRepository = FakeRetRepository()
        val viewModel = buildViewModel(siteRepository, retRepository)

        advanceUntilIdle()
        viewModel.onCreateSessionClicked()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.proximityEligibilityState)
            .isEqualTo(RetProximityEligibilityState.SUPPORTED)

        viewModel.onToggleProximityModeClicked(true)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.session?.proximityModeEnabled).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).contains("altitude antenne")
    }

    @Test
    fun technical_reference_altitude_is_applied_by_default_for_ret() = runTest {
        val siteRepository = FakeSiteRepository(sampleSiteDetail(withTechnicalReferenceAltitude = true))
        val retRepository = FakeRetRepository()
        val viewModel = buildViewModel(siteRepository, retRepository)

        advanceUntilIdle()
        viewModel.onCreateSessionClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.technicalReferenceAltitudeMeters).isEqualTo(118.0)
        assertThat(state.effectiveReferenceAltitudeMeters).isEqualTo(118.0)
        assertThat(state.proximityReferenceAltitudeSource)
            .isEqualTo(RetReferenceAltitudeSourceState.TECHNICAL_DEFAULT)
    }

    @Test
    fun selectingCompletedStatus_withoutRequiredSteps_setsCompletionGuardMessage() = runTest {
        val siteRepository = FakeSiteRepository(sampleSiteDetail())
        val retRepository = FakeRetRepository()
        val viewModel = buildViewModel(siteRepository, retRepository)

        advanceUntilIdle()
        viewModel.onCreateSessionClicked()
        advanceUntilIdle()

        viewModel.onSessionStatusSelected(RetSessionStatus.COMPLETED)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.selectedStatus).isEqualTo(RetSessionStatus.CREATED)
        assertThat(state.completionGuardMessage).isNotNull()
    }

    @Test
    fun createReportDraft_fromRetSession_wiresOriginAndEmitsOpenDraft() = runTest {
        val siteRepository = FakeSiteRepository(sampleSiteDetail())
        val retRepository = FakeRetRepository()
        val reportDraftRepository = FakeReportDraftRepository()
        val viewModel = buildViewModel(
            siteRepository = siteRepository,
            retRepository = retRepository,
            reportDraftRepository = reportDraftRepository
        )

        advanceUntilIdle()
        viewModel.onCreateSessionClicked()
        advanceUntilIdle()

        var openedDraftId: String? = null
        val collectJob = launch {
            viewModel.events.collect { event ->
                if (event is RetGuidedSessionEvent.OpenDraft) {
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
        assertThat(reportDraftRepository.lastOriginWorkflowType).isEqualTo(ReportDraftOriginWorkflowType.RET)
        assertThat(viewModel.uiState.value.isCreatingDraft).isFalse()
        assertThat(openedDraftId).isEqualTo("draft-1")
        assertThat(reportDraftRepository.createDraftCalls).isEqualTo(1)
    }

    @Test
    fun openLinkedDraft_fromRetSession_whenExisting_doesNotCreateDuplicate() = runTest {
        val siteRepository = FakeSiteRepository(sampleSiteDetail())
        val retRepository = FakeRetRepository()
        val reportDraftRepository = FakeReportDraftRepository()
        val viewModel = buildViewModel(
            siteRepository = siteRepository,
            retRepository = retRepository,
            reportDraftRepository = reportDraftRepository
        )

        advanceUntilIdle()
        viewModel.onCreateSessionClicked()
        advanceUntilIdle()
        val session = requireNotNull(viewModel.uiState.value.session)
        reportDraftRepository.seedDraft(
            ReportDraft(
                id = "draft-existing-ret",
                siteId = session.siteId,
                originSessionId = session.id,
                originSectorId = session.sectorId,
                originWorkflowType = ReportDraftOriginWorkflowType.RET,
                title = "RET existant",
                observation = "Obs",
                revision = 2,
                createdAtEpochMillis = 10L,
                updatedAtEpochMillis = 40L
            )
        )

        var openedDraftId: String? = null
        val collectJob = launch {
            viewModel.events.collect { event ->
                if (event is RetGuidedSessionEvent.OpenDraft) {
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

        assertThat(openedDraftId).isEqualTo("draft-existing-ret")
        assertThat(reportDraftRepository.createDraftCalls).isEqualTo(0)
    }

    private fun buildViewModel(
        siteRepository: FakeSiteRepository,
        retRepository: FakeRetRepository,
        reportDraftRepository: FakeReportDraftRepository = FakeReportDraftRepository(),
        locationRepository: FakeLocationRepository = FakeLocationRepository()
    ): RetGuidedSessionViewModel {
        return RetGuidedSessionViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    QuartzDestination.RetGuidedSession.ARG_SITE_ID to "site-1",
                    QuartzDestination.RetGuidedSession.ARG_SECTOR_ID to "site-1-sector-S0"
                )
            ),
            observeSiteDetailUseCase = ObserveSiteDetailUseCase(siteRepository),
            observeSectorRetSessionHistoryUseCase = ObserveSectorRetSessionHistoryUseCase(retRepository),
            createSectorRetSessionUseCase = CreateSectorRetSessionUseCase(retRepository),
            getLastKnownUserLocationUseCase = GetLastKnownUserLocationUseCase(locationRepository),
            updateRetStepStatusUseCase = UpdateRetStepStatusUseCase(retRepository),
            updateRetSessionGeospatialContextUseCase = UpdateRetSessionGeospatialContextUseCase(
                retRepository
            ),
            updateRetSessionSummaryUseCase = UpdateRetSessionSummaryUseCase(retRepository),
            openOrCreateGuidedSessionReportDraftUseCase = OpenOrCreateGuidedSessionReportDraftUseCase(
                reportDraftRepository
            ),
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

    private class FakeLocationRepository : LocationRepository {
        override suspend fun getLastKnownLocation(): UserLocation? {
            return UserLocation(
                latitude = 33.5901,
                longitude = -7.6038,
                capturedAtEpochMillis = 1L,
                altitudeMeters = 120.0,
                verticalAccuracyMeters = 4f
            )
        }
    }

    private class FakeRetRepository : RetGuidedSessionRepository {
        private val historyBySector = mutableMapOf<String, MutableStateFlow<List<RetGuidedSession>>>()
        private var sequence = 0L

        override fun observeSectorSessionHistory(siteId: String, sectorId: String): Flow<List<RetGuidedSession>> {
            return historyBySector.getOrPut(key(siteId, sectorId)) { MutableStateFlow(emptyList()) }
        }

        override fun observeLatestSectorSession(siteId: String, sectorId: String): Flow<RetGuidedSession?> {
            return observeSectorSessionHistory(siteId, sectorId).map { it.firstOrNull() }
        }

        override fun observeSiteClosureProjections(siteId: String): Flow<List<RetClosureProjection>> {
            return flowOf(emptyList())
        }

        override suspend fun createSession(siteId: String, sectorId: String, sectorCode: String): RetGuidedSession {
            sequence += 1
            val session = RetGuidedSession(
                id = "ret-$sequence",
                siteId = siteId,
                sectorId = sectorId,
                sectorCode = sectorCode,
                measurementZoneRadiusMeters = RetGeospatialPolicy.DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS,
                measurementZoneExtensionReason = "",
                proximityModeEnabled = false,
                proximityReferenceAltitudeMeters = null,
                proximityReferenceAltitudeSource = RetReferenceAltitudeSourceState.UNAVAILABLE,
                status = RetSessionStatus.CREATED,
                resultOutcome = RetResultOutcome.NOT_RUN,
                notes = "",
                resultSummary = "",
                createdAtEpochMillis = sequence,
                updatedAtEpochMillis = sequence,
                completedAtEpochMillis = null,
                steps = defaultSteps()
            )
            val flow = historyBySector.getOrPut(key(siteId, sectorId)) { MutableStateFlow(emptyList()) }
            flow.value = listOf(session) + flow.value
            return session
        }

        override suspend fun updateStepStatus(sessionId: String, stepCode: RetStepCode, status: RetStepStatus) {
            mutateSession(sessionId) { session ->
                val updatedSteps = session.steps.map { step ->
                    if (step.code == stepCode) step.copy(status = status) else step
                }
                val nextStatus = if (session.status == RetSessionStatus.CREATED && status != RetStepStatus.TODO) {
                    RetSessionStatus.IN_PROGRESS
                } else {
                    session.status
                }
                session.copy(
                    status = nextStatus,
                    steps = updatedSteps,
                    updatedAtEpochMillis = session.updatedAtEpochMillis + 1
                )
            }
        }

        override suspend fun updateSessionSummary(
            sessionId: String,
            status: RetSessionStatus,
            resultOutcome: RetResultOutcome,
            notes: String,
            resultSummary: String
        ) {
            mutateSession(sessionId) { session ->
                if (status == RetSessionStatus.COMPLETED && !session.completionGuard().canComplete) {
                    throw IllegalStateException("Required RET steps must be completed before closing the session.")
                }
                session.copy(
                    status = status,
                    resultOutcome = resultOutcome,
                    notes = notes,
                    resultSummary = resultSummary,
                    updatedAtEpochMillis = session.updatedAtEpochMillis + 1,
                    completedAtEpochMillis = if (status == RetSessionStatus.COMPLETED) {
                        session.updatedAtEpochMillis + 1
                    } else {
                        null
                    }
                )
            }
        }

        override suspend fun updateSessionGeospatialContext(
            sessionId: String,
            measurementZoneRadiusMeters: Int,
            measurementZoneExtensionReason: String,
            proximityModeEnabled: Boolean,
            proximityReferenceAltitudeMeters: Double?,
            proximityReferenceAltitudeSource: RetReferenceAltitudeSourceState
        ) {
            mutateSession(sessionId) { session ->
                session.copy(
                    measurementZoneRadiusMeters = measurementZoneRadiusMeters,
                    measurementZoneExtensionReason = measurementZoneExtensionReason,
                    proximityModeEnabled = proximityModeEnabled,
                    proximityReferenceAltitudeMeters = proximityReferenceAltitudeMeters,
                    proximityReferenceAltitudeSource = proximityReferenceAltitudeSource,
                    updatedAtEpochMillis = session.updatedAtEpochMillis + 1
                )
            }
        }

        private fun mutateSession(sessionId: String, transform: (RetGuidedSession) -> RetGuidedSession) {
            historyBySector.values.forEach { flow ->
                val current = flow.value
                val index = current.indexOfFirst { it.id == sessionId }
                if (index >= 0) {
                    val mutable = current.toMutableList()
                    mutable[index] = transform(mutable[index])
                    flow.value = mutable.sortedByDescending { it.createdAtEpochMillis }
                    return
                }
            }
            throw IllegalStateException("Session not found: $sessionId")
        }

        private fun key(siteId: String, sectorId: String): String = "$siteId::$sectorId"

        private fun defaultSteps(): List<RetGuidedStep> {
            return listOf(
                RetGuidedStep(
                    code = RetStepCode.CALIBRATION_PRECHECK,
                    required = true,
                    status = RetStepStatus.TODO
                ),
                RetGuidedStep(
                    code = RetStepCode.VALIDATION_CAPTURE,
                    required = true,
                    status = RetStepStatus.TODO
                ),
                RetGuidedStep(
                    code = RetStepCode.RESTORE_TILT_AND_RESULT,
                    required = true,
                    status = RetStepStatus.TODO
                )
            )
        }
    }

    private class FakeReportDraftRepository : ReportDraftRepository {
        private val draftsById = linkedMapOf<String, ReportDraft>()
        private val draftFlows = mutableMapOf<String, MutableStateFlow<ReportDraft?>>()
        private var nextId = 1

        var lastCreateSiteId: String? = null
            private set
        var lastOriginSessionId: String? = null
            private set
        var lastOriginSectorId: String? = null
            private set
        var lastOriginWorkflowType: ReportDraftOriginWorkflowType? = null
            private set
        var createDraftCalls: Int = 0
            private set

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
            val now = nextId.toLong()
            val draft = ReportDraft(
                id = "draft-$nextId",
                siteId = siteId,
                originSessionId = originSessionId,
                originSectorId = originSectorId,
                originWorkflowType = originWorkflowType,
                title = "Brouillon rapport",
                observation = "",
                revision = 1,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now
            )
            nextId += 1
            seedDraft(draft)
            return draft
        }

        override suspend fun updateDraft(draftId: String, title: String, observation: String): ReportDraft? {
            val existing = draftsById[draftId] ?: return null
            val updated = existing.copy(
                title = title,
                observation = observation,
                revision = existing.revision + 1,
                updatedAtEpochMillis = existing.updatedAtEpochMillis + 1
            )
            seedDraft(updated)
            return updated
        }

        override suspend fun findLatestLinkedDraft(
            siteId: String,
            originSessionId: String,
            originWorkflowType: ReportDraftOriginWorkflowType?
        ): ReportDraft? {
            return draftsById.values
                .filter { draft ->
                    draft.siteId == siteId &&
                        draft.originSessionId == originSessionId &&
                        draft.originWorkflowType == originWorkflowType
                }
                .maxByOrNull { it.updatedAtEpochMillis }
        }

        override fun observeDraft(draftId: String): Flow<ReportDraft?> {
            return draftFlows.getOrPut(draftId) { MutableStateFlow(draftsById[draftId]) }
        }

        override fun listDraftsBySite(siteId: String): Flow<List<ReportDraft>> {
            return flowOf(
                draftsById.values.filter { it.siteId == siteId }.sortedByDescending { it.updatedAtEpochMillis }
            )
        }

        fun seedDraft(draft: ReportDraft) {
            draftsById[draft.id] = draft
            draftFlows.getOrPut(draft.id) { MutableStateFlow(null) }.value = draft
        }
    }

    private fun sampleSiteDetail(withTechnicalReferenceAltitude: Boolean = true): SiteDetail {
        val antennas = if (withTechnicalReferenceAltitude) {
            listOf(
                SiteAntenna(
                    id = "ant-1",
                    sectorId = "site-1-sector-S0",
                    reference = "RET-A1",
                    referenceAltitudeMeters = 118.0,
                    installedState = "INSTALLED",
                    forecastState = null,
                    tiltConfiguredDegrees = null,
                    tiltObservedDegrees = null,
                    documentationRef = null
                )
            )
        } else {
            emptyList()
        }
        return SiteDetail(
            id = "site-1",
            externalCode = "QZ-001",
            name = "Quartz Site 1",
            latitude = 33.5899,
            longitude = -7.6039,
            status = "ACTIVE",
            sectorsInService = 1,
            sectorsForecast = 0,
            indoorOnly = false,
            updatedAtEpochMillis = 1L,
            sectors = listOf(
                SiteSector(
                    id = "site-1-sector-S0",
                    siteId = "site-1",
                    code = "S0",
                    azimuthDegrees = 30,
                    status = "ACTIVE",
                    hasConnectedCell = true,
                    antennas = antennas
                )
            )
        )
    }
}
