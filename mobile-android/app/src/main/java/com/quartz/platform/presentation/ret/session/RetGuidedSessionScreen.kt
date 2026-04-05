package com.quartz.platform.presentation.ret.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quartz.platform.R
import com.quartz.platform.domain.model.RetResultOutcome
import com.quartz.platform.domain.model.RetSessionStatus
import com.quartz.platform.domain.model.RetStepCode
import com.quartz.platform.domain.model.RetStepStatus
import com.quartz.platform.presentation.components.AdvancedDisclosureButton
import com.quartz.platform.presentation.components.OperationalMessageCard
import com.quartz.platform.presentation.components.OperationalSeverity
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RetGuidedSessionRoute(
    onBack: () -> Unit,
    onOpenDraft: (String) -> Unit,
    viewModel: RetGuidedSessionViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is RetGuidedSessionEvent.OpenDraft -> onOpenDraft(event.draftId)
            }
        }
    }

    RetGuidedSessionScreen(
        state = state,
        onBack = onBack,
        onCreateSessionClicked = viewModel::onCreateSessionClicked,
        onResumeLatestClicked = viewModel::onResumeLatestClicked,
        onSelectHistorySessionClicked = viewModel::onSelectHistorySessionClicked,
        onStepStatusSelected = viewModel::onStepStatusSelected,
        onMeasurementZoneExtensionReasonChanged = viewModel::onMeasurementZoneExtensionReasonChanged,
        onProximityReferenceAltitudeChanged = viewModel::onProximityReferenceAltitudeChanged,
        onExtendMeasurementZoneClicked = viewModel::onExtendMeasurementZoneClicked,
        onResetMeasurementZoneClicked = viewModel::onResetMeasurementZoneClicked,
        onToggleProximityModeClicked = viewModel::onToggleProximityModeClicked,
        onRefreshUserLocationClicked = viewModel::onRefreshUserLocationClicked,
        onSessionStatusSelected = viewModel::onSessionStatusSelected,
        onResultOutcomeSelected = viewModel::onResultOutcomeSelected,
        onNotesChanged = viewModel::onNotesChanged,
        onResultSummaryChanged = viewModel::onResultSummaryChanged,
        onSaveSummaryClicked = viewModel::onSaveSummaryClicked,
        onCreateReportDraft = viewModel::onCreateReportDraftClicked
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetGuidedSessionScreen(
    state: RetGuidedSessionUiState,
    onBack: () -> Unit,
    onCreateSessionClicked: () -> Unit,
    onResumeLatestClicked: () -> Unit,
    onSelectHistorySessionClicked: (String) -> Unit,
    onStepStatusSelected: (RetStepCode, RetStepStatus) -> Unit,
    onMeasurementZoneExtensionReasonChanged: (String) -> Unit,
    onProximityReferenceAltitudeChanged: (String) -> Unit,
    onExtendMeasurementZoneClicked: () -> Unit,
    onResetMeasurementZoneClicked: () -> Unit,
    onToggleProximityModeClicked: (Boolean) -> Unit,
    onRefreshUserLocationClicked: () -> Unit,
    onSessionStatusSelected: (RetSessionStatus) -> Unit,
    onResultOutcomeSelected: (RetResultOutcome) -> Unit,
    onNotesChanged: (String) -> Unit,
    onResultSummaryChanged: (String) -> Unit,
    onSaveSummaryClicked: () -> Unit,
    onCreateReportDraft: () -> Unit
) {
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showExecutionControls by rememberSaveable { mutableStateOf(false) }
    var showReviewCapture by rememberSaveable { mutableStateOf(false) }
    var showChecklist by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(state.session?.id) {
        showHistory = false
        showExecutionControls = false
        showReviewCapture = false
        showChecklist = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_ret_guided_session)) }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        RetMissionHeaderCard(
                            state = state,
                            onResumeLatestClicked = onResumeLatestClicked,
                            onCreateSessionClicked = onCreateSessionClicked
                        )
                    }

                    item {
                        RetRuntimeStateBanner(state = state)
                    }

                    if (state.session == null) {
                        item {
                            RetEmptyRuntimeCard()
                        }
                    } else {
                        item {
                            RetMissionSummaryCard(state = state)
                        }

                        item {
                            RetPrimaryActionsCard(
                                hasUnsavedChanges = state.hasUnsavedChanges,
                                isSavingSummary = state.isSavingSummary,
                                isCreatingDraft = state.isCreatingDraft,
                                onSaveSummaryClicked = onSaveSummaryClicked,
                                onCreateReportDraft = onCreateReportDraft
                            )
                        }

                        state.completionGuardMessage?.let { guardMessage ->
                            item {
                                RetCompletionGuardCard(message = guardMessage)
                            }
                        }

                        item {
                            RetGeospatialSessionSurfaceCard(
                                state = state,
                                onMeasurementZoneExtensionReasonChanged = onMeasurementZoneExtensionReasonChanged,
                                onProximityReferenceAltitudeChanged = onProximityReferenceAltitudeChanged,
                                onExtendMeasurementZoneClicked = onExtendMeasurementZoneClicked,
                                onResetMeasurementZoneClicked = onResetMeasurementZoneClicked,
                                onToggleProximityModeClicked = onToggleProximityModeClicked,
                                onRefreshUserLocationClicked = onRefreshUserLocationClicked
                            )
                        }

                        item {
                            AdvancedDisclosureButton(
                                expanded = showChecklist,
                                onToggle = { showChecklist = !showChecklist },
                                showLabel = stringResource(R.string.ret_action_show_checklist),
                                hideLabel = stringResource(R.string.ret_action_hide_checklist)
                            )
                        }

                        if (showChecklist) {
                            item {
                                RetChecklistHeaderCard(
                                    completedRequiredSteps = state.session.steps.count { step ->
                                        step.required && step.status == RetStepStatus.DONE
                                    },
                                    requiredSteps = state.session.steps.count { it.required }
                                )
                            }
                            items(state.session.steps, key = { it.code.name }) { step ->
                                StepCard(
                                    stepCode = step.code,
                                    required = step.required,
                                    status = step.status,
                                    onStepStatusSelected = onStepStatusSelected
                                )
                            }
                        }

                        item {
                            AdvancedDisclosureButton(
                                expanded = showExecutionControls,
                                onToggle = { showExecutionControls = !showExecutionControls },
                                showLabel = stringResource(R.string.ret_action_show_execution_controls),
                                hideLabel = stringResource(R.string.ret_action_hide_execution_controls)
                            )
                        }

                        if (showExecutionControls) {
                            item {
                                SessionStatusCard(
                                    state = state,
                                    onSessionStatusSelected = onSessionStatusSelected
                                )
                            }
                            item {
                                ResultOutcomeCard(
                                    selectedOutcome = state.selectedOutcome,
                                    onResultOutcomeSelected = onResultOutcomeSelected
                                )
                            }
                        }

                        item {
                            AdvancedDisclosureButton(
                                expanded = showReviewCapture,
                                onToggle = { showReviewCapture = !showReviewCapture },
                                showLabel = stringResource(R.string.ret_action_show_review_capture),
                                hideLabel = stringResource(R.string.ret_action_hide_review_capture)
                            )
                        }

                        if (showReviewCapture) {
                            item {
                                RetReviewCaptureHeaderCard()
                            }
                            item {
                                OutlinedTextField(
                                    value = state.notesInput,
                                    onValueChange = onNotesChanged,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(stringResource(R.string.ret_input_notes)) }
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = state.resultSummaryInput,
                                    onValueChange = onResultSummaryChanged,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(stringResource(R.string.ret_input_result_summary)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                                )
                            }
                        }

                        item {
                            AdvancedDisclosureButton(
                                expanded = showHistory,
                                onToggle = { showHistory = !showHistory },
                                showLabel = stringResource(R.string.ret_action_show_history),
                                hideLabel = stringResource(R.string.ret_action_hide_history)
                            )
                        }

                        if (showHistory) {
                            items(state.sessionHistory, key = { it.id }) { historyItem ->
                                SessionHistoryCard(
                                    session = historyItem,
                                    isSelected = historyItem.id == state.session.id,
                                    onSelectHistorySessionClicked = onSelectHistorySessionClicked
                                )
                            }
                        } else {
                            item {
                                RetHistoryCollapsedHintCard()
                            }
                        }
                    }

                    state.errorMessage?.let { error ->
                        item {
                            OperationalMessageCard(
                                title = stringResource(R.string.ret_runtime_error_title),
                                message = error,
                                severity = OperationalSeverity.CRITICAL
                            )
                        }
                    }

                    state.infoMessage?.let { info ->
                        item {
                            OperationalMessageCard(
                                title = stringResource(R.string.ret_runtime_info_title),
                                message = info,
                                severity = OperationalSeverity.NORMAL
                            )
                        }
                    }

                    item {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onBack
                        ) {
                            Text(stringResource(R.string.action_back_to_site))
                        }
                    }
                }
            }
        }
    }
}
