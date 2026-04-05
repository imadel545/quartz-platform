package com.quartz.platform.presentation.performance.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quartz.platform.R
import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.model.QosFamilyExecutionStatus
import com.quartz.platform.presentation.components.MissionPrimaryActionBar
import com.quartz.platform.presentation.components.MissionPrimaryActionButton
import com.quartz.platform.presentation.components.MissionHeaderCard
import com.quartz.platform.presentation.components.OperationalMessageCard
import com.quartz.platform.presentation.components.OperationalMetric
import com.quartz.platform.presentation.components.OperationalSeverity
import com.quartz.platform.presentation.components.OperationalSignal
import com.quartz.platform.presentation.components.OperationalStateBanner
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PerformanceSessionRoute(
    onBack: () -> Unit,
    onOpenDraft: (String) -> Unit,
    viewModel: PerformanceSessionViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PerformanceSessionEvent.OpenDraft -> onOpenDraft(event.draftId)
            }
        }
    }
    PerformanceSessionScreen(
        state = state,
        onBack = onBack,
        onCreateThroughputSessionClicked = viewModel::onCreateThroughputSessionClicked,
        onCreateQosSessionClicked = viewModel::onCreateQosSessionClicked,
        onSelectHistorySessionClicked = viewModel::onSelectHistorySessionClicked,
        onOperatorSelected = viewModel::onOperatorSelected,
        onTechnologySelected = viewModel::onTechnologySelected,
        onStepStatusSelected = viewModel::onStepStatusSelected,
        onSessionStatusSelected = viewModel::onSessionStatusSelected,
        onRefreshDeviceDiagnosticsClicked = viewModel::onRefreshDeviceDiagnosticsClicked,
        onApplyDeviceDiagnosticsClicked = viewModel::onApplyDeviceDiagnosticsClicked,
        onPrerequisiteNetworkChanged = viewModel::onPrerequisiteNetworkChanged,
        onPrerequisiteBatteryChanged = viewModel::onPrerequisiteBatteryChanged,
        onPrerequisiteLocationChanged = viewModel::onPrerequisiteLocationChanged,
        onThroughputDownloadChanged = viewModel::onThroughputDownloadChanged,
        onThroughputUploadChanged = viewModel::onThroughputUploadChanged,
        onThroughputLatencyChanged = viewModel::onThroughputLatencyChanged,
        onThroughputMinDownloadChanged = viewModel::onThroughputMinDownloadChanged,
        onThroughputMinUploadChanged = viewModel::onThroughputMinUploadChanged,
        onThroughputMaxLatencyChanged = viewModel::onThroughputMaxLatencyChanged,
        onQosScriptSelected = viewModel::onQosScriptSelected,
        onQosConfiguredRepeatChanged = viewModel::onQosConfiguredRepeatChanged,
        onQosScriptEditorNameChanged = viewModel::onQosScriptEditorNameChanged,
        onQosScriptEditorRepeatChanged = viewModel::onQosScriptEditorRepeatChanged,
        onQosScriptEditorTechnologiesChanged = viewModel::onQosScriptEditorTechnologiesChanged,
        onQosScriptEditorFamilyToggled = viewModel::onQosScriptEditorFamilyToggled,
        onSaveQosScriptClicked = viewModel::onSaveQosScriptClicked,
        onQosRunnerStartClicked = viewModel::onQosRunnerStartClicked,
        onQosRunnerPauseClicked = viewModel::onQosRunnerPauseClicked,
        onQosRunnerResumeClicked = viewModel::onQosRunnerResumeClicked,
        onQosRunnerPassClicked = viewModel::onQosRunnerPassClicked,
        onQosRunnerFailClicked = viewModel::onQosRunnerFailClicked,
        onQosRunnerBlockClicked = viewModel::onQosRunnerBlockClicked,
        onQosFamilyReasonCodeChanged = viewModel::onQosFamilyReasonCodeChanged,
        onQosFamilyFailureReasonChanged = viewModel::onQosFamilyFailureReasonChanged,
        onQosTargetTechnologyChanged = viewModel::onQosTargetTechnologyChanged,
        onQosTargetPhoneChanged = viewModel::onQosTargetPhoneChanged,
        onQosIterationCountChanged = viewModel::onQosIterationCountChanged,
        onQosSuccessCountChanged = viewModel::onQosSuccessCountChanged,
        onQosFailureCountChanged = viewModel::onQosFailureCountChanged,
        onNotesChanged = viewModel::onNotesChanged,
        onResultSummaryChanged = viewModel::onResultSummaryChanged,
        onSaveSummaryClicked = viewModel::onSaveSummaryClicked,
        onOpenLinkedDraftClicked = viewModel::onOpenLinkedDraftClicked
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceSessionScreen(
    state: PerformanceSessionUiState,
    onBack: () -> Unit,
    onCreateThroughputSessionClicked: () -> Unit,
    onCreateQosSessionClicked: () -> Unit,
    onSelectHistorySessionClicked: (String) -> Unit,
    onOperatorSelected: (String?) -> Unit,
    onTechnologySelected: (String?) -> Unit,
    onStepStatusSelected: (com.quartz.platform.domain.model.PerformanceStepCode, com.quartz.platform.domain.model.PerformanceStepStatus) -> Unit,
    onSessionStatusSelected: (PerformanceSessionStatus) -> Unit,
    onRefreshDeviceDiagnosticsClicked: () -> Unit,
    onApplyDeviceDiagnosticsClicked: () -> Unit,
    onPrerequisiteNetworkChanged: (Boolean) -> Unit,
    onPrerequisiteBatteryChanged: (Boolean) -> Unit,
    onPrerequisiteLocationChanged: (Boolean) -> Unit,
    onThroughputDownloadChanged: (String) -> Unit,
    onThroughputUploadChanged: (String) -> Unit,
    onThroughputLatencyChanged: (String) -> Unit,
    onThroughputMinDownloadChanged: (String) -> Unit,
    onThroughputMinUploadChanged: (String) -> Unit,
    onThroughputMaxLatencyChanged: (String) -> Unit,
    onQosScriptSelected: (String, String) -> Unit,
    onQosConfiguredRepeatChanged: (String) -> Unit,
    onQosScriptEditorNameChanged: (String) -> Unit,
    onQosScriptEditorRepeatChanged: (String) -> Unit,
    onQosScriptEditorTechnologiesChanged: (String) -> Unit,
    onQosScriptEditorFamilyToggled: (com.quartz.platform.domain.model.QosTestFamily) -> Unit,
    onSaveQosScriptClicked: () -> Unit,
    onQosRunnerStartClicked: (com.quartz.platform.domain.model.QosTestFamily) -> Unit,
    onQosRunnerPauseClicked: (com.quartz.platform.domain.model.QosTestFamily) -> Unit,
    onQosRunnerResumeClicked: (com.quartz.platform.domain.model.QosTestFamily) -> Unit,
    onQosRunnerPassClicked: (com.quartz.platform.domain.model.QosTestFamily) -> Unit,
    onQosRunnerFailClicked: (com.quartz.platform.domain.model.QosTestFamily) -> Unit,
    onQosRunnerBlockClicked: (com.quartz.platform.domain.model.QosTestFamily) -> Unit,
    onQosFamilyReasonCodeChanged: (com.quartz.platform.domain.model.QosTestFamily, com.quartz.platform.domain.model.QosExecutionIssueCode?) -> Unit,
    onQosFamilyFailureReasonChanged: (com.quartz.platform.domain.model.QosTestFamily, String) -> Unit,
    onQosTargetTechnologyChanged: (String) -> Unit,
    onQosTargetPhoneChanged: (String) -> Unit,
    onQosIterationCountChanged: (String) -> Unit,
    onQosSuccessCountChanged: (String) -> Unit,
    onQosFailureCountChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onResultSummaryChanged: (String) -> Unit,
    onSaveSummaryClicked: () -> Unit,
    onOpenLinkedDraftClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.title_performance_session)) })
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val session = state.session
        val workflowSignal = state.selectedSessionWorkflowType?.let { workflow ->
            OperationalSignal(
                text = stringResource(
                    R.string.performance_signal_workflow,
                    stringResource(performanceWorkflowTypeLabelRes(workflow))
                ),
                severity = OperationalSeverity.SUCCESS
            )
        } ?: OperationalSignal(
            text = stringResource(R.string.performance_signal_no_active_session),
            severity = OperationalSeverity.WARNING
        )
        val preflightReady = state.prerequisiteNetworkReady &&
            state.prerequisiteBatterySufficient &&
            state.prerequisiteLocationReady
        val preflightSignal = OperationalSignal(
            text = stringResource(
                R.string.performance_signal_prerequisites,
                if (preflightReady) stringResource(R.string.value_yes) else stringResource(R.string.value_no)
            ),
            severity = if (preflightReady) OperationalSeverity.SUCCESS else OperationalSeverity.WARNING
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MissionHeaderCard(
                    title = stringResource(R.string.performance_mission_title),
                    subtitle = stringResource(
                        R.string.performance_mission_subtitle,
                        state.siteLabel.ifBlank { state.siteId }
                    ),
                    signals = listOf(workflowSignal, preflightSignal),
                    metrics = buildList {
                        add(
                            OperationalMetric(
                                value = state.sessionHistory.size.toString(),
                                label = stringResource(R.string.performance_metric_history)
                            )
                        )
                        if (session != null) {
                            val completionGuard = session.completionGuard()
                            add(
                                OperationalMetric(
                                    value = "${completionGuard.completedRequiredStepCount}/${completionGuard.requiredStepCount}",
                                    label = stringResource(R.string.performance_metric_required_steps),
                                    severity = if (completionGuard.missingRequiredStepCount == 0) OperationalSeverity.SUCCESS else OperationalSeverity.WARNING
                                )
                            )
                        }
                        state.qosExecutionSnapshot?.engineState?.let { engineState ->
                            add(
                                OperationalMetric(
                                    value = stringResource(qosExecutionEngineStateLabelRes(engineState)),
                                    label = stringResource(R.string.performance_metric_engine),
                                    severity = when (engineState) {
                                        com.quartz.platform.domain.model.QosExecutionEngineState.RUNNING,
                                        com.quartz.platform.domain.model.QosExecutionEngineState.RESUMED,
                                        com.quartz.platform.domain.model.QosExecutionEngineState.COMPLETED -> OperationalSeverity.SUCCESS
                                        com.quartz.platform.domain.model.QosExecutionEngineState.PREFLIGHT_BLOCKED,
                                        com.quartz.platform.domain.model.QosExecutionEngineState.PAUSED -> OperationalSeverity.WARNING
                                        com.quartz.platform.domain.model.QosExecutionEngineState.FAILED,
                                        com.quartz.platform.domain.model.QosExecutionEngineState.BLOCKED -> OperationalSeverity.CRITICAL
                                        com.quartz.platform.domain.model.QosExecutionEngineState.READY -> OperationalSeverity.NORMAL
                                    }
                                )
                            )
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.performance_shell_disclaimer),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                val sessionStateSeverity = when {
                    session == null -> OperationalSeverity.NORMAL
                    !preflightReady -> OperationalSeverity.WARNING
                    state.selectedSessionWorkflowType == PerformanceWorkflowType.QOS_SCRIPT &&
                        state.qosExecutionSnapshot?.engineState in setOf(
                        com.quartz.platform.domain.model.QosExecutionEngineState.FAILED,
                        com.quartz.platform.domain.model.QosExecutionEngineState.BLOCKED
                    ) -> OperationalSeverity.CRITICAL
                    state.selectedSessionWorkflowType == PerformanceWorkflowType.QOS_SCRIPT &&
                        state.qosExecutionSnapshot?.engineState == com.quartz.platform.domain.model.QosExecutionEngineState.COMPLETED -> {
                        OperationalSeverity.SUCCESS
                    }
                    state.selectedStatus == PerformanceSessionStatus.COMPLETED -> OperationalSeverity.SUCCESS
                    else -> OperationalSeverity.NORMAL
                }
                val sessionStateMessage = when {
                    session == null -> stringResource(R.string.performance_runtime_state_no_session)
                    !preflightReady -> stringResource(R.string.performance_runtime_state_preflight_blocked)
                    state.selectedSessionWorkflowType == PerformanceWorkflowType.QOS_SCRIPT &&
                        state.qosExecutionSnapshot != null -> {
                        stringResource(
                            R.string.performance_runtime_state_qos_engine,
                            stringResource(qosExecutionEngineStateLabelRes(state.qosExecutionSnapshot.engineState))
                        )
                    }
                    else -> stringResource(
                        R.string.performance_runtime_state_session_status,
                        stringResource(performanceSessionStatusLabelRes(state.selectedStatus))
                    )
                }
                val sessionStateHint = when {
                    session == null -> stringResource(R.string.performance_runtime_state_no_session_hint)
                    !preflightReady -> stringResource(R.string.performance_runtime_state_preflight_blocked_hint)
                    state.hasUnsavedChanges -> stringResource(R.string.performance_runtime_state_unsaved_hint)
                    else -> stringResource(R.string.performance_runtime_state_ready_hint)
                }
                OperationalStateBanner(
                    title = stringResource(R.string.performance_runtime_state_banner_title),
                    message = sessionStateMessage,
                    severity = sessionStateSeverity,
                    hint = sessionStateHint
                )
            }

            state.errorMessage?.let { error ->
                item {
                    OperationalMessageCard(
                        title = stringResource(R.string.home_runtime_alert_title),
                        message = error,
                        severity = OperationalSeverity.CRITICAL
                    )
                }
            }

            state.infoMessage?.let { info ->
                item {
                    OperationalMessageCard(
                        title = stringResource(R.string.home_runtime_info_title),
                        message = info,
                        severity = OperationalSeverity.NORMAL
                    )
                }
            }

            if (session == null) {
                item {
                    SessionCreationCard(
                        isCreatingSession = state.isCreatingSession,
                        selectedOperator = state.selectedOperator,
                        selectedTechnology = state.selectedTechnology,
                        availableOperators = state.availableOperators,
                        availableTechnologies = state.availableTechnologies,
                        onOperatorSelected = onOperatorSelected,
                        onTechnologySelected = onTechnologySelected,
                        onCreateThroughputSessionClicked = onCreateThroughputSessionClicked,
                        onCreateQosSessionClicked = onCreateQosSessionClicked
                    )
                }
            } else {
                item {
                    SessionSummaryCard(
                        session = session,
                        selectedStatus = state.selectedStatus,
                        historyCount = state.sessionHistory.size,
                        onSessionStatusSelected = onSessionStatusSelected
                    )
                }

                item {
                    PreflightReadinessCard(
                        networkReady = state.prerequisiteNetworkReady,
                        batterySufficient = state.prerequisiteBatterySufficient,
                        locationReady = state.prerequisiteLocationReady,
                        observedNetworkStatus = state.observedNetworkStatus,
                        observedBatteryLevelPercent = state.observedBatteryLevelPercent,
                        observedBatteryIsCharging = state.observedBatteryIsCharging,
                        observedBatterySufficient = state.observedBatterySufficient,
                        observedLocationAvailable = state.observedLocationAvailable,
                        observedSignalsCapturedAtEpochMillis = state.observedSignalsCapturedAtEpochMillis,
                        isRefreshing = state.isRefreshingDeviceDiagnostics,
                        onRefreshDeviceDiagnosticsClicked = onRefreshDeviceDiagnosticsClicked,
                        onApplyDeviceDiagnosticsClicked = onApplyDeviceDiagnosticsClicked,
                        onPrerequisiteNetworkChanged = onPrerequisiteNetworkChanged,
                        onPrerequisiteBatteryChanged = onPrerequisiteBatteryChanged,
                        onPrerequisiteLocationChanged = onPrerequisiteLocationChanged
                    )
                }

                when (session.workflowType) {
                    PerformanceWorkflowType.THROUGHPUT -> {
                        item {
                            ThroughputMissionCard(
                                download = state.throughputDownloadInput,
                                upload = state.throughputUploadInput,
                                latency = state.throughputLatencyInput,
                                minDownload = state.throughputMinDownloadInput,
                                minUpload = state.throughputMinUploadInput,
                                maxLatency = state.throughputMaxLatencyInput,
                                onThroughputDownloadChanged = onThroughputDownloadChanged,
                                onThroughputUploadChanged = onThroughputUploadChanged,
                                onThroughputLatencyChanged = onThroughputLatencyChanged,
                                onThroughputMinDownloadChanged = onThroughputMinDownloadChanged,
                                onThroughputMinUploadChanged = onThroughputMinUploadChanged,
                                onThroughputMaxLatencyChanged = onThroughputMaxLatencyChanged
                            )
                        }
                    }

                    PerformanceWorkflowType.QOS_SCRIPT -> {
                        val sortedFamilies = state.qosSelectedTestFamilies.sortedBy { it.name }
                        val activeFamily = state.qosExecutionSnapshot?.activeFamily ?: sortedFamilies.firstOrNull()
                        val activeFamilyStatus = activeFamily?.let { state.qosFamilyStatusByType[it] }
                            ?: QosFamilyExecutionStatus.NOT_RUN
                        val activeCoverage = activeFamily?.let { state.qosFamilyRunCoverageByType[it] }
                        val activePreflightIssues = activeFamily?.let { state.qosPreflightIssuesByFamily[it].orEmpty() }.orEmpty()

                        item {
                            QosMissionOverviewCard(
                                selectedScriptName = state.qosSelectedScriptName,
                                selectedTestFamilies = state.qosSelectedTestFamilies,
                                configuredRepeat = state.qosConfiguredRepeatInput,
                                configuredTechnologies = state.qosConfiguredTechnologies,
                                scriptSnapshotUpdatedAtEpochMillis = state.qosScriptSnapshotUpdatedAtEpochMillis,
                                executionSnapshot = state.qosExecutionSnapshot
                            )
                        }
                        item {
                            QosPreflightReadinessCard(
                                selectedTestFamilies = state.qosSelectedTestFamilies,
                                runCoverageByType = state.qosFamilyRunCoverageByType,
                                executionSnapshot = state.qosExecutionSnapshot,
                                activeFamilyPreflightIssues = activePreflightIssues,
                                completionIssues = state.qosCompletionIssues,
                                showCompletionIssues = state.selectedStatus == PerformanceSessionStatus.COMPLETED
                            )
                        }
                        item {
                            QosActiveRunCard(
                                activeFamily = activeFamily,
                                activeFamilyStatus = activeFamilyStatus,
                                activeCoverage = activeCoverage,
                                executionSnapshot = state.qosExecutionSnapshot,
                                onQosRunnerStartClicked = onQosRunnerStartClicked,
                                onQosRunnerPauseClicked = onQosRunnerPauseClicked,
                                onQosRunnerResumeClicked = onQosRunnerResumeClicked,
                                onQosRunnerPassClicked = onQosRunnerPassClicked,
                                onQosRunnerFailClicked = onQosRunnerFailClicked,
                                onQosRunnerBlockClicked = onQosRunnerBlockClicked
                            )
                        }
                        item {
                            QosOutcomeCaptureCard(
                                configuredRepeat = state.qosConfiguredRepeatInput,
                                configuredTechnologies = state.qosConfiguredTechnologies,
                                targetTechnology = state.qosTargetTechnologyInput,
                                targetPhone = state.qosTargetPhoneInput,
                                iterationCount = state.qosIterationCountInput,
                                successCount = state.qosSuccessCountInput,
                                failureCount = state.qosFailureCountInput,
                                activeFamily = activeFamily,
                                activeFamilyStatus = activeFamilyStatus,
                                familyReasonCodeByType = state.qosFamilyReasonCodeByType,
                                familyFailureReasonByType = state.qosFamilyFailureReasonByType,
                                onQosConfiguredRepeatChanged = onQosConfiguredRepeatChanged,
                                onQosTargetTechnologyChanged = onQosTargetTechnologyChanged,
                                onQosTargetPhoneChanged = onQosTargetPhoneChanged,
                                onQosIterationCountChanged = onQosIterationCountChanged,
                                onQosSuccessCountChanged = onQosSuccessCountChanged,
                                onQosFailureCountChanged = onQosFailureCountChanged,
                                onQosFamilyReasonCodeChanged = onQosFamilyReasonCodeChanged,
                                onQosFamilyFailureReasonChanged = onQosFamilyFailureReasonChanged
                            )
                        }
                        item {
                            QosAdvancedSectionsCard(
                                selectedScriptId = state.qosSelectedScriptId,
                                availableScripts = state.availableQosScripts,
                                runPlan = state.qosRunPlan,
                                timelineEvents = state.qosExecutionTimelineEvents,
                                scriptEditorName = state.qosScriptEditorNameInput,
                                scriptEditorRepeat = state.qosScriptEditorRepeatInput,
                                scriptEditorTechnologies = state.qosScriptEditorTechnologiesInput,
                                scriptEditorSelectedFamilies = state.qosScriptEditorSelectedFamilies,
                                isSavingScript = state.isSavingQosScript,
                                sortedFamilies = sortedFamilies,
                                familyStatusByType = state.qosFamilyStatusByType,
                                runCoverageByType = state.qosFamilyRunCoverageByType,
                                onQosScriptSelected = onQosScriptSelected,
                                onQosScriptEditorNameChanged = onQosScriptEditorNameChanged,
                                onQosScriptEditorRepeatChanged = onQosScriptEditorRepeatChanged,
                                onQosScriptEditorTechnologiesChanged = onQosScriptEditorTechnologiesChanged,
                                onQosScriptEditorFamilyToggled = onQosScriptEditorFamilyToggled,
                                onSaveQosScriptClicked = onSaveQosScriptClicked
                            )
                        }
                    }
                }

                item {
                    ReviewCaptureCard(
                        resultSummaryInput = state.resultSummaryInput,
                        notesInput = state.notesInput,
                        onResultSummaryChanged = onResultSummaryChanged,
                        onNotesChanged = onNotesChanged
                    )
                }

                item {
                    PerformanceSupportSectionsCard(
                        sessionHistory = state.sessionHistory,
                        selectedSessionId = state.selectedSessionId,
                        steps = session.steps,
                        onSelectHistorySessionClicked = onSelectHistorySessionClicked,
                        onStepStatusSelected = onStepStatusSelected
                    )
                }
            }

            state.completionGuardMessage?.let { guard ->
                item {
                    OperationalMessageCard(
                        title = stringResource(R.string.home_runtime_alert_title),
                        message = guard,
                        severity = OperationalSeverity.CRITICAL
                    )
                }
            }

            if (state.session != null) {
                item {
                    val shouldShowSavePrimary = state.hasUnsavedChanges
                    MissionPrimaryActionBar(
                        primaryAction = {
                            MissionPrimaryActionButton(
                                label = if (shouldShowSavePrimary) {
                                    if (state.isSavingSummary) {
                                        stringResource(R.string.performance_action_save_loading)
                                    } else {
                                        stringResource(R.string.performance_action_save)
                                    }
                                } else {
                                    stringResource(R.string.action_open_linked_report_draft)
                                },
                                onClick = if (shouldShowSavePrimary) {
                                    onSaveSummaryClicked
                                } else {
                                    onOpenLinkedDraftClicked
                                },
                                enabled = if (shouldShowSavePrimary) {
                                    !state.isSavingSummary
                                } else {
                                    true
                                }
                            )
                        },
                        secondaryAction = {
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = if (shouldShowSavePrimary) {
                                    onOpenLinkedDraftClicked
                                } else {
                                    onBack
                                }
                            ) {
                                Text(
                                    if (shouldShowSavePrimary) {
                                        stringResource(R.string.action_open_linked_report_draft)
                                    } else {
                                        stringResource(R.string.action_back_to_site)
                                    }
                                )
                            }
                            if (shouldShowSavePrimary) {
                                OutlinedButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = onBack
                                ) {
                                    Text(stringResource(R.string.action_back_to_site))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
