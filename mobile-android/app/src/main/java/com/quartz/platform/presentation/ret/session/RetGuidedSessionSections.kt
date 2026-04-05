package com.quartz.platform.presentation.ret.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.quartz.platform.R
import com.quartz.platform.domain.model.RetGuidedSession
import com.quartz.platform.domain.model.RetProximityEligibilityState
import com.quartz.platform.domain.model.RetReferenceAltitudeSourceState
import com.quartz.platform.domain.model.RetResultOutcome
import com.quartz.platform.domain.model.RetSessionStatus
import com.quartz.platform.domain.model.RetStepCode
import com.quartz.platform.domain.model.RetStepStatus
import com.quartz.platform.presentation.components.AdvancedDisclosureButton
import com.quartz.platform.presentation.components.MissionHeaderCard
import com.quartz.platform.presentation.components.OperationalEmptyStateCard
import com.quartz.platform.presentation.components.OperationalMessageCard
import com.quartz.platform.presentation.components.OperationalMetric
import com.quartz.platform.presentation.components.OperationalSectionCard
import com.quartz.platform.presentation.components.OperationalSeverity
import com.quartz.platform.presentation.components.OperationalSignal
import com.quartz.platform.presentation.components.OperationalSignalRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RetMissionHeaderCard(
    state: RetGuidedSessionUiState,
    onResumeLatestClicked: () -> Unit,
    onCreateSessionClicked: () -> Unit
) {
    val missionSubtitle = stringResource(
        R.string.ret_label_site_sector,
        state.siteLabel.ifBlank { state.siteId },
        state.sectorCode.ifBlank { state.sectorId }
    )
    val hasLatest = state.sessionHistory.isNotEmpty()

    MissionHeaderCard(
        title = stringResource(R.string.ret_mission_runtime_title),
        subtitle = missionSubtitle,
        signals = listOf(
            OperationalSignal(
                text = stringResource(
                    R.string.ret_mission_signal_history_count,
                    state.sessionHistory.size
                )
            ),
            OperationalSignal(
                text = stringResource(
                    R.string.ret_mission_signal_proximity_mode,
                    if (state.proximityModeEnabled) {
                        stringResource(R.string.value_yes)
                    } else {
                        stringResource(R.string.value_no)
                    }
                ),
                severity = if (state.proximityModeEnabled) {
                    OperationalSeverity.SUCCESS
                } else {
                    OperationalSeverity.NORMAL
                }
            )
        ),
        metrics = listOf(
            OperationalMetric(
                value = state.sessionHistory.size.toString(),
                label = stringResource(R.string.ret_metric_sessions)
            ),
            OperationalMetric(
                value = if (state.session != null) {
                    stringResource(R.string.value_yes)
                } else {
                    stringResource(R.string.value_no)
                },
                label = stringResource(R.string.ret_metric_session_active),
                severity = if (state.session != null) {
                    OperationalSeverity.SUCCESS
                } else {
                    OperationalSeverity.WARNING
                }
            )
        ),
        primaryAction = {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isCreatingSession,
                onClick = {
                    if (hasLatest) {
                        onResumeLatestClicked()
                    } else {
                        onCreateSessionClicked()
                    }
                }
            ) {
                Text(
                    when {
                        state.isCreatingSession -> stringResource(R.string.ret_action_create_session_loading)
                        hasLatest -> stringResource(R.string.ret_action_resume_latest)
                        else -> stringResource(R.string.ret_action_create_session)
                    }
                )
            }
        },
        secondaryActions = {
            if (hasLatest) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isCreatingSession,
                    onClick = onCreateSessionClicked
                ) {
                    Text(stringResource(R.string.ret_action_create_session))
                }
            }
        }
    ) {
        Text(
            text = stringResource(R.string.ret_shell_disclaimer),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun RetRuntimeStateBanner(state: RetGuidedSessionUiState) {
    val stateBanner = when {
        state.session == null -> Triple(
            stringResource(R.string.ret_runtime_state_title),
            stringResource(R.string.ret_runtime_state_no_session_message),
            OperationalSeverity.WARNING
        )
        state.completionGuardMessage != null -> Triple(
            stringResource(R.string.ret_runtime_state_title),
            stringResource(R.string.ret_runtime_state_blocked_message),
            OperationalSeverity.CRITICAL
        )
        state.hasUnsavedChanges -> Triple(
            stringResource(R.string.ret_runtime_state_title),
            stringResource(R.string.ret_runtime_state_unsaved_message),
            OperationalSeverity.WARNING
        )
        else -> Triple(
            stringResource(R.string.ret_runtime_state_title),
            stringResource(R.string.ret_runtime_state_ready_message),
            OperationalSeverity.SUCCESS
        )
    }

    OperationalMessageCard(
        title = stateBanner.first,
        message = stateBanner.second,
        severity = stateBanner.third
    )
}

@Composable
fun RetEmptyRuntimeCard() {
    OperationalEmptyStateCard(
        title = stringResource(R.string.ret_section_runtime_empty_title),
        message = stringResource(R.string.ret_empty_session)
    )
}

@Composable
fun RetMissionSummaryCard(state: RetGuidedSessionUiState) {
    val session = requireNotNull(state.session)
    val requiredSteps = session.steps.count { it.required }
    val completedRequiredSteps = session.steps.count { it.required && it.status == RetStepStatus.DONE }
    OperationalSectionCard(
        title = stringResource(R.string.ret_section_mission_status),
        subtitle = stringResource(
            R.string.ret_label_site_sector,
            state.siteLabel.ifBlank { state.siteId },
            state.sectorCode.ifBlank { state.sectorId }
        )
    ) {
        OperationalSignalRow(
            signals = listOf(
                OperationalSignal(
                    text = stringResource(
                        R.string.ret_label_session_status,
                        stringResource(retSessionStatusLabelRes(session.status))
                    )
                ),
                OperationalSignal(
                    text = stringResource(
                        R.string.ret_label_result_outcome,
                        stringResource(retResultOutcomeLabelRes(state.selectedOutcome))
                    ),
                    severity = if (state.selectedOutcome == RetResultOutcome.PASS) {
                        OperationalSeverity.SUCCESS
                    } else {
                        OperationalSeverity.WARNING
                    }
                ),
                OperationalSignal(
                    text = stringResource(
                        R.string.ret_mission_required_progress,
                        completedRequiredSteps,
                        requiredSteps
                    )
                )
            )
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

@Composable
fun RetPrimaryActionsCard(
    hasUnsavedChanges: Boolean,
    isSavingSummary: Boolean,
    isCreatingDraft: Boolean,
    onSaveSummaryClicked: () -> Unit,
    onCreateReportDraft: () -> Unit
) {
    OperationalSectionCard(
        title = stringResource(R.string.ret_section_primary_actions_title),
        subtitle = stringResource(R.string.ret_section_primary_actions_hint)
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = hasUnsavedChanges && !isSavingSummary,
            onClick = onSaveSummaryClicked
        ) {
            Text(
                if (isSavingSummary) {
                    stringResource(R.string.ret_action_save_summary_loading)
                } else {
                    stringResource(R.string.ret_action_save_summary)
                }
            )
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCreatingDraft,
            onClick = onCreateReportDraft
        ) {
            Text(
                if (isCreatingDraft) {
                    stringResource(R.string.ret_action_create_report_draft_loading)
                } else {
                    stringResource(R.string.ret_action_create_report_draft)
                }
            )
        }
    }
}

@Composable
fun RetCompletionGuardCard(message: String) {
    OperationalMessageCard(
        title = stringResource(R.string.ret_section_blocking_points_title),
        message = message,
        severity = OperationalSeverity.CRITICAL
    )
}

@Composable
fun RetChecklistHeaderCard(completedRequiredSteps: Int, requiredSteps: Int) {
    OperationalSectionCard(
        title = stringResource(R.string.ret_header_checklist),
        subtitle = stringResource(
            R.string.ret_mission_required_progress,
            completedRequiredSteps,
            requiredSteps
        )
    ) {
        Text(
            text = stringResource(R.string.ret_checklist_hint),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun RetReviewCaptureHeaderCard() {
    OperationalSectionCard(
        title = stringResource(R.string.ret_section_summary_inputs_title),
        subtitle = stringResource(R.string.ret_section_summary_inputs_hint)
    ) {
        Text(
            text = stringResource(R.string.ret_review_capture_hint),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun RetHistoryCollapsedHintCard() {
    OperationalSectionCard(
        title = stringResource(R.string.ret_header_session_history, 0)
    ) {
        Text(
            text = stringResource(R.string.ret_history_collapsed_hint),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun RetGeospatialSessionSurfaceCard(
    state: RetGuidedSessionUiState,
    onMeasurementZoneExtensionReasonChanged: (String) -> Unit,
    onProximityReferenceAltitudeChanged: (String) -> Unit,
    onExtendMeasurementZoneClicked: () -> Unit,
    onResetMeasurementZoneClicked: () -> Unit,
    onToggleProximityModeClicked: (Boolean) -> Unit,
    onRefreshUserLocationClicked: () -> Unit
) {
    var showAdvancedGeospatial by remember { mutableStateOf(false) }
    OperationalSectionCard(
        title = stringResource(R.string.ret_header_geospatial),
        subtitle = stringResource(R.string.ret_section_geospatial_hint)
    ) {
        OperationalSignalRow(
            signals = listOf(
                OperationalSignal(
                    text = stringResource(
                        R.string.ret_label_distance_to_zone,
                        state.distanceToMeasurementZoneMeters?.toString()
                            ?: stringResource(R.string.value_not_available)
                    ),
                    severity = when (state.proximityEligibilityState) {
                        RetProximityEligibilityState.ELIGIBLE -> OperationalSeverity.SUCCESS
                        RetProximityEligibilityState.INELIGIBLE -> OperationalSeverity.CRITICAL
                        RetProximityEligibilityState.SUPPORTED -> OperationalSeverity.WARNING
                        RetProximityEligibilityState.UNAVAILABLE -> OperationalSeverity.WARNING
                    }
                ),
                OperationalSignal(
                    text = stringResource(
                        R.string.ret_label_zone_radius,
                        state.measurementZoneRadiusMeters
                    )
                ),
                OperationalSignal(
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
                    )
                )
            )
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
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onRefreshUserLocationClicked
            ) {
                Text(stringResource(R.string.ret_action_refresh_position))
            }
        }

        AdvancedDisclosureButton(
            expanded = showAdvancedGeospatial,
            onToggle = { showAdvancedGeospatial = !showAdvancedGeospatial },
            showLabel = stringResource(R.string.ret_action_show_geospatial_advanced),
            hideLabel = stringResource(R.string.ret_action_hide_geospatial_advanced)
        )

        if (showAdvancedGeospatial) {
            OperationalSectionCard(
                title = stringResource(R.string.ret_geospatial_advanced_title),
                subtitle = stringResource(R.string.ret_geospatial_advanced_hint)
            ) {
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
                androidx.compose.material3.OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.measurementZoneExtensionReasonInput,
                    onValueChange = onMeasurementZoneExtensionReasonChanged,
                    label = { Text(stringResource(R.string.ret_input_zone_extension_reason)) },
                    supportingText = { Text(stringResource(R.string.ret_hint_zone_extension_reason)) }
                )
                androidx.compose.material3.OutlinedTextField(
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
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onResetMeasurementZoneClicked
                    ) {
                        Text(stringResource(R.string.ret_action_reset_zone))
                    }
                }
            }
        }
    }
}

@Composable
fun SessionHistoryCard(
    session: RetGuidedSession,
    isSelected: Boolean,
    onSelectHistorySessionClicked: (String) -> Unit
) {
    OperationalSectionCard(
        title = stringResource(
            R.string.ret_label_history_item,
            stringResource(retSessionStatusLabelRes(session.status)),
            formatEpoch(session.updatedAtEpochMillis)
        ),
        subtitle = stringResource(
            R.string.ret_label_result_outcome,
            stringResource(retResultOutcomeLabelRes(session.resultOutcome))
        )
    ) {
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

@Composable
fun SessionStatusCard(
    state: RetGuidedSessionUiState,
    onSessionStatusSelected: (RetSessionStatus) -> Unit
) {
    val session = requireNotNull(state.session)
    OperationalSectionCard(
        title = stringResource(
            R.string.ret_label_session_status,
            stringResource(retSessionStatusLabelRes(session.status))
        ),
        subtitle = stringResource(R.string.label_updated_at, formatEpoch(session.updatedAtEpochMillis))
    ) {
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

@Composable
fun ResultOutcomeCard(
    selectedOutcome: RetResultOutcome,
    onResultOutcomeSelected: (RetResultOutcome) -> Unit
) {
    OperationalSectionCard(
        title = stringResource(
            R.string.ret_label_result_outcome,
            stringResource(retResultOutcomeLabelRes(selectedOutcome))
        )
    ) {
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

@Composable
fun StepCard(
    stepCode: RetStepCode,
    required: Boolean,
    status: RetStepStatus,
    onStepStatusSelected: (RetStepCode, RetStepStatus) -> Unit
) {
    OperationalSectionCard(
        title = stringResource(retStepCodeLabelRes(stepCode)),
        subtitle = stringResource(retStepInstructionLabelRes(stepCode))
    ) {
        OperationalSignalRow(
            signals = listOf(
                OperationalSignal(
                    text = if (required) {
                        stringResource(R.string.ret_label_required_step)
                    } else {
                        stringResource(R.string.ret_label_optional_step)
                    }
                ),
                OperationalSignal(
                    text = stringResource(
                        R.string.ret_label_step_status,
                        stringResource(retStepStatusLabelRes(status))
                    ),
                    severity = when (status) {
                        RetStepStatus.DONE -> OperationalSeverity.SUCCESS
                        RetStepStatus.BLOCKED -> OperationalSeverity.CRITICAL
                        RetStepStatus.IN_PROGRESS -> OperationalSeverity.WARNING
                        RetStepStatus.TODO -> OperationalSeverity.NORMAL
                    }
                )
            )
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

private val dateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())

private fun formatEpoch(epochMillis: Long): String {
    return dateFormatter.format(Instant.ofEpochMilli(epochMillis))
}
