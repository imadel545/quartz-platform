package com.quartz.platform.presentation.performance.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quartz.platform.R
import com.quartz.platform.domain.model.LocalQosScriptCatalog
import com.quartz.platform.domain.model.PerformanceGuidedStep
import com.quartz.platform.domain.model.PerformanceSession
import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.PerformanceStepCode
import com.quartz.platform.domain.model.PerformanceStepStatus
import com.quartz.platform.domain.model.PerformanceWorkflowType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PerformanceSessionRoute(
    onBack: () -> Unit,
    viewModel: PerformanceSessionViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
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
        onQosTargetTechnologyChanged = viewModel::onQosTargetTechnologyChanged,
        onQosTargetPhoneChanged = viewModel::onQosTargetPhoneChanged,
        onQosIterationCountChanged = viewModel::onQosIterationCountChanged,
        onQosSuccessCountChanged = viewModel::onQosSuccessCountChanged,
        onQosFailureCountChanged = viewModel::onQosFailureCountChanged,
        onNotesChanged = viewModel::onNotesChanged,
        onResultSummaryChanged = viewModel::onResultSummaryChanged,
        onSaveSummaryClicked = viewModel::onSaveSummaryClicked
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
    onStepStatusSelected: (PerformanceStepCode, PerformanceStepStatus) -> Unit,
    onSessionStatusSelected: (PerformanceSessionStatus) -> Unit,
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
    onQosTargetTechnologyChanged: (String) -> Unit,
    onQosTargetPhoneChanged: (String) -> Unit,
    onQosIterationCountChanged: (String) -> Unit,
    onQosSuccessCountChanged: (String) -> Unit,
    onQosFailureCountChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onResultSummaryChanged: (String) -> Unit,
    onSaveSummaryClicked: () -> Unit
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
                            text = stringResource(R.string.performance_shell_disclaimer),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = stringResource(
                                R.string.performance_label_site,
                                state.siteLabel.ifBlank { state.siteId }
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

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

            item {
                Text(
                    text = stringResource(R.string.performance_header_session_history, state.sessionHistory.size),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (state.sessionHistory.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.performance_empty_session),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(state.sessionHistory, key = { it.id }) { session ->
                    PerformanceHistoryCard(
                        session = session,
                        isSelected = session.id == state.selectedSessionId,
                        onSelect = onSelectHistorySessionClicked
                    )
                }
            }

            state.session?.let { session ->
                item {
                    SessionSummaryCard(
                        session = session,
                        selectedStatus = state.selectedStatus,
                        onSessionStatusSelected = onSessionStatusSelected
                    )
                }

                item {
                    PrerequisitesCard(
                        networkReady = state.prerequisiteNetworkReady,
                        batterySufficient = state.prerequisiteBatterySufficient,
                        locationReady = state.prerequisiteLocationReady,
                        onPrerequisiteNetworkChanged = onPrerequisiteNetworkChanged,
                        onPrerequisiteBatteryChanged = onPrerequisiteBatteryChanged,
                        onPrerequisiteLocationChanged = onPrerequisiteLocationChanged
                    )
                }

                item {
                    Text(
                        text = stringResource(R.string.performance_header_checklist),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(session.steps, key = { it.code.name }) { step ->
                    PerformanceStepCard(step = step, onStepStatusSelected = onStepStatusSelected)
                }

                when (session.workflowType) {
                    PerformanceWorkflowType.THROUGHPUT -> {
                        item {
                            ThroughputPanel(
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
                        item {
                            QosPanel(
                                selectedScriptId = state.qosSelectedScriptId,
                                selectedScriptName = state.qosSelectedScriptName,
                                targetTechnology = state.qosTargetTechnologyInput,
                                targetPhone = state.qosTargetPhoneInput,
                                iterationCount = state.qosIterationCountInput,
                                successCount = state.qosSuccessCountInput,
                                failureCount = state.qosFailureCountInput,
                                onQosScriptSelected = onQosScriptSelected,
                                onQosTargetTechnologyChanged = onQosTargetTechnologyChanged,
                                onQosTargetPhoneChanged = onQosTargetPhoneChanged,
                                onQosIterationCountChanged = onQosIterationCountChanged,
                                onQosSuccessCountChanged = onQosSuccessCountChanged,
                                onQosFailureCountChanged = onQosFailureCountChanged
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.notesInput,
                        onValueChange = onNotesChanged,
                        label = { Text(stringResource(R.string.performance_input_notes)) }
                    )
                }

                item {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.resultSummaryInput,
                        onValueChange = onResultSummaryChanged,
                        label = { Text(stringResource(R.string.performance_input_result_summary)) }
                    )
                }

                state.completionGuardMessage?.let { guard ->
                    item {
                        Text(
                            text = guard,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            state.errorMessage?.let { error ->
                item {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            state.infoMessage?.let { info ->
                item {
                    Text(
                        text = info,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.session != null && state.hasUnsavedChanges && !state.isSavingSummary,
                    onClick = onSaveSummaryClicked
                ) {
                    Text(
                        if (state.isSavingSummary) {
                            stringResource(R.string.performance_action_save_loading)
                        } else {
                            stringResource(R.string.performance_action_save)
                        }
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

@Composable
private fun SessionCreationCard(
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.performance_header_create_session),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(
                    R.string.performance_label_selected_operator,
                    selectedOperator ?: stringResource(R.string.value_not_available)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                availableOperators.forEach { operator ->
                    Button(onClick = { onOperatorSelected(operator) }) {
                        Text(operator)
                    }
                }
            }

            Text(
                text = stringResource(
                    R.string.performance_label_selected_technology,
                    selectedTechnology ?: stringResource(R.string.value_not_available)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                availableTechnologies.forEach { technology ->
                    Button(onClick = { onTechnologySelected(technology) }) {
                        Text(technology)
                    }
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCreatingSession,
                onClick = onCreateThroughputSessionClicked
            ) {
                Text(stringResource(R.string.performance_action_create_throughput_session))
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCreatingSession,
                onClick = onCreateQosSessionClicked
            ) {
                Text(stringResource(R.string.performance_action_create_qos_session))
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
                text = stringResource(R.string.label_updated_at, formatEpoch(session.updatedAtEpochMillis)),
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
private fun SessionSummaryCard(
    session: PerformanceSession,
    selectedStatus: PerformanceSessionStatus,
    onSessionStatusSelected: (PerformanceSessionStatus) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.performance_label_session_type,
                    stringResource(performanceWorkflowTypeLabelRes(session.workflowType))
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.performance_label_session_status,
                    stringResource(performanceSessionStatusLabelRes(selectedStatus))
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerformanceSessionStatus.entries.forEach { status ->
                    Button(onClick = { onSessionStatusSelected(status) }) {
                        Text(stringResource(performanceSessionStatusLabelRes(status)))
                    }
                }
            }
        }
    }
}

@Composable
private fun PrerequisitesCard(
    networkReady: Boolean,
    batterySufficient: Boolean,
    locationReady: Boolean,
    onPrerequisiteNetworkChanged: (Boolean) -> Unit,
    onPrerequisiteBatteryChanged: (Boolean) -> Unit,
    onPrerequisiteLocationChanged: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.performance_header_prerequisites),
                style = MaterialTheme.typography.titleSmall
            )
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
        Button(onClick = { onChange(!value) }) {
            Text(
                if (value) stringResource(R.string.value_yes) else stringResource(R.string.value_no)
            )
        }
    }
}

@Composable
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerformanceStepStatus.entries.forEach { status ->
                    Button(onClick = { onStepStatusSelected(step.code, status) }) {
                        Text(stringResource(performanceStepStatusLabelRes(status)))
                    }
                }
            }
        }
    }
}

@Composable
private fun ThroughputPanel(
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.performance_header_throughput_result),
                style = MaterialTheme.typography.titleSmall
            )
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
}

@Composable
private fun QosPanel(
    selectedScriptId: String?,
    selectedScriptName: String?,
    targetTechnology: String,
    targetPhone: String,
    iterationCount: String,
    successCount: String,
    failureCount: String,
    onQosScriptSelected: (String, String) -> Unit,
    onQosTargetTechnologyChanged: (String) -> Unit,
    onQosTargetPhoneChanged: (String) -> Unit,
    onQosIterationCountChanged: (String) -> Unit,
    onQosSuccessCountChanged: (String) -> Unit,
    onQosFailureCountChanged: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.performance_header_qos_script),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(
                    R.string.performance_label_selected_qos_script,
                    selectedScriptName ?: stringResource(R.string.value_not_available)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            LocalQosScriptCatalog.defaults.forEach { script ->
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = script.id != selectedScriptId,
                    onClick = { onQosScriptSelected(script.id, script.name) }
                ) {
                    Text("${script.name} - ${script.description}")
                }
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = targetTechnology,
                onValueChange = onQosTargetTechnologyChanged,
                label = { Text(stringResource(R.string.performance_input_qos_target_technology)) }
            )
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = successCount,
                onValueChange = onQosSuccessCountChanged,
                label = { Text(stringResource(R.string.performance_input_qos_success_count)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = failureCount,
                onValueChange = onQosFailureCountChanged,
                label = { Text(stringResource(R.string.performance_input_qos_failure_count)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

private fun formatEpoch(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return formatter.format(
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    )
}
