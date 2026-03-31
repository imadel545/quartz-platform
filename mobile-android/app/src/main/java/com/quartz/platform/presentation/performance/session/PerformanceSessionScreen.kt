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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quartz.platform.R
import com.quartz.platform.domain.model.PerformanceGuidedStep
import com.quartz.platform.domain.model.PerformanceSession
import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.PerformanceStepCode
import com.quartz.platform.domain.model.PerformanceStepStatus
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.model.QosCompletionIssue
import com.quartz.platform.domain.model.QosExecutionEngineState
import com.quartz.platform.domain.model.QosRecoveryState
import com.quartz.platform.domain.model.QosExecutionSnapshot
import com.quartz.platform.domain.model.QosFamilyRunCoverage
import com.quartz.platform.domain.model.QosPreflightIssue
import com.quartz.platform.domain.model.QosExecutionEventType
import com.quartz.platform.domain.model.QosExecutionIssueCode
import com.quartz.platform.domain.model.QosExecutionTimelineEvent
import com.quartz.platform.domain.model.QosRunPlanItem
import com.quartz.platform.domain.model.QosRunPlanItemStatus
import com.quartz.platform.domain.model.QosScriptDefinition
import com.quartz.platform.domain.model.QosFamilyExecutionStatus
import com.quartz.platform.domain.model.QosTestFamily
import com.quartz.platform.domain.model.qosExecutionEventSortOrder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    onQosConfiguredRepeatChanged: (String) -> Unit,
    onQosScriptEditorNameChanged: (String) -> Unit,
    onQosScriptEditorRepeatChanged: (String) -> Unit,
    onQosScriptEditorTechnologiesChanged: (String) -> Unit,
    onQosScriptEditorFamilyToggled: (QosTestFamily) -> Unit,
    onSaveQosScriptClicked: () -> Unit,
    onQosRunnerStartClicked: (QosTestFamily) -> Unit,
    onQosRunnerPauseClicked: (QosTestFamily) -> Unit,
    onQosRunnerResumeClicked: (QosTestFamily) -> Unit,
    onQosRunnerPassClicked: (QosTestFamily) -> Unit,
    onQosRunnerFailClicked: (QosTestFamily) -> Unit,
    onQosRunnerBlockClicked: (QosTestFamily) -> Unit,
    onQosFamilyReasonCodeChanged: (QosTestFamily, QosExecutionIssueCode?) -> Unit,
    onQosFamilyFailureReasonChanged: (QosTestFamily, String) -> Unit,
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
                                selectedTestFamilies = state.qosSelectedTestFamilies,
                                configuredRepeat = state.qosConfiguredRepeatInput,
                                configuredTechnologies = state.qosConfiguredTechnologies,
                                scriptSnapshotUpdatedAtEpochMillis = state.qosScriptSnapshotUpdatedAtEpochMillis,
                                availableScripts = state.availableQosScripts,
                                scriptEditorName = state.qosScriptEditorNameInput,
                                scriptEditorRepeat = state.qosScriptEditorRepeatInput,
                                scriptEditorTechnologies = state.qosScriptEditorTechnologiesInput,
                                scriptEditorSelectedFamilies = state.qosScriptEditorSelectedFamilies,
                                isSavingScript = state.isSavingQosScript,
                                familyStatusByType = state.qosFamilyStatusByType,
                                familyReasonCodeByType = state.qosFamilyReasonCodeByType,
                                familyFailureReasonByType = state.qosFamilyFailureReasonByType,
                                runCoverageByType = state.qosFamilyRunCoverageByType,
                                runPlan = state.qosRunPlan,
                                executionSnapshot = state.qosExecutionSnapshot,
                                preflightIssuesByFamily = state.qosPreflightIssuesByFamily,
                                timelineEvents = state.qosExecutionTimelineEvents,
                                completionIssues = state.qosCompletionIssues,
                                showCompletionIssues = state.selectedStatus == PerformanceSessionStatus.COMPLETED,
                                targetTechnology = state.qosTargetTechnologyInput,
                                targetPhone = state.qosTargetPhoneInput,
                                iterationCount = state.qosIterationCountInput,
                                successCount = state.qosSuccessCountInput,
                                failureCount = state.qosFailureCountInput,
                                onQosScriptSelected = onQosScriptSelected,
                                onQosConfiguredRepeatChanged = onQosConfiguredRepeatChanged,
                                onQosScriptEditorNameChanged = onQosScriptEditorNameChanged,
                                onQosScriptEditorRepeatChanged = onQosScriptEditorRepeatChanged,
                                onQosScriptEditorTechnologiesChanged = onQosScriptEditorTechnologiesChanged,
                                onQosScriptEditorFamilyToggled = onQosScriptEditorFamilyToggled,
                                onSaveQosScriptClicked = onSaveQosScriptClicked,
                                onQosRunnerStartClicked = onQosRunnerStartClicked,
                                onQosRunnerPauseClicked = onQosRunnerPauseClicked,
                                onQosRunnerResumeClicked = onQosRunnerResumeClicked,
                                onQosRunnerPassClicked = onQosRunnerPassClicked,
                                onQosRunnerFailClicked = onQosRunnerFailClicked,
                                onQosRunnerBlockClicked = onQosRunnerBlockClicked,
                                onQosFamilyReasonCodeChanged = onQosFamilyReasonCodeChanged,
                                onQosFamilyFailureReasonChanged = onQosFamilyFailureReasonChanged,
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
                    enabled = state.session != null,
                    onClick = onOpenLinkedDraftClicked
                ) {
                    Text(stringResource(R.string.action_open_linked_report_draft))
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
    selectedTestFamilies: Set<QosTestFamily>,
    configuredRepeat: String,
    configuredTechnologies: Set<String>,
    scriptSnapshotUpdatedAtEpochMillis: Long?,
    availableScripts: List<QosScriptDefinition>,
    scriptEditorName: String,
    scriptEditorRepeat: String,
    scriptEditorTechnologies: String,
    scriptEditorSelectedFamilies: Set<QosTestFamily>,
    isSavingScript: Boolean,
    familyStatusByType: Map<QosTestFamily, QosFamilyExecutionStatus>,
    familyReasonCodeByType: Map<QosTestFamily, QosExecutionIssueCode?>,
    familyFailureReasonByType: Map<QosTestFamily, String>,
    runCoverageByType: Map<QosTestFamily, QosFamilyRunCoverage>,
    runPlan: List<QosRunPlanItem>,
    executionSnapshot: QosExecutionSnapshot?,
    preflightIssuesByFamily: Map<QosTestFamily, Set<QosPreflightIssue>>,
    timelineEvents: List<QosExecutionTimelineEvent>,
    completionIssues: Set<QosCompletionIssue>,
    showCompletionIssues: Boolean,
    targetTechnology: String,
    targetPhone: String,
    iterationCount: String,
    successCount: String,
    failureCount: String,
    onQosScriptSelected: (String, String) -> Unit,
    onQosConfiguredRepeatChanged: (String) -> Unit,
    onQosScriptEditorNameChanged: (String) -> Unit,
    onQosScriptEditorRepeatChanged: (String) -> Unit,
    onQosScriptEditorTechnologiesChanged: (String) -> Unit,
    onQosScriptEditorFamilyToggled: (QosTestFamily) -> Unit,
    onSaveQosScriptClicked: () -> Unit,
    onQosRunnerStartClicked: (QosTestFamily) -> Unit,
    onQosRunnerPauseClicked: (QosTestFamily) -> Unit,
    onQosRunnerResumeClicked: (QosTestFamily) -> Unit,
    onQosRunnerPassClicked: (QosTestFamily) -> Unit,
    onQosRunnerFailClicked: (QosTestFamily) -> Unit,
    onQosRunnerBlockClicked: (QosTestFamily) -> Unit,
    onQosFamilyReasonCodeChanged: (QosTestFamily, QosExecutionIssueCode?) -> Unit,
    onQosFamilyFailureReasonChanged: (QosTestFamily, String) -> Unit,
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
            availableScripts.forEach { script ->
                Button(
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
            Text(
                text = stringResource(
                    R.string.performance_label_qos_selected_test_families,
                    buildList {
                        for (family in selectedTestFamilies.sortedBy { it.name }) {
                            add(stringResource(qosTestFamilyLabelRes(family)))
                        }
                    }.joinToString(", ").ifBlank { stringResource(R.string.value_not_available) }
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
            scriptSnapshotUpdatedAtEpochMillis?.let { snapshotAt ->
                Text(
                    text = stringResource(
                        R.string.performance_label_qos_script_snapshot_at,
                        formatEpoch(snapshotAt)
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = stringResource(R.string.performance_header_qos_family_execution),
                style = MaterialTheme.typography.titleSmall
            )
            executionSnapshot?.let { snapshot ->
                Text(
                    text = stringResource(
                        R.string.performance_label_qos_engine_state,
                        stringResource(qosExecutionEngineStateLabelRes(snapshot.engineState))
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(
                        R.string.performance_label_qos_engine_progress,
                        snapshot.plannedRunCount,
                        snapshot.pendingRunCount,
                        snapshot.runningRunCount,
                        snapshot.pausedRunCount,
                        snapshot.failedRunCount,
                        snapshot.blockedRunCount
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                if (snapshot.recoveryState != QosRecoveryState.NONE) {
                    Text(
                        text = stringResource(
                            R.string.performance_label_qos_engine_recovery_state,
                            stringResource(qosRecoveryStateLabelRes(snapshot.recoveryState))
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                snapshot.activeFamily?.let { activeFamily ->
                    Text(
                        text = stringResource(
                            R.string.performance_label_qos_engine_active_run,
                            stringResource(qosTestFamilyLabelRes(activeFamily)),
                            snapshot.activeRepetitionIndex ?: 1
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                snapshot.nextFamily?.let { nextFamily ->
                    Text(
                        text = stringResource(
                            R.string.performance_label_qos_engine_next_run,
                            stringResource(qosTestFamilyLabelRes(nextFamily)),
                            snapshot.nextRepetitionIndex ?: 1
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (runPlan.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.performance_header_qos_run_plan),
                    style = MaterialTheme.typography.titleSmall
                )
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
                val hiddenRunCount = runPlan.size - 10
                if (hiddenRunCount > 0) {
                    Text(
                        text = stringResource(
                            R.string.performance_label_qos_run_plan_more,
                            hiddenRunCount
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (showCompletionIssues && completionIssues.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.performance_header_qos_preflight_issues),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error
                )
                completionIssues.toList().sortedBy { issue -> issue.name }.forEach { issue ->
                    Text(
                        text = stringResource(
                            R.string.performance_label_qos_preflight_issue,
                            stringResource(qosCompletionIssueLabelRes(issue))
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            selectedTestFamilies.sortedBy { family -> family.name }.forEach { family ->
                val familyStatus = familyStatusByType[family] ?: QosFamilyExecutionStatus.NOT_RUN
                val coverage = runCoverageByType[family]
                val preflightIssues = preflightIssuesByFamily[family].orEmpty()
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
                if (preflightIssues.isNotEmpty()) {
                    preflightIssues.toList().sortedBy { issue -> issue.name }.forEach { issue ->
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onQosRunnerStartClicked(family) }
                    ) {
                        Text(stringResource(R.string.performance_action_qos_runner_start))
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onQosRunnerPauseClicked(family) }
                    ) {
                        Text(stringResource(R.string.performance_action_qos_runner_pause))
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onQosRunnerResumeClicked(family) }
                    ) {
                        Text(stringResource(R.string.performance_action_qos_runner_resume))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onQosRunnerPassClicked(family) }
                    ) {
                        Text(stringResource(R.string.performance_action_qos_runner_pass))
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onQosRunnerFailClicked(family) }
                    ) {
                        Text(stringResource(R.string.performance_action_qos_runner_fail))
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onQosRunnerBlockClicked(family) }
                    ) {
                        Text(stringResource(R.string.performance_action_qos_runner_block))
                    }
                }
                if (familyStatus == QosFamilyExecutionStatus.FAILED || familyStatus == QosFamilyExecutionStatus.BLOCKED) {
                    Text(
                        text = stringResource(
                            R.string.performance_label_qos_family_reason_code,
                            familyReasonCodeByType[family]?.let { code ->
                                stringResource(qosIssueCodeLabelRes(code))
                            } ?: stringResource(R.string.value_not_available)
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    qosReasonOptionsForFamily(family).forEach { code ->
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onQosFamilyReasonCodeChanged(family, code) }
                        ) {
                            Text(
                                stringResource(
                                    R.string.performance_action_qos_reason_code_select,
                                    stringResource(qosIssueCodeLabelRes(code))
                                )
                            )
                        }
                    }
                    familyReasonCodeByType[family]?.let { code ->
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
                        value = familyFailureReasonByType[family].orEmpty(),
                        onValueChange = { value -> onQosFamilyFailureReasonChanged(family, value) },
                        label = { Text(stringResource(R.string.performance_input_qos_family_failure_reason)) }
                    )
                }
            }
            Text(
                text = stringResource(R.string.performance_header_qos_execution_timeline),
                style = MaterialTheme.typography.titleSmall
            )
            if (timelineEvents.isEmpty()) {
                Text(
                    text = stringResource(R.string.performance_label_qos_timeline_empty),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                timelineEvents
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
                    )
                    .take(8)
                    .forEach { event ->
                        val line = stringResource(
                            R.string.performance_label_qos_timeline_line,
                            formatEpoch(event.occurredAtEpochMillis),
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
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = configuredRepeat,
                onValueChange = onQosConfiguredRepeatChanged,
                label = { Text(stringResource(R.string.performance_input_qos_configured_repeat_count)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text(
                text = stringResource(R.string.performance_header_qos_script_editor),
                style = MaterialTheme.typography.titleSmall
            )
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
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = targetTechnology,
                onValueChange = onQosTargetTechnologyChanged,
                label = { Text(stringResource(R.string.performance_input_qos_target_technology)) }
            )
            if (
                configuredTechnologies.isNotEmpty() &&
                targetTechnology.isNotBlank() &&
                targetTechnology !in configuredTechnologies
            ) {
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
        }
    }
}

private fun formatEpoch(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return formatter.format(
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    )
}

private fun qosTestFamilyLabelRes(family: QosTestFamily): Int {
    return when (family) {
        QosTestFamily.THROUGHPUT_LATENCY -> R.string.qos_test_family_throughput_latency
        QosTestFamily.VIDEO_STREAMING -> R.string.qos_test_family_video_streaming
        QosTestFamily.SMS -> R.string.qos_test_family_sms
        QosTestFamily.VOLTE_CALL -> R.string.qos_test_family_volte_call
        QosTestFamily.CSFB_CALL -> R.string.qos_test_family_csfb_call
        QosTestFamily.EMERGENCY_CALL -> R.string.qos_test_family_emergency_call
        QosTestFamily.STANDARD_CALL -> R.string.qos_test_family_standard_call
    }
}

private fun qosFamilyExecutionStatusLabelRes(status: QosFamilyExecutionStatus): Int {
    return when (status) {
        QosFamilyExecutionStatus.NOT_RUN -> R.string.performance_qos_family_status_not_run
        QosFamilyExecutionStatus.PASSED -> R.string.performance_qos_family_status_passed
        QosFamilyExecutionStatus.FAILED -> R.string.performance_qos_family_status_failed
        QosFamilyExecutionStatus.BLOCKED -> R.string.performance_qos_family_status_blocked
    }
}

private fun qosExecutionEventTypeLabelRes(eventType: QosExecutionEventType): Int {
    return when (eventType) {
        QosExecutionEventType.STARTED -> R.string.performance_qos_event_started
        QosExecutionEventType.PAUSED -> R.string.performance_qos_event_paused
        QosExecutionEventType.RESUMED -> R.string.performance_qos_event_resumed
        QosExecutionEventType.PASSED -> R.string.performance_qos_family_status_passed
        QosExecutionEventType.FAILED -> R.string.performance_qos_family_status_failed
        QosExecutionEventType.BLOCKED -> R.string.performance_qos_family_status_blocked
    }
}

private fun qosIssueCodeLabelRes(code: QosExecutionIssueCode): Int {
    return when (code) {
        QosExecutionIssueCode.PREREQUISITE_NOT_READY -> R.string.qos_issue_code_prerequisite_not_ready
        QosExecutionIssueCode.TARGET_TECHNOLOGY_MISMATCH -> R.string.qos_issue_code_target_technology_mismatch
        QosExecutionIssueCode.PHONE_TARGET_MISSING -> R.string.qos_issue_code_phone_target_missing
        QosExecutionIssueCode.NETWORK_UNAVAILABLE -> R.string.qos_issue_code_network_unavailable
        QosExecutionIssueCode.THRESHOLD_NOT_MET -> R.string.qos_issue_code_threshold_not_met
        QosExecutionIssueCode.OPERATOR_ABORTED -> R.string.qos_issue_code_operator_aborted
        QosExecutionIssueCode.UNKNOWN -> R.string.qos_issue_code_unknown
    }
}

private fun qosIssueCodeActionRes(code: QosExecutionIssueCode): Int {
    return when (code) {
        QosExecutionIssueCode.PREREQUISITE_NOT_READY -> R.string.qos_issue_action_prerequisite_not_ready
        QosExecutionIssueCode.TARGET_TECHNOLOGY_MISMATCH -> R.string.qos_issue_action_target_technology_mismatch
        QosExecutionIssueCode.PHONE_TARGET_MISSING -> R.string.qos_issue_action_phone_target_missing
        QosExecutionIssueCode.NETWORK_UNAVAILABLE -> R.string.qos_issue_action_network_unavailable
        QosExecutionIssueCode.THRESHOLD_NOT_MET -> R.string.qos_issue_action_threshold_not_met
        QosExecutionIssueCode.OPERATOR_ABORTED -> R.string.qos_issue_action_operator_aborted
        QosExecutionIssueCode.UNKNOWN -> R.string.qos_issue_action_unknown
    }
}

private fun qosReasonOptionsForFamily(family: QosTestFamily): List<QosExecutionIssueCode> {
    return when (family) {
        QosTestFamily.THROUGHPUT_LATENCY,
        QosTestFamily.VIDEO_STREAMING -> listOf(
            QosExecutionIssueCode.NETWORK_UNAVAILABLE,
            QosExecutionIssueCode.THRESHOLD_NOT_MET,
            QosExecutionIssueCode.OPERATOR_ABORTED,
            QosExecutionIssueCode.UNKNOWN
        )

        QosTestFamily.SMS,
        QosTestFamily.VOLTE_CALL,
        QosTestFamily.CSFB_CALL,
        QosTestFamily.EMERGENCY_CALL,
        QosTestFamily.STANDARD_CALL -> listOf(
            QosExecutionIssueCode.PHONE_TARGET_MISSING,
            QosExecutionIssueCode.NETWORK_UNAVAILABLE,
            QosExecutionIssueCode.OPERATOR_ABORTED,
            QosExecutionIssueCode.UNKNOWN
        )
    }
}

private fun qosCompletionIssueLabelRes(issue: QosCompletionIssue): Int {
    return when (issue) {
        QosCompletionIssue.SCRIPT_REFERENCE_MISSING -> R.string.error_performance_qos_issue_script_reference_missing
        QosCompletionIssue.TEST_FAMILIES_MISSING -> R.string.error_performance_qos_issue_test_families_missing
        QosCompletionIssue.FAMILY_RESULT_INCOMPLETE -> R.string.error_performance_qos_issue_family_result_incomplete
        QosCompletionIssue.REPETITION_COVERAGE_INCOMPLETE -> R.string.error_performance_qos_issue_repetition_coverage_incomplete
        QosCompletionIssue.FAILURE_REASON_CODE_MISSING -> R.string.error_performance_qos_issue_failure_reason_missing
        QosCompletionIssue.PHONE_TARGET_MISSING -> R.string.error_performance_qos_issue_phone_target_missing
        QosCompletionIssue.TARGET_TECHNOLOGY_INVALID -> R.string.error_performance_qos_issue_target_technology_invalid
        QosCompletionIssue.COUNTERS_INCONSISTENT -> R.string.error_performance_qos_issue_counters_inconsistent
    }
}

private fun qosPreflightIssueLabelRes(issue: QosPreflightIssue): Int {
    return when (issue) {
        QosPreflightIssue.PREREQUISITES_NOT_READY -> R.string.error_performance_qos_issue_prerequisites_not_ready
        QosPreflightIssue.SCRIPT_REFERENCE_MISSING -> R.string.error_performance_qos_issue_script_reference_missing
        QosPreflightIssue.FAMILY_NOT_SELECTED -> R.string.error_performance_qos_issue_family_not_selected
        QosPreflightIssue.PHONE_TARGET_MISSING -> R.string.error_performance_qos_issue_phone_target_missing
        QosPreflightIssue.TARGET_TECHNOLOGY_INVALID -> R.string.error_performance_qos_issue_target_technology_invalid
        QosPreflightIssue.REPETITION_ALREADY_STARTED -> R.string.error_performance_qos_issue_repetition_already_started
        QosPreflightIssue.REPETITION_ALREADY_COMPLETED -> R.string.error_performance_qos_issue_repetition_coverage_incomplete
        QosPreflightIssue.ANOTHER_REPETITION_ACTIVE -> R.string.error_performance_qos_issue_another_repetition_active
        QosPreflightIssue.REPETITION_NOT_STARTED -> R.string.error_performance_qos_issue_repetition_not_started
        QosPreflightIssue.REPETITION_NOT_PAUSED -> R.string.error_performance_qos_issue_repetition_not_paused
        QosPreflightIssue.FAILURE_REASON_CODE_REQUIRED -> R.string.error_performance_qos_issue_failure_reason_missing
    }
}

private fun qosExecutionEngineStateLabelRes(state: QosExecutionEngineState): Int {
    return when (state) {
        QosExecutionEngineState.READY -> R.string.performance_qos_engine_state_ready
        QosExecutionEngineState.PREFLIGHT_BLOCKED -> R.string.performance_qos_engine_state_preflight_blocked
        QosExecutionEngineState.RUNNING -> R.string.performance_qos_engine_state_running
        QosExecutionEngineState.PAUSED -> R.string.performance_qos_engine_state_paused
        QosExecutionEngineState.RESUMED -> R.string.performance_qos_engine_state_resumed
        QosExecutionEngineState.COMPLETED -> R.string.performance_qos_engine_state_completed
        QosExecutionEngineState.FAILED -> R.string.performance_qos_engine_state_failed
        QosExecutionEngineState.BLOCKED -> R.string.performance_qos_engine_state_blocked
    }
}

private fun qosRecoveryStateLabelRes(state: QosRecoveryState): Int {
    return when (state) {
        QosRecoveryState.NONE -> R.string.performance_qos_recovery_state_none
        QosRecoveryState.RESUME_AVAILABLE -> R.string.performance_qos_recovery_state_resume_available
        QosRecoveryState.INVARIANT_BROKEN -> R.string.performance_qos_recovery_state_invariant_broken
    }
}

private fun qosRunPlanStatusLabelRes(status: QosRunPlanItemStatus): Int {
    return when (status) {
        QosRunPlanItemStatus.PENDING -> R.string.performance_qos_run_plan_status_pending
        QosRunPlanItemStatus.RUNNING -> R.string.performance_qos_run_plan_status_running
        QosRunPlanItemStatus.PAUSED -> R.string.performance_qos_run_plan_status_paused
        QosRunPlanItemStatus.PASSED -> R.string.performance_qos_run_plan_status_passed
        QosRunPlanItemStatus.FAILED -> R.string.performance_qos_run_plan_status_failed
        QosRunPlanItemStatus.BLOCKED -> R.string.performance_qos_run_plan_status_blocked
    }
}
