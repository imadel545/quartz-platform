package com.quartz.platform.presentation.performance.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quartz.platform.R
import com.quartz.platform.core.text.UiStrings
import com.quartz.platform.domain.model.PerformanceSession
import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.PerformanceStepCode
import com.quartz.platform.domain.model.PerformanceStepStatus
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.model.NetworkStatus
import com.quartz.platform.domain.model.QosCompletionIssue
import com.quartz.platform.domain.model.QosExecutionEventType
import com.quartz.platform.domain.model.QosExecutionIssueCode
import com.quartz.platform.domain.model.QosExecutionTimelineEvent
import com.quartz.platform.domain.model.QosExecutionSnapshot
import com.quartz.platform.domain.model.QosPreflightIssue
import com.quartz.platform.domain.model.QosRunPlanItemStatus
import com.quartz.platform.domain.model.QosRunnerAction
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import com.quartz.platform.domain.model.QosRunSummary
import com.quartz.platform.domain.model.QosFamilyExecutionResult
import com.quartz.platform.domain.model.QosFamilyExecutionStatus
import com.quartz.platform.domain.model.QosTestFamily
import com.quartz.platform.domain.model.ThroughputMetrics
import com.quartz.platform.domain.model.assessQosFamilyPreflight
import com.quartz.platform.domain.model.assessQosCompletion
import com.quartz.platform.domain.model.computeQosRunPlan
import com.quartz.platform.domain.model.computeQosFamilyRunCoverage
import com.quartz.platform.domain.model.deriveQosExecutionSnapshot
import com.quartz.platform.domain.model.deriveQosPassFailCounters
import com.quartz.platform.domain.model.qosExecutionEventSortOrder
import com.quartz.platform.domain.usecase.CreateSitePerformanceSessionUseCase
import com.quartz.platform.domain.usecase.EnsureDefaultQosScriptsUseCase
import com.quartz.platform.domain.usecase.GetCurrentBatterySnapshotUseCase
import com.quartz.platform.domain.usecase.GetLastKnownUserLocationUseCase
import com.quartz.platform.domain.usecase.ObserveSiteDetailUseCase
import com.quartz.platform.domain.usecase.ObserveNetworkStatusUseCase
import com.quartz.platform.domain.usecase.ObserveQosScriptsUseCase
import com.quartz.platform.domain.usecase.ObserveSitePerformanceSessionHistoryUseCase
import com.quartz.platform.domain.usecase.OpenOrCreateGuidedSessionReportDraftUseCase
import com.quartz.platform.domain.usecase.UpdatePerformanceSessionExecutionUseCase
import com.quartz.platform.domain.usecase.UpdatePerformanceStepStatusUseCase
import com.quartz.platform.domain.usecase.UpsertQosScriptUseCase
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
class PerformanceSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeSiteDetailUseCase: ObserveSiteDetailUseCase,
    private val observeSitePerformanceSessionHistoryUseCase: ObserveSitePerformanceSessionHistoryUseCase,
    private val observeQosScriptsUseCase: ObserveQosScriptsUseCase,
    private val observeNetworkStatusUseCase: ObserveNetworkStatusUseCase,
    private val getCurrentBatterySnapshotUseCase: GetCurrentBatterySnapshotUseCase,
    private val getLastKnownUserLocationUseCase: GetLastKnownUserLocationUseCase,
    private val ensureDefaultQosScriptsUseCase: EnsureDefaultQosScriptsUseCase,
    private val createSitePerformanceSessionUseCase: CreateSitePerformanceSessionUseCase,
    private val upsertQosScriptUseCase: UpsertQosScriptUseCase,
    private val openOrCreateGuidedSessionReportDraftUseCase: OpenOrCreateGuidedSessionReportDraftUseCase,
    private val updatePerformanceStepStatusUseCase: UpdatePerformanceStepStatusUseCase,
    private val updatePerformanceSessionExecutionUseCase: UpdatePerformanceSessionExecutionUseCase,
    private val uiStrings: UiStrings
) : ViewModel() {

    private val siteId: String = checkNotNull(savedStateHandle[QuartzDestination.PerformanceSession.ARG_SITE_ID])
    private val mutableState = MutableStateFlow(
        PerformanceSessionUiState(siteId = siteId)
    )
    val uiState: StateFlow<PerformanceSessionUiState> = mutableState.asStateFlow()
    private val _events = MutableSharedFlow<PerformanceSessionEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            ensureDefaultQosScriptsUseCase()
        }
        observeDeviceNetworkStatus()
        observeContext()
    }

    fun onCreateThroughputSessionClicked() {
        createSession(PerformanceWorkflowType.THROUGHPUT)
    }

    fun onCreateQosSessionClicked() {
        createSession(PerformanceWorkflowType.QOS_SCRIPT)
    }

    fun onSelectHistorySessionClicked(sessionId: String) {
        mutableState.update { state ->
            state.copy(
                selectedSessionId = sessionId,
                hasUnsavedChanges = false,
                completionGuardMessage = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun onOpenLinkedDraftClicked() {
        val session = mutableState.value.session ?: return
        viewModelScope.launch {
            runCatching {
                openOrCreateGuidedSessionReportDraftUseCase(
                    siteId = session.siteId,
                    originSessionId = session.id,
                    originSectorId = null,
                    originWorkflowType = ReportDraftOriginWorkflowType.PERFORMANCE
                )
            }.onFailure { throwable ->
                mutableState.update { state ->
                    state.copy(
                        errorMessage = throwable.message ?: uiStrings.get(R.string.error_create_linked_report_draft),
                        infoMessage = null
                    )
                }
            }.onSuccess { result ->
                val infoRes = if (result.created) {
                    R.string.info_created_linked_report_draft
                } else {
                    R.string.info_opened_existing_linked_report_draft
                }
                mutableState.update { state ->
                    state.copy(infoMessage = uiStrings.get(infoRes), errorMessage = null)
                }
                _events.tryEmit(PerformanceSessionEvent.OpenDraft(result.draft.id))
            }
        }
    }

    fun onOperatorSelected(operator: String?) {
        mutableState.update { state ->
            state.copy(
                selectedOperator = operator,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun onTechnologySelected(technology: String?) {
        mutableState.update { state ->
            state.copy(
                selectedTechnology = technology,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun onStepStatusSelected(stepCode: PerformanceStepCode, status: PerformanceStepStatus) {
        val sessionId = mutableState.value.session?.id ?: return
        viewModelScope.launch {
            runCatching {
                updatePerformanceStepStatusUseCase(
                    sessionId = sessionId,
                    stepCode = stepCode,
                    status = status
                )
            }.onFailure { throwable ->
                mutableState.update { state ->
                    state.copy(
                        errorMessage = throwable.message
                            ?: uiStrings.get(R.string.error_performance_update_step)
                    )
                }
            }
        }
    }

    fun onSessionStatusSelected(status: PerformanceSessionStatus) {
        val currentSession = mutableState.value.session
        if (currentSession != null && status == PerformanceSessionStatus.COMPLETED) {
            val guard = currentSession.completionGuard()
            if (!guard.canComplete) {
                mutableState.update { state ->
                    state.copy(
                        completionGuardMessage = uiStrings.get(
                            R.string.error_performance_complete_requires_required_steps
                        ),
                        errorMessage = null,
                        infoMessage = null
                    )
                }
                return
            }
        }
        mutableState.update { state ->
            deriveQosUiState(
                state.copy(
                    selectedStatus = status,
                    hasUnsavedChanges = true,
                    completionGuardMessage = null,
                    errorMessage = null,
                    infoMessage = null
                )
            )
        }
    }

    fun onPrerequisiteNetworkChanged(value: Boolean) {
        updateField { copy(prerequisiteNetworkReady = value) }
    }

    fun onPrerequisiteBatteryChanged(value: Boolean) {
        updateField { copy(prerequisiteBatterySufficient = value) }
    }

    fun onPrerequisiteLocationChanged(value: Boolean) {
        updateField { copy(prerequisiteLocationReady = value) }
    }

    fun onRefreshDeviceDiagnosticsClicked() {
        viewModelScope.launch {
            mutableState.update { state ->
                state.copy(
                    isRefreshingDeviceDiagnostics = true,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            val batterySnapshot = runCatching { getCurrentBatterySnapshotUseCase() }.getOrNull()
            val userLocation = runCatching { getLastKnownUserLocationUseCase() }.getOrNull()
            val hasLocation = userLocation != null
            val capturedAt = System.currentTimeMillis()
            mutableState.update { state ->
                deriveQosUiState(
                    state.copy(
                        observedBatteryLevelPercent = batterySnapshot?.levelPercent,
                        observedBatteryIsCharging = batterySnapshot?.isCharging,
                        observedLocationAvailable = hasLocation,
                        observedSignalsCapturedAtEpochMillis = capturedAt,
                        isRefreshingDeviceDiagnostics = false,
                        infoMessage = uiStrings.get(R.string.info_performance_device_diagnostics_refreshed)
                    )
                )
            }
        }
    }

    fun onApplyDeviceDiagnosticsClicked() {
        mutableState.update { state ->
            val networkReady = state.observedNetworkStatus == NetworkStatus.AVAILABLE
            val batteryReady = state.observedBatterySufficient == true
            val locationReady = state.observedLocationAvailable == true
            deriveQosUiState(
                state.copy(
                    prerequisiteNetworkReady = networkReady,
                    prerequisiteBatterySufficient = batteryReady,
                    prerequisiteLocationReady = locationReady,
                    hasUnsavedChanges = true,
                    completionGuardMessage = null,
                    errorMessage = null,
                    infoMessage = uiStrings.get(R.string.info_performance_device_diagnostics_applied)
                )
            )
        }
    }

    fun onThroughputDownloadChanged(value: String) = updateField { copy(throughputDownloadInput = sanitizeDecimal(value)) }
    fun onThroughputUploadChanged(value: String) = updateField { copy(throughputUploadInput = sanitizeDecimal(value)) }
    fun onThroughputLatencyChanged(value: String) = updateField { copy(throughputLatencyInput = sanitizeInteger(value)) }
    fun onThroughputMinDownloadChanged(value: String) = updateField { copy(throughputMinDownloadInput = sanitizeDecimal(value)) }
    fun onThroughputMinUploadChanged(value: String) = updateField { copy(throughputMinUploadInput = sanitizeDecimal(value)) }
    fun onThroughputMaxLatencyChanged(value: String) = updateField { copy(throughputMaxLatencyInput = sanitizeInteger(value)) }

    fun onQosScriptSelected(scriptId: String, scriptName: String) {
        val script = mutableState.value.availableQosScripts.firstOrNull { it.id == scriptId }
        mutableState.update { state ->
            val selectedFamilies = script?.testFamilies.orEmpty()
            val defaultStatuses = selectedFamilies.associateWith { family ->
                state.qosFamilyStatusByType[family] ?: QosFamilyExecutionStatus.NOT_RUN
            }
            val defaultReasonCodes = selectedFamilies.associateWith { family ->
                state.qosFamilyReasonCodeByType[family]
            }
            val defaultReasons = selectedFamilies.associateWith { family ->
                state.qosFamilyFailureReasonByType[family].orEmpty()
            }
            deriveQosUiState(
                state.copy(
                qosSelectedScriptId = scriptId,
                qosSelectedScriptName = scriptName,
                qosSelectedTestFamilies = selectedFamilies,
                qosConfiguredRepeatInput = script?.repeatCount?.toString().orEmpty(),
                qosConfiguredTechnologies = script?.targetTechnologies.orEmpty(),
                qosScriptSnapshotUpdatedAtEpochMillis = script?.updatedAtEpochMillis,
                qosScriptEditorNameInput = script?.name.orEmpty(),
                qosScriptEditorRepeatInput = script?.repeatCount?.toString() ?: state.qosScriptEditorRepeatInput,
                qosScriptEditorTechnologiesInput = script?.targetTechnologies?.joinToString(", ").orEmpty(),
                qosScriptEditorSelectedFamilies = script?.testFamilies.orEmpty(),
                qosFamilyStatusByType = defaultStatuses,
                qosFamilyReasonCodeByType = defaultReasonCodes,
                qosFamilyFailureReasonByType = defaultReasons,
                qosTargetTechnologyInput = script?.targetTechnologies?.firstOrNull().orEmpty(),
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                errorMessage = null,
                infoMessage = null
                )
            )
        }
    }

    fun onQosConfiguredRepeatChanged(value: String) = updateField {
        copy(qosConfiguredRepeatInput = sanitizeInteger(value))
    }

    fun onQosScriptEditorNameChanged(value: String) = updateField {
        copy(qosScriptEditorNameInput = value)
    }

    fun onQosScriptEditorRepeatChanged(value: String) = updateField {
        copy(qosScriptEditorRepeatInput = sanitizeInteger(value))
    }

    fun onQosScriptEditorTechnologiesChanged(value: String) = updateField {
        copy(qosScriptEditorTechnologiesInput = value)
    }

    fun onQosScriptEditorFamilyToggled(family: QosTestFamily) {
        updateField {
            copy(
                qosScriptEditorSelectedFamilies = qosScriptEditorSelectedFamilies.toggle(family)
            )
        }
    }

    fun onSaveQosScriptClicked() {
        val current = mutableState.value
        if (current.isSavingQosScript) return

        val repeat = parseOptionalInt(
            value = current.qosScriptEditorRepeatInput,
            errorRes = R.string.error_performance_invalid_integer
        ) ?: return

        viewModelScope.launch {
            mutableState.update { state ->
                state.copy(isSavingQosScript = true, errorMessage = null, infoMessage = null)
            }
            runCatching {
                upsertQosScriptUseCase(
                    id = current.qosSelectedScriptId,
                    name = current.qosScriptEditorNameInput,
                    repeatCount = repeat.coerceAtLeast(1),
                    targetTechnologies = current.qosScriptEditorTechnologiesInput
                        .split(',')
                        .map { value -> value.trim() }
                        .filter { value -> value.isNotBlank() }
                        .toSet(),
                    testFamilies = current.qosScriptEditorSelectedFamilies
                )
            }.onFailure { throwable ->
                mutableState.update { state ->
                    state.copy(
                        isSavingQosScript = false,
                        errorMessage = throwable.message ?: uiStrings.get(R.string.error_performance_save_qos_script),
                        infoMessage = null
                    )
                }
            }.onSuccess { script ->
                mutableState.update { state ->
                    val nextStatuses = script.testFamilies.associateWith { family ->
                        state.qosFamilyStatusByType[family] ?: QosFamilyExecutionStatus.NOT_RUN
                    }
                    val nextReasonCodes = script.testFamilies.associateWith { family ->
                        state.qosFamilyReasonCodeByType[family]
                    }
                    val nextReasons = script.testFamilies.associateWith { family ->
                        state.qosFamilyFailureReasonByType[family].orEmpty()
                    }
                    deriveQosUiState(
                        state.copy(
                        isSavingQosScript = false,
                        qosSelectedScriptId = script.id,
                        qosSelectedScriptName = script.name,
                        qosSelectedTestFamilies = script.testFamilies,
                        qosConfiguredRepeatInput = script.repeatCount.toString(),
                        qosConfiguredTechnologies = script.targetTechnologies,
                        qosScriptSnapshotUpdatedAtEpochMillis = script.updatedAtEpochMillis,
                        qosFamilyStatusByType = nextStatuses,
                        qosFamilyReasonCodeByType = nextReasonCodes,
                        qosFamilyFailureReasonByType = nextReasons,
                        qosTargetTechnologyInput = script.targetTechnologies.firstOrNull().orEmpty(),
                        infoMessage = uiStrings.get(R.string.info_performance_saved_qos_script),
                        errorMessage = null
                        )
                    )
                }
            }
        }
    }

    fun onQosTargetTechnologyChanged(value: String) = updateField { copy(qosTargetTechnologyInput = value) }
    fun onQosTargetPhoneChanged(value: String) = updateField { copy(qosTargetPhoneInput = value.filter { it.isDigit() || it == '+' }) }
    fun onQosIterationCountChanged(value: String) = updateField { copy(qosIterationCountInput = sanitizeInteger(value)) }
    fun onQosSuccessCountChanged(value: String) = updateField { copy(qosSuccessCountInput = sanitizeInteger(value)) }
    fun onQosFailureCountChanged(value: String) = updateField { copy(qosFailureCountInput = sanitizeInteger(value)) }
    fun onQosRunnerStartClicked(family: QosTestFamily) {
        applyQosRunnerAction(
            family = family,
            action = QosRunnerAction.START,
            terminalEventType = QosExecutionEventType.STARTED
        )
    }

    fun onQosRunnerPassClicked(family: QosTestFamily) {
        applyQosRunnerAction(
            family = family,
            action = QosRunnerAction.MARK_PASSED,
            terminalEventType = QosExecutionEventType.PASSED
        )
    }

    fun onQosRunnerFailClicked(family: QosTestFamily) {
        applyQosRunnerAction(
            family = family,
            action = QosRunnerAction.MARK_FAILED,
            terminalEventType = QosExecutionEventType.FAILED
        )
    }

    fun onQosRunnerBlockClicked(family: QosTestFamily) {
        applyQosRunnerAction(
            family = family,
            action = QosRunnerAction.MARK_BLOCKED,
            terminalEventType = QosExecutionEventType.BLOCKED
        )
    }

    fun onQosRunnerPauseClicked(family: QosTestFamily) {
        applyQosRunnerAction(
            family = family,
            action = QosRunnerAction.PAUSE,
            terminalEventType = QosExecutionEventType.PAUSED
        )
    }

    fun onQosRunnerResumeClicked(family: QosTestFamily) {
        applyQosRunnerAction(
            family = family,
            action = QosRunnerAction.RESUME,
            terminalEventType = QosExecutionEventType.RESUMED
        )
    }

    fun onQosFamilyFailureReasonChanged(family: QosTestFamily, value: String) {
        updateField {
            copy(
                qosFamilyFailureReasonByType = qosFamilyFailureReasonByType + (family to value)
            )
        }
    }

    fun onQosFamilyReasonCodeChanged(family: QosTestFamily, value: QosExecutionIssueCode?) {
        updateField {
            copy(
                qosFamilyReasonCodeByType = qosFamilyReasonCodeByType + (family to value)
            )
        }
    }

    private fun applyQosRunnerAction(
        family: QosTestFamily,
        action: QosRunnerAction,
        terminalEventType: QosExecutionEventType
    ) {
        val state = mutableState.value
        val session = state.session ?: return
        val summary = qosSummaryForAssessment(state)
        val preflightIssues = assessQosFamilyPreflight(
            qosRunSummary = summary,
            family = family,
            action = action,
            prerequisiteNetworkReady = state.prerequisiteNetworkReady,
            prerequisiteBatterySufficient = state.prerequisiteBatterySufficient,
            prerequisiteLocationReady = state.prerequisiteLocationReady,
            reasonCode = state.qosFamilyReasonCodeByType[family],
            failureReason = state.qosFamilyFailureReasonByType[family]
        )
        if (preflightIssues.isNotEmpty()) {
            mutableState.update { current ->
                val suggestedReasonCode = if (
                    (action == QosRunnerAction.MARK_FAILED || action == QosRunnerAction.MARK_BLOCKED) &&
                    current.qosFamilyReasonCodeByType[family] == null
                ) {
                    qosPreflightIssueToReasonCode(preflightIssues.first())
                } else {
                    null
                }
                deriveQosUiState(
                    current.copy(
                        qosPreflightIssuesByFamily = current.qosPreflightIssuesByFamily + (family to preflightIssues),
                        qosFamilyReasonCodeByType = if (suggestedReasonCode != null) {
                            current.qosFamilyReasonCodeByType + (family to suggestedReasonCode)
                        } else {
                            current.qosFamilyReasonCodeByType
                        },
                        errorMessage = uiStrings.get(qosPreflightIssueToErrorRes(preflightIssues.first())),
                        infoMessage = null
                    )
                )
            }
            return
        }

        val runPlan = computeQosRunPlan(summary)
        val nextCheckpointSequence = (
            state.qosExecutionTimelineEvents.maxOfOrNull { event -> event.checkpointSequence }
                ?: state.qosExecutionTimelineEvents.size
            ) + 1
        val nextTimeline = when (action) {
            QosRunnerAction.START -> {
                val nextRepetitionIndex = computeQosFamilyRunCoverage(summary, family).nextRepetitionIndex
                state.qosExecutionTimelineEvents + QosExecutionTimelineEvent(
                    family = family,
                    repetitionIndex = nextRepetitionIndex,
                    eventType = QosExecutionEventType.STARTED,
                    occurredAtEpochMillis = System.currentTimeMillis(),
                    checkpointSequence = nextCheckpointSequence
                )
            }

            QosRunnerAction.PAUSE -> {
                val activeRepetitionIndex = runPlan.firstOrNull { item ->
                    item.family == family && item.status == QosRunPlanItemStatus.RUNNING
                }?.repetitionIndex ?: return
                state.qosExecutionTimelineEvents + QosExecutionTimelineEvent(
                    family = family,
                    repetitionIndex = activeRepetitionIndex,
                    eventType = QosExecutionEventType.PAUSED,
                    occurredAtEpochMillis = System.currentTimeMillis(),
                    checkpointSequence = nextCheckpointSequence
                )
            }

            QosRunnerAction.RESUME -> {
                val pausedRepetitionIndex = runPlan.firstOrNull { item ->
                    item.family == family && item.status == QosRunPlanItemStatus.PAUSED
                }?.repetitionIndex ?: return
                state.qosExecutionTimelineEvents + QosExecutionTimelineEvent(
                    family = family,
                    repetitionIndex = pausedRepetitionIndex,
                    eventType = QosExecutionEventType.RESUMED,
                    occurredAtEpochMillis = System.currentTimeMillis(),
                    checkpointSequence = nextCheckpointSequence
                )
            }

            QosRunnerAction.MARK_PASSED,
            QosRunnerAction.MARK_FAILED,
            QosRunnerAction.MARK_BLOCKED -> {
                val activeRepetitionIndex = computeQosFamilyRunCoverage(summary, family).activeRepetitionIndex
                    ?: return
                state.qosExecutionTimelineEvents + QosExecutionTimelineEvent(
                    family = family,
                    repetitionIndex = activeRepetitionIndex,
                    eventType = terminalEventType,
                    reasonCode = state.qosFamilyReasonCodeByType[family],
                    reason = state.qosFamilyFailureReasonByType[family]
                        ?.trim()
                        ?.takeIf { it.isNotBlank() },
                    occurredAtEpochMillis = System.currentTimeMillis(),
                    checkpointSequence = nextCheckpointSequence
                )
            }
        }

        var persistedState: PerformanceSessionUiState? = null
        mutableState.update { current ->
            val nextStatus = when (terminalEventType) {
                QosExecutionEventType.STARTED -> current.qosFamilyStatusByType[family]
                    ?: QosFamilyExecutionStatus.NOT_RUN
                QosExecutionEventType.PAUSED,
                QosExecutionEventType.RESUMED -> current.qosFamilyStatusByType[family]
                    ?: QosFamilyExecutionStatus.NOT_RUN
                QosExecutionEventType.PASSED -> QosFamilyExecutionStatus.PASSED
                QosExecutionEventType.FAILED -> QosFamilyExecutionStatus.FAILED
                QosExecutionEventType.BLOCKED -> QosFamilyExecutionStatus.BLOCKED
            }
            val nextReasons = when (terminalEventType) {
                QosExecutionEventType.PASSED,
                QosExecutionEventType.STARTED -> current.qosFamilyFailureReasonByType - family
                QosExecutionEventType.PAUSED,
                QosExecutionEventType.RESUMED -> current.qosFamilyFailureReasonByType
                QosExecutionEventType.FAILED,
                QosExecutionEventType.BLOCKED -> current.qosFamilyFailureReasonByType
            }
            val nextReasonCodes = when (terminalEventType) {
                QosExecutionEventType.PASSED,
                QosExecutionEventType.STARTED -> current.qosFamilyReasonCodeByType - family
                QosExecutionEventType.PAUSED,
                QosExecutionEventType.RESUMED -> current.qosFamilyReasonCodeByType
                QosExecutionEventType.FAILED,
                QosExecutionEventType.BLOCKED -> current.qosFamilyReasonCodeByType
            }
            val nextUiState = deriveQosUiState(
                current.copy(
                    selectedStatus = if (current.selectedStatus == PerformanceSessionStatus.COMPLETED) {
                        PerformanceSessionStatus.IN_PROGRESS
                    } else {
                        current.selectedStatus
                    },
                    qosExecutionTimelineEvents = nextTimeline
                        .sortedWith(
                            compareByDescending<QosExecutionTimelineEvent> { event ->
                                event.checkpointSequence
                            }.thenBy { event ->
                                event.family.name
                            }.thenBy { event ->
                                event.repetitionIndex
                            }.thenBy { event ->
                                event.occurredAtEpochMillis
                            }.thenBy { event ->
                                qosExecutionEventSortOrder(event.eventType)
                            }
                    ),
                    qosFamilyStatusByType = current.qosFamilyStatusByType + (family to nextStatus),
                    qosFamilyReasonCodeByType = nextReasonCodes,
                    qosFamilyFailureReasonByType = nextReasons,
                    qosPreflightIssuesByFamily = current.qosPreflightIssuesByFamily + (family to emptySet()),
                    hasUnsavedChanges = true
                )
            )
            persistedState = nextUiState
            nextUiState
        }

        persistedState?.let { latest ->
            persistQosRunnerProgress(
                sessionId = session.id,
                state = latest
            )
        }
    }
    fun onNotesChanged(value: String) = updateField { copy(notesInput = value) }
    fun onResultSummaryChanged(value: String) = updateField { copy(resultSummaryInput = value) }

    fun onSaveSummaryClicked() {
        val current = mutableState.value
        val session = current.session ?: return
        if (current.isSavingSummary || !current.hasUnsavedChanges) return

        val throughputMetrics = parseThroughputMetrics(current) ?: return
        val qosSummary = parseQosRunSummary(current) ?: return

        if (current.selectedStatus == PerformanceSessionStatus.COMPLETED) {
            if (!(current.prerequisiteNetworkReady &&
                    current.prerequisiteBatterySufficient &&
                    current.prerequisiteLocationReady)
            ) {
                mutableState.update { state ->
                    state.copy(errorMessage = uiStrings.get(R.string.error_performance_complete_requires_prerequisites))
                }
                return
            }
            when (session.workflowType) {
                PerformanceWorkflowType.THROUGHPUT -> {
                    if (!throughputMetrics.hasAnyMeasurement) {
                        mutableState.update { state ->
                            state.copy(errorMessage = uiStrings.get(R.string.error_performance_throughput_result_required))
                        }
                        return
                    }
                }

                PerformanceWorkflowType.QOS_SCRIPT -> {
                    val qosAssessment = assessQosCompletion(qosSummary)
                    if (!qosAssessment.canComplete) {
                        mutableState.update { state ->
                            state.copy(
                                errorMessage = uiStrings.get(
                                    qosCompletionIssueToErrorRes(qosAssessment.issues.first())
                                )
                            )
                        }
                        return
                    }
                }
            }
        }

        viewModelScope.launch {
            mutableState.update { state ->
                state.copy(isSavingSummary = true, errorMessage = null, infoMessage = null)
            }

            runCatching {
                updatePerformanceSessionExecutionUseCase(
                    sessionId = session.id,
                    status = current.selectedStatus,
                    prerequisiteNetworkReady = current.prerequisiteNetworkReady,
                    prerequisiteBatterySufficient = current.prerequisiteBatterySufficient,
                    prerequisiteLocationReady = current.prerequisiteLocationReady,
                    observedNetworkStatus = current.observedNetworkStatus,
                    observedBatteryLevelPercent = current.observedBatteryLevelPercent,
                    observedLocationAvailable = current.observedLocationAvailable,
                    observedSignalsCapturedAtEpochMillis = current.observedSignalsCapturedAtEpochMillis,
                    throughputMetrics = throughputMetrics,
                    qosRunSummary = qosSummary,
                    notes = current.notesInput.trim(),
                    resultSummary = current.resultSummaryInput.trim()
                )
            }.onFailure { throwable ->
                mutableState.update { state ->
                    state.copy(
                        isSavingSummary = false,
                        errorMessage = throwable.message ?: uiStrings.get(R.string.error_performance_save_summary)
                    )
                }
            }.onSuccess {
                mutableState.update { state ->
                    state.copy(
                        isSavingSummary = false,
                        hasUnsavedChanges = false,
                        completionGuardMessage = null,
                        infoMessage = uiStrings.get(R.string.info_performance_summary_saved)
                    )
                }
            }
        }
    }

    private fun createSession(workflowType: PerformanceWorkflowType) {
        val current = mutableState.value
        if (current.isCreatingSession || current.siteCode.isBlank()) return

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
                createSitePerformanceSessionUseCase(
                    siteId = siteId,
                    siteCode = mutableState.value.siteCode,
                    workflowType = workflowType,
                    operatorName = mutableState.value.selectedOperator,
                    technology = mutableState.value.selectedTechnology
                )
            }.onFailure { throwable ->
                mutableState.update { state ->
                    state.copy(
                        isCreatingSession = false,
                        errorMessage = throwable.message ?: uiStrings.get(R.string.error_performance_create_session)
                    )
                }
            }.onSuccess { created ->
                mutableState.update { state ->
                    state.copy(
                        isCreatingSession = false,
                        selectedSessionId = created.id,
                        hasUnsavedChanges = false,
                        infoMessage = uiStrings.get(R.string.info_performance_session_created)
                    )
                }
            }
        }
    }

    private fun observeDeviceNetworkStatus() {
        viewModelScope.launch {
            observeNetworkStatusUseCase()
                .catch { throwable ->
                    mutableState.update { state ->
                        state.copy(
                            errorMessage = throwable.message ?: uiStrings.get(R.string.error_performance_device_diagnostics_unavailable)
                        )
                    }
                }
                .collect { status ->
                    mutableState.update { state ->
                        deriveQosUiState(
                            state.copy(
                                observedNetworkStatus = status,
                                observedSignalsCapturedAtEpochMillis = System.currentTimeMillis()
                            )
                        )
                    }
                }
        }
    }

    private fun observeContext() {
        viewModelScope.launch {
            combine(
                observeSiteDetailUseCase(siteId),
                observeSitePerformanceSessionHistoryUseCase(siteId),
                observeQosScriptsUseCase()
            ) { site, history, qosScripts -> Triple(site, history, qosScripts) }
                .catch { throwable ->
                    mutableState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: uiStrings.get(R.string.error_performance_observe_session)
                        )
                    }
                }
                .collect { (site, history, qosScripts) ->
                    val operators = site?.sectors
                        .orEmpty()
                        .flatMap { sector -> sector.cells }
                        .map { cell -> cell.operatorName }
                        .distinct()
                        .sorted()
                    val technologies = site?.sectors
                        .orEmpty()
                        .flatMap { sector -> sector.cells }
                        .map { cell -> cell.technology }
                        .distinct()
                        .sorted()
                    val current = mutableState.value
                    val selectedSessionId = when {
                        current.selectedSessionId != null &&
                            history.any { session -> session.id == current.selectedSessionId } -> {
                            current.selectedSessionId
                        }
                        history.isNotEmpty() -> history.first().id
                        else -> null
                    }
                    val selectedSession = history.firstOrNull { session -> session.id == selectedSessionId }
                    val shouldHydrateInputs =
                        !current.hasUnsavedChanges || current.session?.id != selectedSession?.id
                    val firstScript = qosScripts.firstOrNull()

                    val baseState = current.copy(
                        isLoading = false,
                        siteCode = site?.externalCode.orEmpty(),
                        siteLabel = listOf(site?.name.orEmpty(), site?.externalCode.orEmpty())
                            .filter { it.isNotBlank() }
                            .joinToString(" - "),
                        availableOperators = operators,
                        availableTechnologies = technologies,
                        selectedOperator = current.selectedOperator ?: operators.firstOrNull(),
                        selectedTechnology = current.selectedTechnology ?: technologies.firstOrNull(),
                        selectedSessionId = selectedSessionId,
                        session = selectedSession,
                        sessionHistory = history,
                        availableQosScripts = qosScripts,
                        qosScriptEditorNameInput = if (current.qosScriptEditorNameInput.isBlank()) {
                            firstScript?.name.orEmpty()
                        } else {
                            current.qosScriptEditorNameInput
                        },
                        qosScriptEditorRepeatInput = if (
                            current.qosScriptEditorRepeatInput.isBlank() ||
                            current.qosScriptEditorRepeatInput == "0"
                        ) {
                            firstScript?.repeatCount?.toString() ?: "1"
                        } else {
                            current.qosScriptEditorRepeatInput
                        },
                        qosScriptEditorTechnologiesInput = if (current.qosScriptEditorTechnologiesInput.isBlank()) {
                            firstScript?.targetTechnologies?.joinToString(", ").orEmpty()
                        } else {
                            current.qosScriptEditorTechnologiesInput
                        },
                        qosScriptEditorSelectedFamilies = if (current.qosScriptEditorSelectedFamilies.isEmpty()) {
                            firstScript?.testFamilies.orEmpty()
                        } else {
                            current.qosScriptEditorSelectedFamilies
                        }
                    )

                    mutableState.value = if (shouldHydrateInputs) {
                        deriveQosUiState(hydrateInputsFromSession(baseState, selectedSession))
                    } else {
                        deriveQosUiState(baseState)
                    }
                }
        }
    }

    private fun hydrateInputsFromSession(
        state: PerformanceSessionUiState,
        session: PerformanceSession?
    ): PerformanceSessionUiState {
        if (session == null) {
            return state.copy(
                selectedStatus = PerformanceSessionStatus.CREATED,
                prerequisiteNetworkReady = false,
                prerequisiteBatterySufficient = false,
                prerequisiteLocationReady = false,
                observedNetworkStatus = state.observedNetworkStatus,
                observedBatteryLevelPercent = state.observedBatteryLevelPercent,
                observedBatteryIsCharging = null,
                observedLocationAvailable = state.observedLocationAvailable,
                observedSignalsCapturedAtEpochMillis = state.observedSignalsCapturedAtEpochMillis,
                throughputDownloadInput = "",
                throughputUploadInput = "",
                throughputLatencyInput = "",
                throughputMinDownloadInput = "",
                throughputMinUploadInput = "",
                throughputMaxLatencyInput = "",
                qosSelectedScriptId = null,
                qosSelectedScriptName = null,
                qosSelectedTestFamilies = emptySet(),
                qosConfiguredRepeatInput = "",
                qosConfiguredTechnologies = emptySet(),
                qosScriptSnapshotUpdatedAtEpochMillis = null,
                qosScriptEditorNameInput = "",
                qosScriptEditorRepeatInput = "1",
                qosScriptEditorTechnologiesInput = "",
                qosScriptEditorSelectedFamilies = emptySet(),
                qosFamilyStatusByType = emptyMap(),
                qosFamilyReasonCodeByType = emptyMap(),
                qosFamilyFailureReasonByType = emptyMap(),
                qosFamilyRunCoverageByType = emptyMap(),
                qosRunPlan = emptyList(),
                qosExecutionSnapshot = null,
                qosPreflightIssuesByFamily = emptyMap(),
                qosExecutionTimelineEvents = emptyList(),
                qosCompletionIssues = emptySet(),
                qosTargetTechnologyInput = "",
                qosTargetPhoneInput = "",
                qosIterationCountInput = "",
                qosSuccessCountInput = "",
                qosFailureCountInput = "",
                notesInput = "",
                resultSummaryInput = "",
                hasUnsavedChanges = false,
                completionGuardMessage = null
            )
        }

        return state.copy(
            selectedStatus = session.status,
            selectedOperator = session.operatorName ?: state.selectedOperator,
            selectedTechnology = session.technology ?: state.selectedTechnology,
            prerequisiteNetworkReady = session.prerequisiteNetworkReady,
            prerequisiteBatterySufficient = session.prerequisiteBatterySufficient,
            prerequisiteLocationReady = session.prerequisiteLocationReady,
            observedNetworkStatus = session.observedNetworkStatus ?: state.observedNetworkStatus,
            observedBatteryLevelPercent = session.observedBatteryLevelPercent ?: state.observedBatteryLevelPercent,
            observedBatteryIsCharging = null,
            observedLocationAvailable = session.observedLocationAvailable ?: state.observedLocationAvailable,
            observedSignalsCapturedAtEpochMillis = session.observedSignalsCapturedAtEpochMillis
                ?: state.observedSignalsCapturedAtEpochMillis,
            throughputDownloadInput = session.throughputMetrics.downloadMbps?.toString().orEmpty(),
            throughputUploadInput = session.throughputMetrics.uploadMbps?.toString().orEmpty(),
            throughputLatencyInput = session.throughputMetrics.latencyMs?.toString().orEmpty(),
            throughputMinDownloadInput = session.throughputMetrics.minDownloadMbps?.toString().orEmpty(),
            throughputMinUploadInput = session.throughputMetrics.minUploadMbps?.toString().orEmpty(),
            throughputMaxLatencyInput = session.throughputMetrics.maxLatencyMs?.toString().orEmpty(),
            qosSelectedScriptId = session.qosRunSummary.scriptId,
            qosSelectedScriptName = session.qosRunSummary.scriptName,
            qosSelectedTestFamilies = session.qosRunSummary.selectedTestFamilies,
            qosConfiguredRepeatInput = session.qosRunSummary.configuredRepeatCount?.toString().orEmpty(),
            qosConfiguredTechnologies = session.qosRunSummary.configuredTechnologies,
            qosScriptSnapshotUpdatedAtEpochMillis = session.qosRunSummary.scriptSnapshotUpdatedAtEpochMillis,
            qosScriptEditorNameInput = session.qosRunSummary.scriptName.orEmpty(),
            qosScriptEditorRepeatInput = session.qosRunSummary.configuredRepeatCount?.toString() ?: "1",
            qosScriptEditorTechnologiesInput = session.qosRunSummary.configuredTechnologies
                .joinToString(", "),
            qosScriptEditorSelectedFamilies = session.qosRunSummary.selectedTestFamilies,
            qosFamilyStatusByType = session.qosRunSummary.familyExecutionResults
                .associate { result -> result.family to result.status },
            qosFamilyReasonCodeByType = session.qosRunSummary.familyExecutionResults
                .associate { result -> result.family to result.failureReasonCode },
            qosFamilyFailureReasonByType = session.qosRunSummary.familyExecutionResults
                .associate { result -> result.family to (result.failureReason.orEmpty()) },
            qosFamilyRunCoverageByType = emptyMap(),
            qosPreflightIssuesByFamily = emptyMap(),
            qosExecutionTimelineEvents = session.qosRunSummary.executionTimelineEvents,
            qosCompletionIssues = if (session.workflowType == PerformanceWorkflowType.QOS_SCRIPT) {
                assessQosCompletion(session.qosRunSummary).issues
            } else {
                emptySet()
            },
            qosTargetTechnologyInput = session.qosRunSummary.targetTechnology.orEmpty(),
            qosTargetPhoneInput = session.qosRunSummary.targetPhoneNumber.orEmpty(),
            qosIterationCountInput = session.qosRunSummary.iterationCount.toString(),
            qosSuccessCountInput = session.qosRunSummary.successCount.toString(),
            qosFailureCountInput = session.qosRunSummary.failureCount.toString(),
            notesInput = session.notes,
            resultSummaryInput = session.resultSummary,
            hasUnsavedChanges = false,
            completionGuardMessage = null
        )
    }

    private fun updateField(transform: PerformanceSessionUiState.() -> PerformanceSessionUiState) {
        mutableState.update { state ->
            deriveQosUiState(
                state.transform().copy(
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                errorMessage = null,
                infoMessage = null
                )
            )
        }
    }

    private fun persistQosRunnerProgress(
        sessionId: String,
        state: PerformanceSessionUiState
    ) {
        viewModelScope.launch {
            runCatching {
                val status = when (state.selectedStatus) {
                    PerformanceSessionStatus.CREATED,
                    PerformanceSessionStatus.COMPLETED -> PerformanceSessionStatus.IN_PROGRESS
                    else -> state.selectedStatus
                }
                updatePerformanceSessionExecutionUseCase(
                    sessionId = sessionId,
                    status = status,
                    prerequisiteNetworkReady = state.prerequisiteNetworkReady,
                    prerequisiteBatterySufficient = state.prerequisiteBatterySufficient,
                    prerequisiteLocationReady = state.prerequisiteLocationReady,
                    observedNetworkStatus = state.observedNetworkStatus,
                    observedBatteryLevelPercent = state.observedBatteryLevelPercent,
                    observedLocationAvailable = state.observedLocationAvailable,
                    observedSignalsCapturedAtEpochMillis = state.observedSignalsCapturedAtEpochMillis,
                    throughputMetrics = state.session?.throughputMetrics ?: ThroughputMetrics(),
                    qosRunSummary = qosSummaryForAssessment(state),
                    notes = state.notesInput.trim(),
                    resultSummary = state.resultSummaryInput.trim()
                )
            }.onFailure { throwable ->
                mutableState.update { current ->
                    current.copy(
                        errorMessage = throwable.message ?: uiStrings.get(R.string.error_performance_save_summary),
                        infoMessage = null
                    )
                }
            }
        }
    }

    private fun parseThroughputMetrics(state: PerformanceSessionUiState): ThroughputMetrics? {
        val download = parseOptionalDouble(
            value = state.throughputDownloadInput,
            errorRes = R.string.error_performance_invalid_number
        ) ?: return null
        val upload = parseOptionalDouble(
            value = state.throughputUploadInput,
            errorRes = R.string.error_performance_invalid_number
        ) ?: return null
        val latency = parseOptionalInt(
            value = state.throughputLatencyInput,
            errorRes = R.string.error_performance_invalid_integer
        ) ?: return null
        val minDownload = parseOptionalDouble(
            value = state.throughputMinDownloadInput,
            errorRes = R.string.error_performance_invalid_number
        ) ?: return null
        val minUpload = parseOptionalDouble(
            value = state.throughputMinUploadInput,
            errorRes = R.string.error_performance_invalid_number
        ) ?: return null
        val maxLatency = parseOptionalInt(
            value = state.throughputMaxLatencyInput,
            errorRes = R.string.error_performance_invalid_integer
        ) ?: return null

        return ThroughputMetrics(
            downloadMbps = download,
            uploadMbps = upload,
            latencyMs = latency,
            minDownloadMbps = minDownload,
            minUploadMbps = minUpload,
            maxLatencyMs = maxLatency
        )
    }

    private fun parseQosRunSummary(state: PerformanceSessionUiState): QosRunSummary? {
        val configuredRepeat = parseOptionalInt(
            value = state.qosConfiguredRepeatInput,
            errorRes = R.string.error_performance_invalid_integer
        )

        val snapshotFamilies = state.qosSelectedTestFamilies
        val familyResults = snapshotFamilies.map { family ->
            val derivedStatus = resolveFamilyStatusFromTimeline(
                timelineEvents = state.qosExecutionTimelineEvents,
                family = family
            )
            QosFamilyExecutionResult(
                family = family,
                status = derivedStatus ?: state.qosFamilyStatusByType[family] ?: QosFamilyExecutionStatus.NOT_RUN,
                failureReasonCode = state.qosFamilyReasonCodeByType[family],
                failureReason = state.qosFamilyFailureReasonByType[family]
                    ?.trim()
                    ?.takeIf { value -> value.isNotBlank() }
            )
        }.sortedBy { result -> result.family.name }
        val counters = deriveQosPassFailCounters(
            QosRunSummary(
                selectedTestFamilies = snapshotFamilies,
                familyExecutionResults = familyResults,
                executionTimelineEvents = state.qosExecutionTimelineEvents
            )
        )

        return QosRunSummary(
            scriptId = state.qosSelectedScriptId,
            scriptName = state.qosSelectedScriptName,
            configuredRepeatCount = (configuredRepeat ?: 1).coerceAtLeast(1),
            configuredTechnologies = state.qosConfiguredTechnologies,
            scriptSnapshotUpdatedAtEpochMillis = state.qosScriptSnapshotUpdatedAtEpochMillis,
            selectedTestFamilies = snapshotFamilies,
            familyExecutionResults = familyResults,
            executionTimelineEvents = state.qosExecutionTimelineEvents,
            targetTechnology = state.qosTargetTechnologyInput.trim().ifBlank { null },
            targetPhoneNumber = state.qosTargetPhoneInput.trim().ifBlank { null },
            iterationCount = counters.iterationCount,
            successCount = counters.successCount,
            failureCount = counters.failureCount
        )
    }

    private fun parseOptionalDouble(value: String, errorRes: Int): Double? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.toDoubleOrNull()?.also {
            if (it < 0.0) {
                mutableState.update { state ->
                    state.copy(errorMessage = uiStrings.get(errorRes))
                }
                return null
            }
        } ?: run {
            mutableState.update { state ->
                state.copy(errorMessage = uiStrings.get(errorRes))
            }
            null
        }
    }

    private fun parseOptionalInt(value: String, errorRes: Int): Int? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.toIntOrNull()?.takeIf { it >= 0 } ?: run {
            mutableState.update { state ->
                state.copy(errorMessage = uiStrings.get(errorRes))
            }
            null
        }
    }

    private fun sanitizeDecimal(value: String): String {
        return value.replace(',', '.').filterIndexed { index, c ->
            c.isDigit() || c == '.' || (c == '-' && index == 0)
        }
    }

    private fun sanitizeInteger(value: String): String {
        return value.filterIndexed { index, c ->
            c.isDigit() || (c == '-' && index == 0)
        }
    }
}

private fun deriveQosExecutionAggregate(state: PerformanceSessionUiState): PerformanceSessionUiState {
    val counters = deriveQosPassFailCounters(
        qosSummaryForAssessment(state)
    )
    return state.copy(
        qosIterationCountInput = counters.iterationCount.toString(),
        qosSuccessCountInput = counters.successCount.toString(),
        qosFailureCountInput = counters.failureCount.toString()
    )
}

private fun deriveQosUiState(state: PerformanceSessionUiState): PerformanceSessionUiState {
    val aggregated = deriveQosExecutionAggregate(state)
    val isQosWorkflow = aggregated.selectedSessionWorkflowType == PerformanceWorkflowType.QOS_SCRIPT
    if (!isQosWorkflow) {
        return aggregated.copy(
            qosCompletionIssues = emptySet(),
            qosFamilyRunCoverageByType = emptyMap(),
            qosRunPlan = emptyList(),
            qosExecutionSnapshot = null,
            qosPreflightIssuesByFamily = emptyMap()
        )
    }
    val summary = qosSummaryForAssessment(aggregated)
    val runPlan = computeQosRunPlan(summary)
    val executionSnapshot = deriveQosExecutionSnapshot(
        qosRunSummary = summary,
        preconditionsReady = aggregated.prerequisiteNetworkReady &&
            aggregated.prerequisiteBatterySufficient &&
            aggregated.prerequisiteLocationReady
    )
    val coverageByFamily = aggregated.qosSelectedTestFamilies.associateWith { family ->
        computeQosFamilyRunCoverage(summary, family)
    }
    val preflightByFamily = coverageByFamily.mapValues { (family, _) ->
        assessQosFamilyPreflight(
            qosRunSummary = summary,
            family = family,
            action = QosRunnerAction.START,
            prerequisiteNetworkReady = aggregated.prerequisiteNetworkReady,
            prerequisiteBatterySufficient = aggregated.prerequisiteBatterySufficient,
            prerequisiteLocationReady = aggregated.prerequisiteLocationReady,
            reasonCode = aggregated.qosFamilyReasonCodeByType[family],
            failureReason = aggregated.qosFamilyFailureReasonByType[family]
        )
    }
    return aggregated.copy(
        qosCompletionIssues = assessQosCompletion(summary).issues,
        qosFamilyRunCoverageByType = coverageByFamily,
        qosRunPlan = runPlan,
        qosExecutionSnapshot = executionSnapshot,
        qosPreflightIssuesByFamily = preflightByFamily
    )
}

private fun qosSummaryForAssessment(state: PerformanceSessionUiState): QosRunSummary {
    val families = state.qosSelectedTestFamilies
    val familyResults = families.map { family ->
        val derivedStatus = resolveFamilyStatusFromTimeline(
            timelineEvents = state.qosExecutionTimelineEvents,
            family = family
        )
        QosFamilyExecutionResult(
            family = family,
            status = derivedStatus ?: state.qosFamilyStatusByType[family] ?: QosFamilyExecutionStatus.NOT_RUN,
            failureReasonCode = state.qosFamilyReasonCodeByType[family],
            failureReason = state.qosFamilyFailureReasonByType[family]
                ?.trim()
                ?.takeIf { value -> value.isNotBlank() }
        )
    }.sortedBy { result -> result.family.name }
    val counters = deriveQosPassFailCounters(
        QosRunSummary(
            selectedTestFamilies = families,
            familyExecutionResults = familyResults,
            executionTimelineEvents = state.qosExecutionTimelineEvents
        )
    )
    return QosRunSummary(
        scriptId = state.qosSelectedScriptId,
        scriptName = state.qosSelectedScriptName,
        configuredRepeatCount = state.qosConfiguredRepeatInput.toIntOrNull()?.coerceAtLeast(1) ?: 1,
        configuredTechnologies = state.qosConfiguredTechnologies,
        scriptSnapshotUpdatedAtEpochMillis = state.qosScriptSnapshotUpdatedAtEpochMillis,
        selectedTestFamilies = families,
        familyExecutionResults = familyResults,
        executionTimelineEvents = state.qosExecutionTimelineEvents,
        targetTechnology = state.qosTargetTechnologyInput.trim().ifBlank { null },
        targetPhoneNumber = state.qosTargetPhoneInput.trim().ifBlank { null },
        iterationCount = counters.iterationCount,
        successCount = counters.successCount,
        failureCount = counters.failureCount
    )
}

private fun qosCompletionIssueToErrorRes(issue: QosCompletionIssue): Int {
    return when (issue) {
        QosCompletionIssue.SCRIPT_REFERENCE_MISSING,
        QosCompletionIssue.TEST_FAMILIES_MISSING,
        QosCompletionIssue.FAMILY_RESULT_INCOMPLETE -> R.string.error_performance_qos_script_required
        QosCompletionIssue.REPETITION_COVERAGE_INCOMPLETE -> R.string.error_performance_qos_repetition_coverage_required
        QosCompletionIssue.FAILURE_REASON_CODE_MISSING -> R.string.error_performance_qos_failed_reason_required
        QosCompletionIssue.PHONE_TARGET_MISSING -> R.string.error_performance_qos_phone_required
        QosCompletionIssue.TARGET_TECHNOLOGY_INVALID -> R.string.error_performance_qos_target_technology_required
        QosCompletionIssue.COUNTERS_INCONSISTENT -> R.string.error_performance_qos_result_inconsistent
    }
}

private fun qosPreflightIssueToErrorRes(issue: QosPreflightIssue): Int {
    return when (issue) {
        QosPreflightIssue.NETWORK_NOT_READY -> R.string.error_performance_qos_issue_network_not_ready
        QosPreflightIssue.BATTERY_NOT_READY -> R.string.error_performance_qos_issue_battery_not_ready
        QosPreflightIssue.LOCATION_NOT_READY -> R.string.error_performance_qos_issue_location_not_ready
        QosPreflightIssue.SCRIPT_REFERENCE_MISSING -> R.string.error_performance_qos_script_required
        QosPreflightIssue.FAMILY_NOT_SELECTED -> R.string.error_performance_qos_script_required
        QosPreflightIssue.PHONE_TARGET_MISSING -> R.string.error_performance_qos_phone_required
        QosPreflightIssue.TARGET_TECHNOLOGY_INVALID -> R.string.error_performance_qos_target_technology_required
        QosPreflightIssue.REPETITION_ALREADY_STARTED -> R.string.error_performance_qos_repetition_already_started
        QosPreflightIssue.REPETITION_ALREADY_COMPLETED -> R.string.error_performance_qos_repetition_coverage_required
        QosPreflightIssue.ANOTHER_REPETITION_ACTIVE -> R.string.error_performance_qos_another_repetition_active
        QosPreflightIssue.REPETITION_NOT_STARTED -> R.string.error_performance_qos_repetition_not_started
        QosPreflightIssue.REPETITION_NOT_PAUSED -> R.string.error_performance_qos_repetition_not_paused
        QosPreflightIssue.FAILURE_REASON_CODE_REQUIRED -> R.string.error_performance_qos_failed_reason_required
    }
}

private fun qosPreflightIssueToReasonCode(issue: QosPreflightIssue): QosExecutionIssueCode? {
    return when (issue) {
        QosPreflightIssue.NETWORK_NOT_READY -> QosExecutionIssueCode.NETWORK_UNAVAILABLE
        QosPreflightIssue.BATTERY_NOT_READY -> QosExecutionIssueCode.BATTERY_INSUFFICIENT
        QosPreflightIssue.LOCATION_NOT_READY -> QosExecutionIssueCode.LOCATION_UNAVAILABLE
        QosPreflightIssue.PHONE_TARGET_MISSING -> QosExecutionIssueCode.PHONE_TARGET_MISSING
        QosPreflightIssue.TARGET_TECHNOLOGY_INVALID -> QosExecutionIssueCode.TARGET_TECHNOLOGY_MISMATCH
        QosPreflightIssue.REPETITION_ALREADY_STARTED,
        QosPreflightIssue.REPETITION_ALREADY_COMPLETED,
        QosPreflightIssue.ANOTHER_REPETITION_ACTIVE,
        QosPreflightIssue.REPETITION_NOT_STARTED,
        QosPreflightIssue.REPETITION_NOT_PAUSED -> QosExecutionIssueCode.OPERATOR_ABORTED
        QosPreflightIssue.SCRIPT_REFERENCE_MISSING,
        QosPreflightIssue.FAMILY_NOT_SELECTED,
        QosPreflightIssue.FAILURE_REASON_CODE_REQUIRED -> null
    }
}

private fun resolveFamilyStatusFromTimeline(
    timelineEvents: List<QosExecutionTimelineEvent>,
    family: QosTestFamily
): QosFamilyExecutionStatus? {
    val latestTerminal = timelineEvents
        .asSequence()
        .filter { event -> event.family == family }
        .filter { event ->
            event.eventType == QosExecutionEventType.PASSED ||
                event.eventType == QosExecutionEventType.FAILED ||
                event.eventType == QosExecutionEventType.BLOCKED
        }
        .maxWithOrNull(
            compareBy<QosExecutionTimelineEvent> { event ->
                if (event.checkpointSequence > 0) {
                    event.checkpointSequence
                } else {
                    Int.MAX_VALUE
                }
            }.thenBy { event -> event.occurredAtEpochMillis }
                .thenBy { event -> event.repetitionIndex }
                .thenBy { event -> qosExecutionEventSortOrder(event.eventType) }
        )
        ?: return null

    return when (latestTerminal.eventType) {
        QosExecutionEventType.PASSED -> QosFamilyExecutionStatus.PASSED
        QosExecutionEventType.FAILED -> QosFamilyExecutionStatus.FAILED
        QosExecutionEventType.BLOCKED -> QosFamilyExecutionStatus.BLOCKED
        QosExecutionEventType.STARTED,
        QosExecutionEventType.PAUSED,
        QosExecutionEventType.RESUMED -> null
    }
}

private fun <T> Set<T>.toggle(item: T): Set<T> {
    return if (contains(item)) this - item else this + item
}

sealed interface PerformanceSessionEvent {
    data class OpenDraft(val draftId: String) : PerformanceSessionEvent
}
