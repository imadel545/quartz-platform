package com.quartz.platform.presentation.ret.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quartz.platform.R
import com.quartz.platform.domain.model.RetGuidedSession
import com.quartz.platform.domain.model.RetProximityEligibilityState
import com.quartz.platform.domain.model.RetReferenceAltitudeSourceState
import com.quartz.platform.domain.model.RetResultOutcome
import com.quartz.platform.domain.model.RetSessionStatus
import com.quartz.platform.domain.model.RetStepCode
import com.quartz.platform.domain.model.RetStepStatus
import androidx.compose.foundation.text.KeyboardOptions
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    LaunchedEffect(state.session?.id) {
        showHistory = false
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
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.ret_shell_disclaimer),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = stringResource(
                                        R.string.ret_label_site_sector,
                                        state.siteLabel.ifBlank { state.siteId },
                                        state.sectorCode.ifBlank { state.sectorId }
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    item {
                        SessionEntryChoiceCard(
                            hasLatest = state.sessionHistory.isNotEmpty(),
                            isCreating = state.isCreatingSession,
                            onResumeLatestClicked = onResumeLatestClicked,
                            onCreateSessionClicked = onCreateSessionClicked
                        )
                    }

                    if (state.session == null) {
                        item {
                            Text(
                                text = stringResource(R.string.ret_empty_session),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        item {
                            RetMissionSummaryCard(state = state)
                        }

                        item {
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { showHistory = !showHistory }
                            ) {
                                Text(
                                    if (showHistory) {
                                        stringResource(R.string.ret_action_hide_history)
                                    } else {
                                        stringResource(R.string.ret_action_show_history)
                                    }
                                )
                            }
                        }

                        item {
                            Text(
                                text = stringResource(
                                    R.string.ret_header_session_history,
                                    state.sessionHistory.size
                                ),
                                style = MaterialTheme.typography.titleMedium
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
                                Text(
                                    text = stringResource(R.string.ret_history_collapsed_hint),
                                    style = MaterialTheme.typography.bodySmall
                                )
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

                        item {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.hasUnsavedChanges && !state.isSavingSummary,
                                onClick = onSaveSummaryClicked
                            ) {
                                Text(
                                    if (state.isSavingSummary) {
                                        stringResource(R.string.ret_action_save_summary_loading)
                                    } else {
                                        stringResource(R.string.ret_action_save_summary)
                                    }
                                )
                            }
                        }

                        item {
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.isCreatingDraft,
                                onClick = onCreateReportDraft
                            ) {
                                Text(
                                    if (state.isCreatingDraft) {
                                        stringResource(R.string.ret_action_create_report_draft_loading)
                                    } else {
                                        stringResource(R.string.ret_action_create_report_draft)
                                    }
                                )
                            }
                        }

                        item {
                            Text(
                                text = stringResource(R.string.ret_header_checklist),
                                style = MaterialTheme.typography.titleMedium
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
                                label = { Text(stringResource(R.string.ret_input_result_summary)) }
                            )
                        }

                        state.completionGuardMessage?.let { guardMessage ->
                            item {
                                Text(
                                    text = guardMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    state.errorMessage?.let { error ->
                        item {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    state.infoMessage?.let { info ->
                        item {
                            Text(
                                text = info,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
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

@Composable
private fun RetMissionSummaryCard(state: RetGuidedSessionUiState) {
    val session = requireNotNull(state.session)
    val requiredSteps = session.steps.count { it.required }
    val completedRequiredSteps = session.steps.count { it.required && it.status == RetStepStatus.DONE }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.ret_section_mission_status),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(
                    R.string.ret_label_session_status,
                    stringResource(retSessionStatusLabelRes(session.status))
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.ret_label_result_outcome,
                    stringResource(retResultOutcomeLabelRes(state.selectedOutcome))
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.ret_mission_required_progress,
                    completedRequiredSteps,
                    requiredSteps
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = when (state.proximityEligibilityState) {
                    RetProximityEligibilityState.ELIGIBLE ->
                        stringResource(R.string.ret_helper_proximity_eligible)
                    RetProximityEligibilityState.INELIGIBLE ->
                        stringResource(R.string.ret_helper_proximity_ineligible)
                    RetProximityEligibilityState.SUPPORTED ->
                        stringResource(R.string.ret_helper_proximity_supported)
                    RetProximityEligibilityState.UNAVAILABLE ->
                        stringResource(R.string.ret_helper_proximity_unavailable)
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RetGeospatialSessionSurfaceCard(
    state: RetGuidedSessionUiState,
    onMeasurementZoneExtensionReasonChanged: (String) -> Unit,
    onProximityReferenceAltitudeChanged: (String) -> Unit,
    onExtendMeasurementZoneClicked: () -> Unit,
    onResetMeasurementZoneClicked: () -> Unit,
    onToggleProximityModeClicked: (Boolean) -> Unit,
    onRefreshUserLocationClicked: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.ret_header_geospatial),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(
                    R.string.ret_label_zone_radius,
                    state.measurementZoneRadiusMeters
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.ret_label_distance_to_zone,
                    state.distanceToMeasurementZoneMeters?.toString()
                        ?: stringResource(R.string.value_not_available)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.ret_label_inside_zone,
                    when (state.isInsideMeasurementZone) {
                        true -> stringResource(R.string.value_yes)
                        false -> stringResource(R.string.value_no)
                        null -> stringResource(R.string.value_not_available)
                    }
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.ret_label_proximity_eligibility,
                    when (state.proximityEligibilityState) {
                        RetProximityEligibilityState.ELIGIBLE ->
                            stringResource(R.string.ret_value_proximity_eligible)
                        RetProximityEligibilityState.INELIGIBLE ->
                            stringResource(R.string.ret_value_proximity_ineligible)
                        RetProximityEligibilityState.SUPPORTED ->
                            stringResource(R.string.ret_value_proximity_supported)
                        RetProximityEligibilityState.UNAVAILABLE ->
                            stringResource(R.string.ret_value_proximity_unavailable)
                    }
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.ret_label_user_altitude,
                    state.userAltitudeMeters?.let { String.format("%.1f", it) }
                        ?: stringResource(R.string.value_not_available)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.ret_label_effective_reference_altitude,
                    state.effectiveReferenceAltitudeMeters?.let { String.format("%.1f", it) }
                        ?: stringResource(R.string.value_not_available)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.ret_label_technical_reference_altitude,
                    state.technicalReferenceAltitudeMeters?.let { String.format("%.1f", it) }
                        ?: stringResource(R.string.value_not_available)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.ret_label_reference_altitude_source,
                    when (state.proximityReferenceAltitudeSource) {
                        RetReferenceAltitudeSourceState.TECHNICAL_DEFAULT ->
                            stringResource(R.string.ret_value_reference_altitude_source_technical_default)
                        RetReferenceAltitudeSourceState.OPERATOR_OVERRIDE ->
                            stringResource(R.string.ret_value_reference_altitude_source_operator_override)
                        RetReferenceAltitudeSourceState.UNAVAILABLE ->
                            stringResource(R.string.ret_value_reference_altitude_source_unavailable)
                    }
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = when (state.proximityEligibilityState) {
                    RetProximityEligibilityState.ELIGIBLE ->
                        stringResource(R.string.ret_helper_proximity_eligible)
                    RetProximityEligibilityState.INELIGIBLE ->
                        stringResource(R.string.ret_helper_proximity_ineligible)
                    RetProximityEligibilityState.SUPPORTED ->
                        stringResource(R.string.ret_helper_proximity_supported)
                    RetProximityEligibilityState.UNAVAILABLE ->
                        stringResource(R.string.ret_helper_proximity_unavailable)
                },
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.measurementZoneExtensionReasonInput,
                onValueChange = onMeasurementZoneExtensionReasonChanged,
                label = { Text(stringResource(R.string.ret_input_zone_extension_reason)) },
                supportingText = { Text(stringResource(R.string.ret_hint_zone_extension_reason)) }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.proximityReferenceAltitudeInput,
                onValueChange = onProximityReferenceAltitudeChanged,
                label = { Text(stringResource(R.string.ret_input_reference_altitude_override)) },
                supportingText = { Text(stringResource(R.string.ret_hint_reference_altitude_override)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onExtendMeasurementZoneClicked
                ) {
                    Text(stringResource(R.string.ret_action_extend_zone))
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onResetMeasurementZoneClicked
                ) {
                    Text(stringResource(R.string.ret_action_reset_zone))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { onToggleProximityModeClicked(!state.proximityModeEnabled) }
                ) {
                    Text(
                        if (state.proximityModeEnabled) {
                            stringResource(R.string.ret_action_disable_proximity)
                        } else {
                            stringResource(R.string.ret_action_enable_proximity)
                        }
                    )
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onRefreshUserLocationClicked
                ) {
                    Text(stringResource(R.string.ret_action_refresh_position))
                }
            }
        }
    }
}

@Composable
private fun SessionEntryChoiceCard(
    hasLatest: Boolean,
    isCreating: Boolean,
    onResumeLatestClicked: () -> Unit,
    onCreateSessionClicked: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.ret_entry_choice_title),
                style = MaterialTheme.typography.titleSmall
            )
            if (hasLatest) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onResumeLatestClicked
                ) {
                    Text(stringResource(R.string.ret_action_resume_latest))
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCreating,
                onClick = onCreateSessionClicked
            ) {
                Text(
                    if (isCreating) {
                        stringResource(R.string.ret_action_create_session_loading)
                    } else {
                        stringResource(R.string.ret_action_create_session)
                    }
                )
            }
        }
    }
}

@Composable
private fun SessionHistoryCard(
    session: RetGuidedSession,
    isSelected: Boolean,
    onSelectHistorySessionClicked: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.ret_label_history_item,
                    stringResource(retSessionStatusLabelRes(session.status)),
                    formatEpoch(session.updatedAtEpochMillis)
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.ret_label_result_outcome,
                    stringResource(retResultOutcomeLabelRes(session.resultOutcome))
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSelectHistorySessionClicked(session.id) }
            ) {
                Text(
                    if (isSelected) {
                        stringResource(R.string.ret_action_session_opened)
                    } else {
                        stringResource(R.string.ret_action_open_session)
                    }
                )
            }
        }
    }
}

@Composable
private fun SessionStatusCard(
    state: RetGuidedSessionUiState,
    onSessionStatusSelected: (RetSessionStatus) -> Unit
) {
    val session = requireNotNull(state.session)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.ret_label_session_status,
                    stringResource(retSessionStatusLabelRes(session.status))
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.label_updated_at, formatEpoch(session.updatedAtEpochMillis)),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.ret_label_status_update),
                style = MaterialTheme.typography.labelLarge
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RetSessionStatus.entries) { status ->
                    FilterChip(
                        selected = session.status == status,
                        onClick = { onSessionStatusSelected(status) },
                        label = { Text(stringResource(retSessionStatusLabelRes(status))) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultOutcomeCard(
    selectedOutcome: RetResultOutcome,
    onResultOutcomeSelected: (RetResultOutcome) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.ret_label_result_outcome,
                    stringResource(retResultOutcomeLabelRes(selectedOutcome))
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RetResultOutcome.entries) { outcome ->
                    FilterChip(
                        selected = selectedOutcome == outcome,
                        onClick = { onResultOutcomeSelected(outcome) },
                        label = { Text(stringResource(retResultOutcomeLabelRes(outcome))) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    stepCode: RetStepCode,
    required: Boolean,
    status: RetStepStatus,
    onStepStatusSelected: (RetStepCode, RetStepStatus) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(retStepCodeLabelRes(stepCode)),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(retStepInstructionLabelRes(stepCode)),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = if (required) {
                    stringResource(R.string.ret_label_required_step)
                } else {
                    stringResource(R.string.ret_label_optional_step)
                },
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.ret_label_step_status,
                    stringResource(retStepStatusLabelRes(status))
                ),
                style = MaterialTheme.typography.bodySmall
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RetStepStatus.entries) { target ->
                    FilterChip(
                        selected = status == target,
                        onClick = { onStepStatusSelected(stepCode, target) },
                        label = { Text(stringResource(retStepStatusLabelRes(target))) }
                    )
                }
            }
        }
    }
}

private val dateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())

private fun formatEpoch(epochMillis: Long): String {
    return dateFormatter.format(Instant.ofEpochMilli(epochMillis))
}
