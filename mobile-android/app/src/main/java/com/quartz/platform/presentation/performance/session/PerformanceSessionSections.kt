package com.quartz.platform.presentation.performance.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.quartz.platform.R
import com.quartz.platform.domain.model.NetworkStatus
import com.quartz.platform.domain.model.PerformanceGuidedStep
import com.quartz.platform.domain.model.PerformanceSession
import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.PerformanceStepCode
import com.quartz.platform.domain.model.PerformanceStepStatus
import com.quartz.platform.domain.model.QosCompletionIssue
import com.quartz.platform.domain.model.QosExecutionEngineState
import com.quartz.platform.domain.model.QosExecutionIssueCode
import com.quartz.platform.domain.model.QosExecutionSnapshot
import com.quartz.platform.domain.model.QosExecutionTimelineEvent
import com.quartz.platform.domain.model.QosFamilyExecutionStatus
import com.quartz.platform.domain.model.QosFamilyRunCoverage
import com.quartz.platform.domain.model.QosPreflightIssue
import com.quartz.platform.domain.model.QosRecoveryState
import com.quartz.platform.domain.model.QosRunPlanItem
import com.quartz.platform.domain.model.QosRunPlanItemStatus
import com.quartz.platform.domain.model.QosScriptDefinition
import com.quartz.platform.domain.model.QosTestFamily
import com.quartz.platform.presentation.components.AdvancedDisclosureButton
import com.quartz.platform.presentation.components.OperationalEmptyStateCard
import com.quartz.platform.presentation.components.OperationalMetric
import com.quartz.platform.presentation.components.OperationalMetricRow
import com.quartz.platform.presentation.components.OperationalSectionCard
import com.quartz.platform.presentation.components.OperationalSeverity
import com.quartz.platform.presentation.components.OperationalSignal
import com.quartz.platform.presentation.components.OperationalSignalRow

