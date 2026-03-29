package com.quartz.platform.presentation.ret.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quartz.platform.R
import com.quartz.platform.core.geo.GeoMath
import com.quartz.platform.core.text.UiStrings
import com.quartz.platform.domain.model.GeoCoordinate
import com.quartz.platform.domain.model.RetGeospatialPolicy
import com.quartz.platform.domain.model.RetGuidedSession
import com.quartz.platform.domain.model.RetProximityEligibilityState
import com.quartz.platform.domain.model.RetReferenceAltitudeSourceState
import com.quartz.platform.domain.model.RetResultOutcome
import com.quartz.platform.domain.model.RetSessionStatus
import com.quartz.platform.domain.model.RetStepCode
import com.quartz.platform.domain.model.RetStepStatus
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import com.quartz.platform.domain.model.SiteSector
import com.quartz.platform.domain.model.UserLocation
import com.quartz.platform.domain.usecase.CreateSectorRetSessionUseCase
import com.quartz.platform.domain.usecase.GetLastKnownUserLocationUseCase
import com.quartz.platform.domain.usecase.ObserveSectorRetSessionHistoryUseCase
import com.quartz.platform.domain.usecase.ObserveSiteDetailUseCase
import com.quartz.platform.domain.usecase.OpenOrCreateGuidedSessionReportDraftUseCase
import com.quartz.platform.domain.usecase.UpdateRetSessionGeospatialContextUseCase
import com.quartz.platform.domain.usecase.UpdateRetSessionSummaryUseCase
import com.quartz.platform.domain.usecase.UpdateRetStepStatusUseCase
import com.quartz.platform.presentation.navigation.QuartzDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RetGuidedSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeSiteDetailUseCase: ObserveSiteDetailUseCase,
    private val observeSectorRetSessionHistoryUseCase: ObserveSectorRetSessionHistoryUseCase,
    private val createSectorRetSessionUseCase: CreateSectorRetSessionUseCase,
    private val getLastKnownUserLocationUseCase: GetLastKnownUserLocationUseCase,
    private val updateRetStepStatusUseCase: UpdateRetStepStatusUseCase,
    private val updateRetSessionGeospatialContextUseCase: UpdateRetSessionGeospatialContextUseCase,
    private val updateRetSessionSummaryUseCase: UpdateRetSessionSummaryUseCase,
    private val openOrCreateGuidedSessionReportDraftUseCase: OpenOrCreateGuidedSessionReportDraftUseCase,
    private val uiStrings: UiStrings
) : ViewModel() {

    private val siteId: String = checkNotNull(savedStateHandle[QuartzDestination.RetGuidedSession.ARG_SITE_ID])
    private val sectorId: String = checkNotNull(savedStateHandle[QuartzDestination.RetGuidedSession.ARG_SECTOR_ID])

    private val mutableState = MutableStateFlow(
        RetGuidedSessionUiState(
            siteId = siteId,
            sectorId = sectorId
        )
    )
    val uiState: StateFlow<RetGuidedSessionUiState> = mutableState.asStateFlow()
    private val _events = MutableSharedFlow<RetGuidedSessionEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()
    private val userLocation = MutableStateFlow<UserLocation?>(null)

    init {
        observeContext()
        refreshUserLocation()
    }

    fun onCreateSessionClicked() {
        val current = mutableState.value
        if (current.isCreatingSession) return
        if (current.sectorCode.isBlank()) {
            mutableState.update { state ->
                state.copy(errorMessage = uiStrings.get(R.string.error_ret_sector_not_found))
            }
            return
        }

        viewModelScope.launch {
            mutableState.update { state ->
                state.copy(
                    isCreatingSession = true,
                    errorMessage = null,
                    infoMessage = null,
                    completionGuardMessage = null
                )
            }
            runCatching {
                createSectorRetSessionUseCase(
                    siteId = siteId,
                    sectorId = sectorId,
                    sectorCode = mutableState.value.sectorCode
                )
            }.onFailure { throwable ->
                mutableState.update { state ->
                    state.copy(
                        isCreatingSession = false,
                        errorMessage = throwable.message ?: uiStrings.get(R.string.error_ret_create_session)
                    )
                }
            }.onSuccess { created ->
                mutableState.update { state ->
                    state.copy(
                        isCreatingSession = false,
                        infoMessage = uiStrings.get(R.string.info_ret_session_created),
                        latestSessionId = created.id,
                        hasUnsavedChanges = false,
                        completionGuardMessage = null
                    )
                }
            }
        }
    }

    fun onResumeLatestClicked() {
        val latestId = mutableState.value.sessionHistory.firstOrNull()?.id ?: return
        selectSessionById(latestId)
    }

    fun onSelectHistorySessionClicked(sessionId: String) {
        selectSessionById(sessionId)
    }

    fun onStepStatusSelected(stepCode: RetStepCode, status: RetStepStatus) {
        val sessionId = mutableState.value.session?.id ?: return
        viewModelScope.launch {
            runCatching {
                updateRetStepStatusUseCase(
                    sessionId = sessionId,
                    stepCode = stepCode,
                    status = status
                )
            }.onFailure { throwable ->
                mutableState.update { state ->
                    state.copy(errorMessage = throwable.message ?: uiStrings.get(R.string.error_ret_update_step))
                }
            }
        }
    }

    fun onMeasurementZoneExtensionReasonChanged(value: String) {
        mutableState.update { state ->
            state.copy(
                measurementZoneExtensionReasonInput = value,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                infoMessage = null,
                errorMessage = null
            )
        }
    }

    fun onProximityReferenceAltitudeChanged(value: String) {
        val sanitized = value
            .replace(',', '.')
            .filterIndexed { index, character ->
                character.isDigit() || character == '.' || (character == '-' && index == 0)
            }
        val current = mutableState.value
        val altitudeResolution = resolveReferenceAltitude(
            proximityReferenceAltitudeInput = sanitized,
            technicalReferenceAltitudeMeters = current.technicalReferenceAltitudeMeters
        )
        mutableState.update { state ->
            state.copy(
                proximityReferenceAltitudeInput = sanitized,
                effectiveReferenceAltitudeMeters = altitudeResolution.effectiveAltitudeMeters,
                proximityReferenceAltitudeSource = altitudeResolution.sourceState,
                proximityEligibilityState = RetGeospatialPolicy.evaluateProximityEligibility(
                    distanceMeters = state.distanceToMeasurementZoneMeters,
                    userAltitudeMeters = state.userAltitudeMeters,
                    userVerticalAccuracyMeters = state.userAltitudeVerticalAccuracyMeters,
                    referenceAltitudeMeters = altitudeResolution.effectiveAltitudeMeters
                ),
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                infoMessage = null,
                errorMessage = null
            )
        }
    }

    fun onExtendMeasurementZoneClicked() {
        val current = mutableState.value
        val session = current.session ?: return
        val altitudeResolution = resolveReferenceAltitudeForPersistence(current) ?: return
        if (current.proximityModeEnabled) {
            mutableState.update { state ->
                state.copy(errorMessage = uiStrings.get(R.string.error_ret_zone_extension_requires_proximity_off))
            }
            return
        }
        if (RetGeospatialPolicy.isExtensionReasonRequired(current.measurementZoneRadiusMeters) &&
            current.measurementZoneExtensionReasonInput.isBlank()
        ) {
            mutableState.update { state ->
                state.copy(errorMessage = uiStrings.get(R.string.error_ret_zone_extension_reason_required))
            }
            return
        }

        val nextRadius = RetGeospatialPolicy.clampMeasurementZoneRadius(
            current.measurementZoneRadiusMeters + RetGeospatialPolicy.MEASUREMENT_ZONE_EXTENSION_STEP_METERS
        )
        val extensionReason = current.measurementZoneExtensionReasonInput.trim()
        if (nextRadius == current.measurementZoneRadiusMeters) {
            mutableState.update { state ->
                state.copy(infoMessage = uiStrings.get(R.string.info_ret_zone_extension_max_reached))
            }
            return
        }

        persistGeospatialContext(
            sessionId = session.id,
            measurementZoneRadiusMeters = nextRadius,
            measurementZoneExtensionReason = extensionReason,
            proximityModeEnabled = current.proximityModeEnabled,
            proximityReferenceAltitudeMeters = altitudeResolution.effectiveAltitudeMeters,
            proximityReferenceAltitudeSource = altitudeResolution.sourceState,
            successMessageRes = R.string.info_ret_zone_extension_saved
        )
    }

    fun onResetMeasurementZoneClicked() {
        val current = mutableState.value
        val session = current.session ?: return
        val altitudeResolution = resolveReferenceAltitudeForPersistence(current) ?: return

        persistGeospatialContext(
            sessionId = session.id,
            measurementZoneRadiusMeters = RetGeospatialPolicy.DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS,
            measurementZoneExtensionReason = "",
            proximityModeEnabled = current.proximityModeEnabled,
            proximityReferenceAltitudeMeters = altitudeResolution.effectiveAltitudeMeters,
            proximityReferenceAltitudeSource = altitudeResolution.sourceState,
            successMessageRes = R.string.info_ret_zone_extension_reset
        )
    }

    fun onToggleProximityModeClicked(enabled: Boolean) {
        val current = mutableState.value
        val session = current.session ?: return
        val altitudeResolution = resolveReferenceAltitudeForPersistence(current) ?: return

        if (enabled && current.proximityEligibilityState != RetProximityEligibilityState.ELIGIBLE) {
            val messageRes = when (current.proximityEligibilityState) {
                RetProximityEligibilityState.SUPPORTED -> {
                    R.string.error_ret_proximity_requires_reference_altitude
                }
                RetProximityEligibilityState.UNAVAILABLE -> {
                    R.string.error_ret_proximity_altitude_unavailable
                }
                RetProximityEligibilityState.INELIGIBLE -> {
                    R.string.error_ret_proximity_not_eligible
                }
                RetProximityEligibilityState.ELIGIBLE -> {
                    R.string.error_ret_proximity_not_eligible
                }
            }
            mutableState.update { state ->
                state.copy(errorMessage = uiStrings.get(messageRes))
            }
            return
        }

        persistGeospatialContext(
            sessionId = session.id,
            measurementZoneRadiusMeters = current.measurementZoneRadiusMeters,
            measurementZoneExtensionReason = current.measurementZoneExtensionReasonInput.trim(),
            proximityModeEnabled = enabled,
            proximityReferenceAltitudeMeters = altitudeResolution.effectiveAltitudeMeters,
            proximityReferenceAltitudeSource = altitudeResolution.sourceState,
            successMessageRes = if (enabled) {
                R.string.info_ret_proximity_enabled
            } else {
                R.string.info_ret_proximity_disabled
            }
        )
    }

    fun onRefreshUserLocationClicked() {
        refreshUserLocation()
    }

    fun onNotesChanged(value: String) {
        mutableState.update { state ->
            state.copy(
                notesInput = value,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                infoMessage = null,
                errorMessage = null
            )
        }
    }

    fun onResultSummaryChanged(value: String) {
        mutableState.update { state ->
            state.copy(
                resultSummaryInput = value,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                infoMessage = null,
                errorMessage = null
            )
        }
    }

    fun onSessionStatusSelected(status: RetSessionStatus) {
        val session = mutableState.value.session
        val completionValidationMessage = if (session != null) {
            validateCompletionRequirements(status = status, session = session)
        } else {
            null
        }
        if (completionValidationMessage != null) {
            mutableState.update { state ->
                state.copy(
                    completionGuardMessage = completionValidationMessage,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            return
        }

        mutableState.update { state ->
            state.copy(
                selectedStatus = status,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                infoMessage = null,
                errorMessage = null
            )
        }
    }

    fun onResultOutcomeSelected(outcome: RetResultOutcome) {
        mutableState.update { state ->
            state.copy(
                selectedOutcome = outcome,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                infoMessage = null,
                errorMessage = null
            )
        }
    }

    fun onSaveSummaryClicked() {
        val current = mutableState.value
        val session = current.session ?: return
        if (current.isSavingSummary || !current.hasUnsavedChanges) return

        val completionValidationMessage = validateCompletionRequirements(
            status = current.selectedStatus,
            session = session
        )
        if (completionValidationMessage != null) {
            mutableState.update { state ->
                state.copy(completionGuardMessage = completionValidationMessage)
            }
            return
        }

        viewModelScope.launch {
            mutableState.update { state ->
                state.copy(isSavingSummary = true, errorMessage = null, infoMessage = null)
            }
            runCatching {
                updateRetSessionSummaryUseCase(
                    sessionId = session.id,
                    status = mutableState.value.selectedStatus,
                    resultOutcome = mutableState.value.selectedOutcome,
                    notes = mutableState.value.notesInput.trim(),
                    resultSummary = mutableState.value.resultSummaryInput.trim()
                )
            }.onFailure { throwable ->
                mutableState.update { state ->
                    state.copy(
                        isSavingSummary = false,
                        errorMessage = throwable.message ?: uiStrings.get(R.string.error_ret_save_summary)
                    )
                }
            }.onSuccess {
                mutableState.update { state ->
                    state.copy(
                        isSavingSummary = false,
                        hasUnsavedChanges = false,
                        completionGuardMessage = null,
                        infoMessage = uiStrings.get(R.string.info_ret_summary_saved)
                    )
                }
            }
        }
    }

    fun onCreateReportDraftClicked() {
        val current = mutableState.value
        val currentSession = current.session ?: return
        if (current.isCreatingDraft) return

        viewModelScope.launch {
            mutableState.update { state ->
                state.copy(
                    isCreatingDraft = true,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            runCatching {
                openOrCreateGuidedSessionReportDraftUseCase(
                    siteId = currentSession.siteId,
                    originSessionId = currentSession.id,
                    originSectorId = currentSession.sectorId,
                    originWorkflowType = ReportDraftOriginWorkflowType.RET
                )
            }.onFailure { throwable ->
                mutableState.update { state ->
                    state.copy(
                        isCreatingDraft = false,
                        errorMessage = throwable.message ?: uiStrings.get(R.string.error_ret_create_report_draft)
                    )
                }
            }.onSuccess { result ->
                _events.tryEmit(RetGuidedSessionEvent.OpenDraft(result.draft.id))
                mutableState.update { state ->
                    state.copy(
                        isCreatingDraft = false,
                        infoMessage = if (result.created) {
                            uiStrings.get(R.string.info_local_draft_created)
                        } else {
                            uiStrings.get(R.string.info_ret_opened_linked_draft)
                        }
                    )
                }
            }
        }
    }

    private fun observeContext() {
        viewModelScope.launch {
            combine(
                observeSiteDetailUseCase(siteId),
                observeSectorRetSessionHistoryUseCase(siteId, sectorId),
                userLocation
            ) { site, history, currentUserLocation ->
                mutableState.update { current ->
                    val siteName = site?.name.orEmpty()
                    val siteCode = site?.externalCode.orEmpty()
                    val sector = site?.sectors?.firstOrNull { it.id == sectorId }
                    val sectorCode = sector?.code.orEmpty()
                    val selectedSessionId = current.latestSessionId?.takeIf { selected ->
                        history.any { it.id == selected }
                    } ?: history.firstOrNull()?.id
                    val selectedSession = history.firstOrNull { it.id == selectedSessionId }
                    val shouldHydrate = selectedSession != null &&
                        (!current.hasUnsavedChanges || current.session?.id != selectedSession.id)

                    val siteCoordinate = site?.let { GeoCoordinate(it.latitude, it.longitude) }
                    val measurementZoneCoordinate = computeMeasurementZoneCoordinate(siteCoordinate, sector)
                    val technicalReferenceAltitude = sector?.resolveTechnicalReferenceAltitude()
                    val nextReferenceAltitudeInput = if (shouldHydrate) {
                        if (selectedSession?.proximityReferenceAltitudeSource ==
                            RetReferenceAltitudeSourceState.OPERATOR_OVERRIDE
                        ) {
                            formatOptionalAltitude(selectedSession.proximityReferenceAltitudeMeters)
                        } else {
                            ""
                        }
                    } else {
                        current.proximityReferenceAltitudeInput
                    }
                    val measurementZoneRadius = selectedSession?.measurementZoneRadiusMeters
                        ?: RetGeospatialPolicy.DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS
                    val distanceMeters = if (currentUserLocation != null && measurementZoneCoordinate != null) {
                        GeoMath.distanceMeters(
                            GeoCoordinate(currentUserLocation.latitude, currentUserLocation.longitude),
                            measurementZoneCoordinate
                        )
                    } else {
                        null
                    }
                    val altitudeResolution = resolveReferenceAltitude(
                        proximityReferenceAltitudeInput = nextReferenceAltitudeInput,
                        technicalReferenceAltitudeMeters = technicalReferenceAltitude
                    )
                    val proximityEligibilityState = RetGeospatialPolicy.evaluateProximityEligibility(
                        distanceMeters = distanceMeters,
                        userAltitudeMeters = currentUserLocation?.altitudeMeters,
                        userVerticalAccuracyMeters = currentUserLocation?.verticalAccuracyMeters,
                        referenceAltitudeMeters = altitudeResolution.effectiveAltitudeMeters
                    )

                    current.copy(
                        isLoading = false,
                        siteLabel = listOf(siteName, siteCode).filter { it.isNotBlank() }.joinToString(" - "),
                        sectorCode = sectorCode,
                        siteCoordinate = siteCoordinate,
                        measurementZoneCoordinate = measurementZoneCoordinate,
                        measurementZoneRadiusMeters = if (shouldHydrate) {
                            measurementZoneRadius
                        } else {
                            current.measurementZoneRadiusMeters
                        },
                        measurementZoneExtensionReasonInput = if (shouldHydrate) {
                            selectedSession?.measurementZoneExtensionReason.orEmpty()
                        } else {
                            current.measurementZoneExtensionReasonInput
                        },
                        proximityReferenceAltitudeInput = nextReferenceAltitudeInput,
                        technicalReferenceAltitudeMeters = technicalReferenceAltitude,
                        effectiveReferenceAltitudeMeters = altitudeResolution.effectiveAltitudeMeters,
                        proximityReferenceAltitudeSource = altitudeResolution.sourceState,
                        proximityModeEnabled = if (shouldHydrate) {
                            selectedSession?.proximityModeEnabled == true
                        } else {
                            current.proximityModeEnabled
                        },
                        userLocation = currentUserLocation,
                        userAltitudeMeters = currentUserLocation?.altitudeMeters,
                        userAltitudeVerticalAccuracyMeters = currentUserLocation?.verticalAccuracyMeters,
                        distanceToMeasurementZoneMeters = distanceMeters,
                        isInsideMeasurementZone = distanceMeters?.let { it <= measurementZoneRadius },
                        proximityEligibilityState = proximityEligibilityState,
                        sessionHistory = history,
                        latestSessionId = selectedSessionId,
                        session = selectedSession,
                        selectedStatus = if (shouldHydrate) selectedSession.status else current.selectedStatus,
                        selectedOutcome = if (shouldHydrate) {
                            selectedSession.resultOutcome
                        } else {
                            current.selectedOutcome
                        },
                        notesInput = if (shouldHydrate) selectedSession.notes else current.notesInput,
                        resultSummaryInput = if (shouldHydrate) {
                            selectedSession.resultSummary
                        } else {
                            current.resultSummaryInput
                        },
                        hasUnsavedChanges = if (shouldHydrate) false else current.hasUnsavedChanges,
                        completionGuardMessage = if (shouldHydrate) null else current.completionGuardMessage,
                        errorMessage = if (site == null || sector == null) {
                            uiStrings.get(R.string.error_ret_sector_not_found)
                        } else {
                            current.errorMessage
                        }
                    )
                }
            }
                .catch { throwable ->
                    mutableState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: uiStrings.get(R.string.error_ret_observe_session)
                        )
                    }
                }
                .collect {}
        }
    }

    private fun refreshUserLocation() {
        viewModelScope.launch {
            runCatching { getLastKnownUserLocationUseCase() }
                .onSuccess { location -> userLocation.value = location }
                .onFailure { throwable ->
                    mutableState.update { state ->
                        state.copy(
                            infoMessage = throwable.message ?: uiStrings.get(R.string.info_ret_user_location_unavailable),
                            errorMessage = null
                        )
                    }
                }
        }
    }

    private fun persistGeospatialContext(
        sessionId: String,
        measurementZoneRadiusMeters: Int,
        measurementZoneExtensionReason: String,
        proximityModeEnabled: Boolean,
        proximityReferenceAltitudeMeters: Double?,
        proximityReferenceAltitudeSource: RetReferenceAltitudeSourceState,
        successMessageRes: Int
    ) {
        viewModelScope.launch {
            runCatching {
                updateRetSessionGeospatialContextUseCase(
                    sessionId = sessionId,
                    measurementZoneRadiusMeters = measurementZoneRadiusMeters,
                    measurementZoneExtensionReason = measurementZoneExtensionReason,
                    proximityModeEnabled = proximityModeEnabled,
                    proximityReferenceAltitudeMeters = proximityReferenceAltitudeMeters,
                    proximityReferenceAltitudeSource = proximityReferenceAltitudeSource
                )
            }.onFailure { throwable ->
                mutableState.update { state ->
                    state.copy(
                        errorMessage = throwable.message ?: uiStrings.get(R.string.error_ret_update_geospatial_context)
                    )
                }
            }.onSuccess {
                mutableState.update { state ->
                    state.copy(
                        hasUnsavedChanges = true,
                        infoMessage = uiStrings.get(successMessageRes),
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun SiteSector.resolveTechnicalReferenceAltitude(): Double? {
        return antennas.firstNotNullOfOrNull { antenna -> antenna.referenceAltitudeMeters }
    }

    private fun resolveReferenceAltitudeForPersistence(
        state: RetGuidedSessionUiState
    ): ReferenceAltitudeResolution? {
        val parsedOverride = parseOptionalAltitudeInput(state.proximityReferenceAltitudeInput)
        if (state.proximityReferenceAltitudeInput.isNotBlank() && parsedOverride == null) {
            mutableState.update { current ->
                current.copy(
                    errorMessage = uiStrings.get(R.string.error_ret_proximity_reference_altitude_invalid)
                )
            }
            return null
        }
        return resolveReferenceAltitude(
            proximityReferenceAltitudeInput = state.proximityReferenceAltitudeInput,
            technicalReferenceAltitudeMeters = state.technicalReferenceAltitudeMeters
        )
    }

    private fun resolveReferenceAltitude(
        proximityReferenceAltitudeInput: String,
        technicalReferenceAltitudeMeters: Double?
    ): ReferenceAltitudeResolution {
        val operatorOverrideAltitude = parseOptionalAltitudeInput(proximityReferenceAltitudeInput)
        val sourceState = RetGeospatialPolicy.resolveReferenceAltitudeSource(
            technicalReferenceAltitudeMeters = technicalReferenceAltitudeMeters,
            operatorOverrideAltitudeMeters = operatorOverrideAltitude
        )
        val effectiveAltitude = RetGeospatialPolicy.resolveEffectiveReferenceAltitudeMeters(
            sourceState = sourceState,
            technicalReferenceAltitudeMeters = technicalReferenceAltitudeMeters,
            operatorOverrideAltitudeMeters = operatorOverrideAltitude
        )
        return ReferenceAltitudeResolution(
            sourceState = sourceState,
            effectiveAltitudeMeters = effectiveAltitude
        )
    }

    private fun parseOptionalAltitudeInput(raw: String): Double? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        return trimmed.toDoubleOrNull()
    }

    private fun formatOptionalAltitude(value: Double?): String {
        return when {
            value == null -> ""
            value % 1.0 == 0.0 -> value.toInt().toString()
            else -> value.toString()
        }
    }

    private fun computeMeasurementZoneCoordinate(
        siteCoordinate: GeoCoordinate?,
        sector: SiteSector?
    ): GeoCoordinate? {
        if (siteCoordinate == null) return null
        val azimuth = sector?.azimuthDegrees ?: return siteCoordinate
        return GeoMath.offset(
            origin = siteCoordinate,
            bearingDegrees = azimuth.toDouble(),
            distanceMeters = RetGeospatialPolicy.TARGET_OFFSET_FROM_SITE_METERS
        )
    }

    private fun validateCompletionRequirements(
        status: RetSessionStatus,
        session: RetGuidedSession
    ): String? {
        if (status != RetSessionStatus.COMPLETED) return null
        return if (session.completionGuard().canComplete) {
            null
        } else {
            uiStrings.get(R.string.error_ret_complete_requires_required_steps)
        }
    }

    private fun selectSessionById(sessionId: String) {
        mutableState.update { state ->
            val selected = state.sessionHistory.firstOrNull { it.id == sessionId } ?: return@update state
            val nextReferenceInput = if (
                selected.proximityReferenceAltitudeSource == RetReferenceAltitudeSourceState.OPERATOR_OVERRIDE
            ) {
                formatOptionalAltitude(selected.proximityReferenceAltitudeMeters)
            } else {
                ""
            }
            val altitudeResolution = resolveReferenceAltitude(
                proximityReferenceAltitudeInput = nextReferenceInput,
                technicalReferenceAltitudeMeters = state.technicalReferenceAltitudeMeters
            )
            state.copy(
                latestSessionId = sessionId,
                session = selected,
                measurementZoneRadiusMeters = selected.measurementZoneRadiusMeters,
                measurementZoneExtensionReasonInput = selected.measurementZoneExtensionReason,
                proximityReferenceAltitudeInput = nextReferenceInput,
                effectiveReferenceAltitudeMeters = altitudeResolution.effectiveAltitudeMeters,
                proximityReferenceAltitudeSource = altitudeResolution.sourceState,
                proximityModeEnabled = selected.proximityModeEnabled,
                selectedStatus = selected.status,
                selectedOutcome = selected.resultOutcome,
                notesInput = selected.notes,
                resultSummaryInput = selected.resultSummary,
                hasUnsavedChanges = false,
                completionGuardMessage = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }
}

private data class ReferenceAltitudeResolution(
    val sourceState: RetReferenceAltitudeSourceState,
    val effectiveAltitudeMeters: Double?
)

sealed interface RetGuidedSessionEvent {
    data class OpenDraft(val draftId: String) : RetGuidedSessionEvent
}