@Composable
fun SessionCreationCard(
    isCreatingSession: Boolean,
    selectedOperator: String?,
    selectedTechnology: String?,
    availableOperators: List<String>,
    availableTechnologies: List<String>,
    onOperatorSelected: (String?) -> Unit,
    onTechnologySelected: (String?) -> Unit,
    onCreateThroughputSessionClicked: () -> Unit,
    onCreateQosSessionClicked: () -> Unit
) {
    OperationalSectionCard(
        title = stringResource(R.string.performance_header_create_session),
        subtitle = stringResource(R.string.performance_section_create_session_hint)
    ) {
        OperationalSignalRow(
            signals = listOf(
                OperationalSignal(
                    text = stringResource(
                        R.string.performance_label_selected_operator,
                        selectedOperator ?: stringResource(R.string.value_not_available)
                    )
                ),
                OperationalSignal(
                    text = stringResource(
                        R.string.performance_label_selected_technology,
                        selectedTechnology ?: stringResource(R.string.value_not_available)
                    )
                )
            )
        )
        SelectionButtonRow(
            values = availableOperators,
            onSelected = onOperatorSelected
        )
        SelectionButtonRow(
            values = availableTechnologies,
            onSelected = onTechnologySelected
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCreatingSession,
            onClick = onCreateQosSessionClicked
        ) {
            Text(stringResource(R.string.performance_action_create_qos_session))
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCreatingSession,
            onClick = onCreateThroughputSessionClicked
        ) {
            Text(stringResource(R.string.performance_action_create_throughput_session))
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun SessionSummaryCard(
    session: PerformanceSession,
    selectedStatus: PerformanceSessionStatus,
    historyCount: Int,
    onSessionStatusSelected: (PerformanceSessionStatus) -> Unit
) {
    var showStatusControls by rememberSaveable { mutableStateOf(false) }
    val completionGuard = session.completionGuard()
    OperationalSectionCard(
        title = stringResource(R.string.performance_section_session_summary),
        subtitle = stringResource(R.string.performance_section_session_summary_hint)
    ) {
        OperationalSignalRow(
            signals = listOf(
                OperationalSignal(
                    text = stringResource(
                        R.string.performance_label_session_type,
                        stringResource(performanceWorkflowTypeLabelRes(session.workflowType))
                    )
                ),
                OperationalSignal(
                    text = stringResource(
                        R.string.performance_label_session_status,
                        stringResource(performanceSessionStatusLabelRes(selectedStatus))
                    ),
                    severity = sessionStatusSeverity(selectedStatus)
                )
            )
        )
        OperationalMetricRow(
            metrics = listOf(
                OperationalMetric(
                    value = historyCount.toString(),
                    label = stringResource(R.string.performance_metric_history)
                ),
                OperationalMetric(
                    value = "${completionGuard.completedRequiredStepCount}/${completionGuard.requiredStepCount}",
                    label = stringResource(R.string.performance_metric_required_steps),
                    severity = if (completionGuard.missingRequiredStepCount == 0) {
                        OperationalSeverity.SUCCESS
                    } else {
                        OperationalSeverity.WARNING
                    }
                ),
                OperationalMetric(
                    value = if (session.preconditionsReady) stringResource(R.string.value_yes) else stringResource(R.string.value_no),
                    label = stringResource(R.string.performance_metric_prerequisites),
                    severity = if (session.preconditionsReady) {
                        OperationalSeverity.SUCCESS
                    } else {
                        OperationalSeverity.WARNING
                    }
                )
            )
        )
        Text(
            text = stringResource(R.string.label_updated_at, formatPerformanceEpoch(session.updatedAtEpochMillis)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AdvancedDisclosureButton(
            expanded = showStatusControls,
            onToggle = { showStatusControls = !showStatusControls },
            showLabel = stringResource(R.string.performance_action_show_status_controls),
            hideLabel = stringResource(R.string.performance_action_hide_status_controls)
        )
        if (showStatusControls) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PerformanceSessionStatus.entries.forEach { status ->
                    OutlinedButton(onClick = { onSessionStatusSelected(status) }) {
                        Text(stringResource(performanceSessionStatusLabelRes(status)))
                    }
                }
            }
        }
    }
}

@Composable
fun PreflightReadinessCard(
    networkReady: Boolean,
    batterySufficient: Boolean,
    locationReady: Boolean,
    observedNetworkStatus: NetworkStatus?,
    observedBatteryLevelPercent: Int?,
    observedBatteryIsCharging: Boolean?,
    observedBatterySufficient: Boolean?,
    observedLocationAvailable: Boolean?,
    observedSignalsCapturedAtEpochMillis: Long?,
    isRefreshing: Boolean,
    onRefreshDeviceDiagnosticsClicked: () -> Unit,
    onApplyDeviceDiagnosticsClicked: () -> Unit,
    onPrerequisiteNetworkChanged: (Boolean) -> Unit,
    onPrerequisiteBatteryChanged: (Boolean) -> Unit,
    onPrerequisiteLocationChanged: (Boolean) -> Unit
) {
    var showControls by rememberSaveable { mutableStateOf(false) }
    val readyCount = listOf(networkReady, batterySufficient, locationReady).count { it }
    OperationalSectionCard(
        title = stringResource(R.string.performance_section_preflight_title),
        subtitle = stringResource(R.string.performance_section_preflight_hint)
    ) {
        OperationalSignalRow(
            signals = listOf(
                readinessSignal(
                    label = stringResource(R.string.performance_prerequisite_network_ready),
                    ready = networkReady
                ),
                readinessSignal(
                    label = stringResource(R.string.performance_prerequisite_battery_ok),
                    ready = batterySufficient
                ),
                readinessSignal(
                    label = stringResource(R.string.performance_prerequisite_location_ready),
                    ready = locationReady
                )
            )
        )
        OperationalMetricRow(
            metrics = listOf(
                OperationalMetric(
                    value = "$readyCount/3",
                    label = stringResource(R.string.performance_metric_preflight_ready),
                    severity = if (readyCount == 3) OperationalSeverity.SUCCESS else OperationalSeverity.WARNING
                ),
                OperationalMetric(
                    value = observedBatteryLevelPercent?.let { "$it%" } ?: stringResource(R.string.value_not_available),
                    label = stringResource(R.string.performance_metric_battery),
                    severity = when (observedBatterySufficient) {
                        true -> OperationalSeverity.SUCCESS
                        false -> OperationalSeverity.WARNING
                        null -> OperationalSeverity.NORMAL
                    }
                ),
                OperationalMetric(
                    value = when (observedLocationAvailable) {
                        true -> stringResource(R.string.value_yes)
                        false -> stringResource(R.string.value_no)
                        null -> stringResource(R.string.value_not_available)
                    },
                    label = stringResource(R.string.performance_metric_gps),
                    severity = when (observedLocationAvailable) {
                        true -> OperationalSeverity.SUCCESS
                        false -> OperationalSeverity.WARNING
                        null -> OperationalSeverity.NORMAL
                    }
                )
            )
        )
        Text(
            text = stringResource(
                R.string.performance_label_device_network_status,
                stringResource(networkStatusLabelRes(observedNetworkStatus))
            ),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(
                R.string.performance_label_device_battery_status,
                observedBatteryLevelPercent?.toString() ?: stringResource(R.string.value_not_available),
                when (observedBatterySufficient) {
                    true -> stringResource(R.string.value_yes)
                    false -> stringResource(R.string.value_no)
                    null -> stringResource(R.string.value_not_available)
                },
                when (observedBatteryIsCharging) {
                    true -> stringResource(R.string.value_yes)
                    false -> stringResource(R.string.value_no)
                    null -> stringResource(R.string.value_not_available)
                }
            ),
            style = MaterialTheme.typography.bodySmall
        )
        observedSignalsCapturedAtEpochMillis?.let { capturedAt ->
            Text(
                text = stringResource(R.string.label_updated_at, formatPerformanceEpoch(capturedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                enabled = !isRefreshing,
                onClick = onRefreshDeviceDiagnosticsClicked
            ) {
                Text(
                    if (isRefreshing) {
                        stringResource(R.string.performance_action_device_diagnostics_refresh_loading)
                    } else {
                        stringResource(R.string.performance_action_device_diagnostics_refresh)
                    }
                )
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onApplyDeviceDiagnosticsClicked
            ) {
                Text(stringResource(R.string.performance_action_device_diagnostics_apply))
            }
        }
        AdvancedDisclosureButton(
            expanded = showControls,
            onToggle = { showControls = !showControls },
            showLabel = stringResource(R.string.performance_action_show_preflight_controls),
            hideLabel = stringResource(R.string.performance_action_hide_preflight_controls)
        )
        if (showControls) {
            ToggleRow(
                label = stringResource(R.string.performance_prerequisite_network_ready),
                value = networkReady,
                onChange = onPrerequisiteNetworkChanged
            )
            ToggleRow(
                label = stringResource(R.string.performance_prerequisite_battery_ok),
                value = batterySufficient,
                onChange = onPrerequisiteBatteryChanged
            )
            ToggleRow(
                label = stringResource(R.string.performance_prerequisite_location_ready),
                value = locationReady,
                onChange = onPrerequisiteLocationChanged
            )
        }
    }
}

@Composable
fun PerformanceSupportSectionsCard(
    sessionHistory: List<PerformanceSession>,
    selectedSessionId: String?,
    steps: List<PerformanceGuidedStep>,
    onSelectHistorySessionClicked: (String) -> Unit,
    onStepStatusSelected: (PerformanceStepCode, PerformanceStepStatus) -> Unit
) {
    var showChecklist by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    OperationalSectionCard(
        title = stringResource(R.string.performance_section_advanced_runtime),
        subtitle = stringResource(R.string.performance_section_advanced_runtime_hint)
    ) {
        AdvancedDisclosureButton(
            expanded = showChecklist,
            onToggle = { showChecklist = !showChecklist },
            showLabel = stringResource(R.string.performance_action_show_checklist),
            hideLabel = stringResource(R.string.performance_action_hide_checklist)
        )
        if (showChecklist) {
            steps.forEach { step ->
                PerformanceStepCard(step = step, onStepStatusSelected = onStepStatusSelected)
            }
        }
        AdvancedDisclosureButton(
            expanded = showHistory,
            onToggle = { showHistory = !showHistory },
            showLabel = stringResource(R.string.performance_action_show_history, sessionHistory.size),
            hideLabel = stringResource(R.string.performance_action_hide_history, sessionHistory.size)
        )
        if (showHistory) {
            if (sessionHistory.isEmpty()) {
                OperationalEmptyStateCard(
                    title = stringResource(R.string.performance_header_session_history, 0),
                    message = stringResource(R.string.performance_empty_session)
                )
            } else {
                sessionHistory.forEach { historySession ->
                    PerformanceHistoryCard(
                        session = historySession,
                        isSelected = historySession.id == selectedSessionId,
                        onSelect = onSelectHistorySessionClicked
                    )
                }
            }
        }
    }
}

@Composable
fun ThroughputMissionCard(
    download: String,
    upload: String,
    latency: String,
    minDownload: String,
    minUpload: String,
    maxLatency: String,
    onThroughputDownloadChanged: (String) -> Unit,
    onThroughputUploadChanged: (String) -> Unit,
    onThroughputLatencyChanged: (String) -> Unit,
    onThroughputMinDownloadChanged: (String) -> Unit,
    onThroughputMinUploadChanged: (String) -> Unit,
    onThroughputMaxLatencyChanged: (String) -> Unit
) {
    OperationalSectionCard(
        title = stringResource(R.string.performance_header_throughput_result),
        subtitle = stringResource(R.string.performance_section_throughput_hint)
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = download,
            onValueChange = onThroughputDownloadChanged,
            label = { Text(stringResource(R.string.performance_input_download_mbps)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = upload,
            onValueChange = onThroughputUploadChanged,
            label = { Text(stringResource(R.string.performance_input_upload_mbps)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = latency,
            onValueChange = onThroughputLatencyChanged,
            label = { Text(stringResource(R.string.performance_input_latency_ms)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Text(
            text = stringResource(R.string.performance_header_thresholds),
            style = MaterialTheme.typography.titleSmall
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = minDownload,
            onValueChange = onThroughputMinDownloadChanged,
            label = { Text(stringResource(R.string.performance_input_min_download_mbps)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = minUpload,
            onValueChange = onThroughputMinUploadChanged,
            label = { Text(stringResource(R.string.performance_input_min_upload_mbps)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = maxLatency,
            onValueChange = onThroughputMaxLatencyChanged,
            label = { Text(stringResource(R.string.performance_input_max_latency_ms)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
fun QosMissionOverviewCard(
    selectedScriptName: String?,
    selectedTestFamilies: Set<QosTestFamily>,
    configuredRepeat: String,
    configuredTechnologies: Set<String>,
    scriptSnapshotUpdatedAtEpochMillis: Long?,
    executionSnapshot: QosExecutionSnapshot?
) {
    val sortedFamilies = selectedTestFamilies.sortedBy { it.name }
    val selectedFamilyLabels = if (sortedFamilies.isEmpty()) {
        stringResource(R.string.value_not_available)
    } else {
        sortedFamilies
            .map { family -> stringResource(qosTestFamilyLabelRes(family)) }
            .joinToString(", ")
    }
    OperationalSectionCard(
        title = stringResource(R.string.performance_header_qos_script),
        subtitle = stringResource(R.string.performance_section_qos_overview_hint)
    ) {
        OperationalSignalRow(
            signals = listOf(
                OperationalSignal(
                    text = stringResource(
                        R.string.performance_label_selected_qos_script,
                        selectedScriptName ?: stringResource(R.string.value_not_available)
                    ),
                    severity = if (selectedScriptName.isNullOrBlank()) OperationalSeverity.WARNING else OperationalSeverity.SUCCESS
                ),
                OperationalSignal(
                    text = stringResource(
                        R.string.performance_label_qos_engine_state,
                        executionSnapshot?.engineState?.let { stringResource(qosExecutionEngineStateLabelRes(it)) }
                            ?: stringResource(R.string.performance_qos_engine_state_ready)
                    ),
                    severity = executionSnapshot?.engineState?.toSeverity() ?: OperationalSeverity.NORMAL
                )
            )
        )
        OperationalMetricRow(
            metrics = buildList {
                add(
                    OperationalMetric(
                        value = configuredRepeat.ifBlank { "1" },
                        label = stringResource(R.string.performance_metric_repeat_count)
                    )
                )
                add(
                    OperationalMetric(
                        value = selectedTestFamilies.size.toString(),
                        label = stringResource(R.string.performance_metric_family_count)
                    )
                )
                executionSnapshot?.recoveryState?.let { recoveryState ->
                    add(
                        OperationalMetric(
                            value = stringResource(qosRecoveryStateLabelRes(recoveryState)),
                            label = stringResource(R.string.performance_metric_recovery),
                            severity = when (recoveryState) {
                                QosRecoveryState.NONE -> OperationalSeverity.NORMAL
                                QosRecoveryState.RESUME_AVAILABLE -> OperationalSeverity.WARNING
                                QosRecoveryState.INVARIANT_BROKEN -> OperationalSeverity.CRITICAL
                            }
                        )
                    )
                }
            }
        )
        Text(
            text = stringResource(
                R.string.performance_label_qos_selected_test_families,
                selectedFamilyLabels
            ),
            style = MaterialTheme.typography.bodySmall
        )
        if (configuredTechnologies.isNotEmpty()) {
            Text(
                text = stringResource(
                    R.string.performance_label_qos_configured_technologies,
                    configuredTechnologies.sorted().joinToString(", ")
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
        executionSnapshot?.nextFamily?.let { nextFamily ->
            Text(
                text = stringResource(
                    R.string.performance_label_qos_engine_next_run,
                    stringResource(qosTestFamilyLabelRes(nextFamily)),
                    executionSnapshot.nextRepetitionIndex ?: 1
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
        scriptSnapshotUpdatedAtEpochMillis?.let { snapshotAt ->
            Text(
                text = stringResource(
                    R.string.performance_label_qos_script_snapshot_at,
                    formatPerformanceEpoch(snapshotAt)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QosPreflightReadinessCard(
    selectedTestFamilies: Set<QosTestFamily>,
    runCoverageByType: Map<QosTestFamily, QosFamilyRunCoverage>,
    executionSnapshot: QosExecutionSnapshot?,
    activeFamilyPreflightIssues: Set<QosPreflightIssue>,
    completionIssues: Set<QosCompletionIssue>,
    showCompletionIssues: Boolean
) {
    val ready = !showCompletionIssues && activeFamilyPreflightIssues.isEmpty()
    OperationalSectionCard(
        title = stringResource(R.string.performance_header_prerequisites),
        subtitle = stringResource(R.string.performance_section_qos_preflight_hint)
    ) {
        OperationalMetricRow(
            metrics = listOf(
                OperationalMetric(
                    value = selectedTestFamilies.size.toString(),
                    label = stringResource(R.string.performance_metric_family_count)
                ),
                OperationalMetric(
                    value = runCoverageByType.values.count { coverage -> coverage.passFailTerminalCount >= coverage.requiredRepetitions }.toString(),
                    label = stringResource(R.string.performance_metric_completed_families),
                    severity = if (runCoverageByType.isNotEmpty()) OperationalSeverity.SUCCESS else OperationalSeverity.NORMAL
                ),
                OperationalMetric(
                    value = executionSnapshot?.checkpointCount?.toString() ?: "0",
                    label = stringResource(R.string.performance_metric_checkpoints)
                )
            )
        )
        if (showCompletionIssues && completionIssues.isNotEmpty()) {
            completionIssues.toList().sortedBy { it.name }.forEach { issue ->
                Text(
                    text = stringResource(
                        R.string.performance_label_qos_preflight_issue,
                        stringResource(qosCompletionIssueLabelRes(issue))
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else if (ready) {
            Text(
                text = stringResource(R.string.performance_label_qos_preflight_ok),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            activeFamilyPreflightIssues.toList().sortedBy { it.name }.forEach { issue ->
                Text(
                    text = stringResource(
                        R.string.performance_label_qos_family_preflight_issue,
                        stringResource(qosPreflightIssueLabelRes(issue))
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun QosActiveRunCard(
    activeFamily: QosTestFamily?,
    activeFamilyStatus: QosFamilyExecutionStatus,
    activeCoverage: QosFamilyRunCoverage?,
    executionSnapshot: QosExecutionSnapshot?,
    onQosRunnerStartClicked: (QosTestFamily) -> Unit,
    onQosRunnerPauseClicked: (QosTestFamily) -> Unit,
    onQosRunnerResumeClicked: (QosTestFamily) -> Unit,
    onQosRunnerPassClicked: (QosTestFamily) -> Unit,
    onQosRunnerFailClicked: (QosTestFamily) -> Unit,
    onQosRunnerBlockClicked: (QosTestFamily) -> Unit
) {
    OperationalSectionCard(
        title = stringResource(R.string.performance_header_qos_active_run),
        subtitle = stringResource(R.string.performance_section_qos_active_run_hint)
    ) {
        if (activeFamily == null) {
            OperationalEmptyStateCard(
                title = stringResource(R.string.performance_label_qos_no_active_family),
                message = stringResource(R.string.performance_empty_qos_active_family)
            )
            return@OperationalSectionCard
        }
        OperationalSignalRow(
            signals = listOf(
                OperationalSignal(
                    text = stringResource(
                        R.string.performance_label_qos_family_status,
                        stringResource(qosTestFamilyLabelRes(activeFamily)),
                        stringResource(qosFamilyExecutionStatusLabelRes(activeFamilyStatus))
                    ),
                    severity = activeFamilyStatus.toSeverity()
                ),
                OperationalSignal(
                    text = stringResource(
                        R.string.performance_label_qos_engine_state,
                        executionSnapshot?.engineState?.let { stringResource(qosExecutionEngineStateLabelRes(it)) }
                            ?: stringResource(R.string.performance_qos_engine_state_ready)
                    ),
                    severity = executionSnapshot?.engineState?.toSeverity() ?: OperationalSeverity.NORMAL
                )
            )
        )
        activeCoverage?.let { coverage ->
            OperationalMetricRow(
                metrics = listOf(
                    OperationalMetric(
                        value = "${coverage.passFailTerminalCount}/${coverage.requiredRepetitions}",
                        label = stringResource(R.string.performance_metric_repeat_coverage),
                        severity = if (coverage.passFailTerminalCount >= coverage.requiredRepetitions) {
                            OperationalSeverity.SUCCESS
                        } else {
                            OperationalSeverity.WARNING
                        }
                    ),
                    OperationalMetric(
                        value = coverage.startedCount.toString(),
                        label = stringResource(R.string.performance_metric_started_runs)
                    ),
                    OperationalMetric(
                        value = coverage.blockedCount.toString(),
                        label = stringResource(R.string.performance_metric_blocked_runs),
                        severity = if (coverage.blockedCount > 0) OperationalSeverity.WARNING else OperationalSeverity.NORMAL
                    )
                )
            )
            Text(
                text = stringResource(
                    R.string.performance_label_qos_family_runs,
                    coverage.passFailTerminalCount,
                    coverage.requiredRepetitions,
                    coverage.startedCount,
                    coverage.blockedCount,
                    coverage.activeRepetitionIndex?.toString() ?: stringResource(R.string.value_not_available)
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
        when (executionSnapshot?.engineState) {
            QosExecutionEngineState.RUNNING, QosExecutionEngineState.RESUMED -> {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onQosRunnerPauseClicked(activeFamily) }
                ) {
                    Text(stringResource(R.string.performance_action_qos_runner_pause))
                }
            }
            QosExecutionEngineState.PAUSED -> {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onQosRunnerResumeClicked(activeFamily) }
                ) {
                    Text(stringResource(R.string.performance_action_qos_runner_resume))
                }
            }
            else -> {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onQosRunnerStartClicked(activeFamily) }
                ) {
                    Text(stringResource(R.string.performance_action_qos_runner_start))
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { onQosRunnerPassClicked(activeFamily) }
            ) {
                Text(stringResource(R.string.performance_action_qos_runner_pass))
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { onQosRunnerFailClicked(activeFamily) }
            ) {
                Text(stringResource(R.string.performance_action_qos_runner_fail))
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { onQosRunnerBlockClicked(activeFamily) }
            ) {
                Text(stringResource(R.string.performance_action_qos_runner_block))
            }
        }
    }
}

@Composable
fun QosOutcomeCaptureCard(
    configuredRepeat: String,
    configuredTechnologies: Set<String>,
    targetTechnology: String,
    targetPhone: String,
    iterationCount: String,
    successCount: String,
    failureCount: String,
    activeFamily: QosTestFamily?,
    activeFamilyStatus: QosFamilyExecutionStatus,
    familyReasonCodeByType: Map<QosTestFamily, QosExecutionIssueCode?>,
    familyFailureReasonByType: Map<QosTestFamily, String>,
    onQosConfiguredRepeatChanged: (String) -> Unit,
    onQosTargetTechnologyChanged: (String) -> Unit,
    onQosTargetPhoneChanged: (String) -> Unit,
    onQosIterationCountChanged: (String) -> Unit,
    onQosSuccessCountChanged: (String) -> Unit,
    onQosFailureCountChanged: (String) -> Unit,
    onQosFamilyReasonCodeChanged: (QosTestFamily, QosExecutionIssueCode?) -> Unit,
    onQosFamilyFailureReasonChanged: (QosTestFamily, String) -> Unit
) {
    OperationalSectionCard(
        title = stringResource(R.string.performance_header_qos_outcome_capture),
        subtitle = stringResource(R.string.performance_section_qos_outcome_capture_hint)
    ) {
        OperationalMetricRow(
            metrics = listOf(
                OperationalMetric(
                    value = iterationCount.ifBlank { "0" },
                    label = stringResource(R.string.performance_metric_iterations)
                ),
                OperationalMetric(
                    value = successCount.ifBlank { "0" },
                    label = stringResource(R.string.performance_metric_successes),
                    severity = if (successCount.toIntOrNull().orZero() > 0) OperationalSeverity.SUCCESS else OperationalSeverity.NORMAL
                ),
                OperationalMetric(
                    value = failureCount.ifBlank { "0" },
                    label = stringResource(R.string.performance_metric_failures),
                    severity = if (failureCount.toIntOrNull().orZero() > 0) OperationalSeverity.WARNING else OperationalSeverity.NORMAL
                )
            )
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = configuredRepeat,
            onValueChange = onQosConfiguredRepeatChanged,
            label = { Text(stringResource(R.string.performance_input_qos_configured_repeat_count)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = targetTechnology,
            onValueChange = onQosTargetTechnologyChanged,
            label = { Text(stringResource(R.string.performance_input_qos_target_technology)) }
        )
        if (configuredTechnologies.isNotEmpty() && targetTechnology.isNotBlank() && targetTechnology !in configuredTechnologies) {
            Text(
                text = stringResource(R.string.performance_error_qos_target_technology_mismatch_inline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = targetPhone,
            onValueChange = onQosTargetPhoneChanged,
            label = { Text(stringResource(R.string.performance_input_qos_target_phone)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = iterationCount,
            onValueChange = onQosIterationCountChanged,
            label = { Text(stringResource(R.string.performance_input_qos_iteration_count)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            readOnly = true
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = successCount,
            onValueChange = onQosSuccessCountChanged,
            label = { Text(stringResource(R.string.performance_input_qos_success_count)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            readOnly = true
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = failureCount,
            onValueChange = onQosFailureCountChanged,
            label = { Text(stringResource(R.string.performance_input_qos_failure_count)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            readOnly = true
        )

        if (activeFamily != null && (activeFamilyStatus == QosFamilyExecutionStatus.FAILED || activeFamilyStatus == QosFamilyExecutionStatus.BLOCKED)) {
            Text(
                text = stringResource(
                    R.string.performance_label_qos_family_reason_code,
                    familyReasonCodeByType[activeFamily]?.let { code ->
                        stringResource(qosIssueCodeLabelRes(code))
                    } ?: stringResource(R.string.value_not_available)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            qosReasonOptionsForFamily(activeFamily).forEach { code ->
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onQosFamilyReasonCodeChanged(activeFamily, code) }
                ) {
                    Text(
                        stringResource(
                            R.string.performance_action_qos_reason_code_select,
                            stringResource(qosIssueCodeLabelRes(code))
                        )
                    )
                }
            }
            familyReasonCodeByType[activeFamily]?.let { code ->
                Text(
                    text = stringResource(
                        R.string.performance_label_qos_family_reason_action,
                        stringResource(qosIssueCodeActionRes(code))
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = familyFailureReasonByType[activeFamily].orEmpty(),
                onValueChange = { value -> onQosFamilyFailureReasonChanged(activeFamily, value) },
                label = { Text(stringResource(R.string.performance_input_qos_family_failure_reason)) }
            )
        }
    }
}

@Composable
fun QosAdvancedSectionsCard(
    selectedScriptId: String?,
    availableScripts: List<QosScriptDefinition>,
    runPlan: List<QosRunPlanItem>,
    timelineEvents: List<QosExecutionTimelineEvent>,
    scriptEditorName: String,
    scriptEditorRepeat: String,
    scriptEditorTechnologies: String,
    scriptEditorSelectedFamilies: Set<QosTestFamily>,
    isSavingScript: Boolean,
    sortedFamilies: List<QosTestFamily>,
    familyStatusByType: Map<QosTestFamily, QosFamilyExecutionStatus>,
    runCoverageByType: Map<QosTestFamily, QosFamilyRunCoverage>,
    onQosScriptSelected: (String, String) -> Unit,
    onQosScriptEditorNameChanged: (String) -> Unit,
    onQosScriptEditorRepeatChanged: (String) -> Unit,
    onQosScriptEditorTechnologiesChanged: (String) -> Unit,
    onQosScriptEditorFamilyToggled: (QosTestFamily) -> Unit,
    onSaveQosScriptClicked: () -> Unit
) {
    var showScriptSelection by rememberSaveable { mutableStateOf(false) }
    var showRunPlan by rememberSaveable { mutableStateOf(false) }
    var showTimeline by rememberSaveable { mutableStateOf(false) }
    var showScriptEditor by rememberSaveable { mutableStateOf(false) }
    var showFamilyMatrix by rememberSaveable { mutableStateOf(false) }

    OperationalSectionCard(
        title = stringResource(R.string.performance_section_advanced_runtime_qos_title),
        subtitle = stringResource(R.string.performance_section_advanced_runtime_qos_hint)
    ) {
        AdvancedDisclosureButton(
            expanded = showScriptSelection,
            onToggle = { showScriptSelection = !showScriptSelection },
            showLabel = stringResource(R.string.performance_action_show_qos_script_selection),
            hideLabel = stringResource(R.string.performance_action_hide_qos_script_selection)
        )
        if (showScriptSelection) {
            availableScripts.forEach { script ->
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = script.id != selectedScriptId,
                    onClick = { onQosScriptSelected(script.id, script.name) }
                ) {
                    Text(
                        stringResource(
                            R.string.performance_label_qos_script_item,
                            script.name,
                            script.repeatCount,
                            script.testFamilies.size
                        )
                    )
                }
            }
        }

        if (runPlan.isNotEmpty()) {
            AdvancedDisclosureButton(
                expanded = showRunPlan,
                onToggle = { showRunPlan = !showRunPlan },
                showLabel = stringResource(R.string.performance_action_show_qos_run_plan),
                hideLabel = stringResource(R.string.performance_action_hide_qos_run_plan)
            )
            if (showRunPlan) {
                runPlan.take(10).forEach { run ->
                    Text(
                        text = stringResource(
                            R.string.performance_label_qos_run_plan_line,
                            stringResource(qosTestFamilyLabelRes(run.family)),
                            run.repetitionIndex,
                            stringResource(qosRunPlanStatusLabelRes(run.status))
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        AdvancedDisclosureButton(
            expanded = showTimeline,
            onToggle = { showTimeline = !showTimeline },
            showLabel = stringResource(R.string.performance_action_show_qos_timeline),
            hideLabel = stringResource(R.string.performance_action_hide_qos_timeline)
        )
        if (showTimeline) {
            if (timelineEvents.isEmpty()) {
                Text(
                    text = stringResource(R.string.performance_label_qos_timeline_empty),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                timelineEvents.sortedWith(
                    compareByDescending<QosExecutionTimelineEvent> { event -> event.checkpointSequence }
                        .thenBy { event -> event.family.name }
                        .thenBy { event -> event.repetitionIndex }
                        .thenBy { event -> event.occurredAtEpochMillis }
                        .thenBy { event -> com.quartz.platform.domain.model.qosExecutionEventSortOrder(event.eventType) }
                ).take(8).forEach { event ->
                    val line = stringResource(
                        R.string.performance_label_qos_timeline_line,
                        formatPerformanceEpoch(event.occurredAtEpochMillis),
                        stringResource(qosTestFamilyLabelRes(event.family)),
                        event.repetitionIndex,
                        stringResource(qosExecutionEventTypeLabelRes(event.eventType))
                    )
                    Text(
                        text = event.reason?.takeIf { reason -> reason.isNotBlank() }?.let { reason ->
                            "$line (${stringResource(R.string.label_failure_reason, reason)})"
                        } ?: line,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        AdvancedDisclosureButton(
            expanded = showScriptEditor,
            onToggle = { showScriptEditor = !showScriptEditor },
            showLabel = stringResource(R.string.performance_action_show_qos_script_editor),
            hideLabel = stringResource(R.string.performance_action_hide_qos_script_editor)
        )
        if (showScriptEditor) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = scriptEditorName,
                onValueChange = onQosScriptEditorNameChanged,
                label = { Text(stringResource(R.string.performance_input_qos_script_name)) }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = scriptEditorRepeat,
                onValueChange = onQosScriptEditorRepeatChanged,
                label = { Text(stringResource(R.string.performance_input_qos_script_repeat_count)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = scriptEditorTechnologies,
                onValueChange = onQosScriptEditorTechnologiesChanged,
                label = { Text(stringResource(R.string.performance_input_qos_script_technologies)) }
            )
            QosTestFamily.entries.forEach { family ->
                ToggleRow(
                    label = stringResource(qosTestFamilyLabelRes(family)),
                    value = scriptEditorSelectedFamilies.contains(family),
                    onChange = { onQosScriptEditorFamilyToggled(family) }
                )
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSavingScript,
                onClick = onSaveQosScriptClicked
            ) {
                Text(
                    if (isSavingScript) {
                        stringResource(R.string.performance_action_save_loading)
                    } else {
                        stringResource(R.string.performance_action_save_qos_script)
                    }
                )
            }
        }

        AdvancedDisclosureButton(
            expanded = showFamilyMatrix,
            onToggle = { showFamilyMatrix = !showFamilyMatrix },
            showLabel = stringResource(R.string.performance_action_show_qos_family_matrix),
            hideLabel = stringResource(R.string.performance_action_hide_qos_family_matrix)
        )
        if (showFamilyMatrix) {
            if (sortedFamilies.isEmpty()) {
                Text(
                    text = stringResource(R.string.performance_empty_qos_family_matrix),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                sortedFamilies.forEach { family ->
                    val familyStatus = familyStatusByType[family] ?: QosFamilyExecutionStatus.NOT_RUN
                    val coverage = runCoverageByType[family]
                    Text(
                        text = stringResource(
                            R.string.performance_label_qos_family_status,
                            stringResource(qosTestFamilyLabelRes(family)),
                            stringResource(qosFamilyExecutionStatusLabelRes(familyStatus))
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    coverage?.let {
                        Text(
                            text = stringResource(
                                R.string.performance_label_qos_family_runs,
                                it.passFailTerminalCount,
                                it.requiredRepetitions,
                                it.startedCount,
                                it.blockedCount,
                                it.activeRepetitionIndex?.toString() ?: stringResource(R.string.value_not_available)
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewCaptureCard(
    resultSummaryInput: String,
    notesInput: String,
    onResultSummaryChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit
) {
    OperationalSectionCard(
        title = stringResource(R.string.performance_section_review_capture),
        subtitle = stringResource(R.string.performance_section_review_capture_hint)
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = resultSummaryInput,
            onValueChange = onResultSummaryChanged,
            label = { Text(stringResource(R.string.performance_input_result_summary)) }
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = notesInput,
            onValueChange = onNotesChanged,
            label = { Text(stringResource(R.string.performance_input_notes)) }
        )
    }
}

@Composable
private fun SelectionButtonRow(
    values: List<String>,
    onSelected: (String?) -> Unit
) {
    if (values.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        values.forEach { value ->
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { onSelected(value) }
            ) {
                Text(value)
            }
        }
    }
}

@Composable
private fun PerformanceHistoryCard(
    session: PerformanceSession,
    isSelected: Boolean,
    onSelect: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(performanceWorkflowTypeLabelRes(session.workflowType)),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(
                    R.string.performance_label_session_status,
                    stringResource(performanceSessionStatusLabelRes(session.status))
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.label_updated_at, formatPerformanceEpoch(session.updatedAtEpochMillis)),
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSelected,
                onClick = { onSelect(session.id) }
            ) {
                Text(
                    if (isSelected) {
                        stringResource(R.string.performance_action_session_selected)
                    } else {
                        stringResource(R.string.performance_action_open_session)
                    }
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    value: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { onChange(!value) }) {
            Text(if (value) stringResource(R.string.value_yes) else stringResource(R.string.value_no))
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun PerformanceStepCard(
    step: PerformanceGuidedStep,
    onStepStatusSelected: (PerformanceStepCode, PerformanceStepStatus) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(performanceStepCodeLabelRes(step.code)),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.performance_label_step_status,
                    stringResource(performanceStepStatusLabelRes(step.status))
                ),
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PerformanceStepStatus.entries.forEach { status ->
                    OutlinedButton(onClick = { onStepStatusSelected(step.code, status) }) {
                        Text(stringResource(performanceStepStatusLabelRes(status)))
                    }
                }
            }
        }
    }
}

private fun readinessSignal(label: String, ready: Boolean): OperationalSignal {
    return OperationalSignal(
        text = label,
        severity = if (ready) OperationalSeverity.SUCCESS else OperationalSeverity.WARNING
    )
}

private fun sessionStatusSeverity(status: PerformanceSessionStatus): OperationalSeverity {
    return when (status) {
        PerformanceSessionStatus.CREATED -> OperationalSeverity.NORMAL
        PerformanceSessionStatus.IN_PROGRESS -> OperationalSeverity.WARNING
        PerformanceSessionStatus.COMPLETED -> OperationalSeverity.SUCCESS
        PerformanceSessionStatus.CANCELLED -> OperationalSeverity.CRITICAL
    }
}

private fun QosExecutionEngineState.toSeverity(): OperationalSeverity {
    return when (this) {
        QosExecutionEngineState.READY -> OperationalSeverity.NORMAL
        QosExecutionEngineState.PREFLIGHT_BLOCKED -> OperationalSeverity.WARNING
        QosExecutionEngineState.RUNNING,
        QosExecutionEngineState.RESUMED -> OperationalSeverity.SUCCESS
        QosExecutionEngineState.PAUSED -> OperationalSeverity.WARNING
        QosExecutionEngineState.COMPLETED -> OperationalSeverity.SUCCESS
        QosExecutionEngineState.FAILED,
        QosExecutionEngineState.BLOCKED -> OperationalSeverity.CRITICAL
    }
}

private fun QosFamilyExecutionStatus.toSeverity(): OperationalSeverity {
    return when (this) {
        QosFamilyExecutionStatus.NOT_RUN -> OperationalSeverity.NORMAL
        QosFamilyExecutionStatus.PASSED -> OperationalSeverity.SUCCESS
        QosFamilyExecutionStatus.FAILED -> OperationalSeverity.CRITICAL
        QosFamilyExecutionStatus.BLOCKED -> OperationalSeverity.WARNING
    }
}

private fun String?.toIntOrNullSafe(): Int? = this?.toIntOrNull()
private fun Int?.orZero(): Int = this ?: 0
