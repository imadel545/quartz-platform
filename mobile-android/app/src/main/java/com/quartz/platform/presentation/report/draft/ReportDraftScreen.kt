package com.quartz.platform.presentation.report.draft

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quartz.platform.R
import com.quartz.platform.data.remote.simulation.SyncSimulationMode
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.presentation.components.AdvancedDisclosureButton
import com.quartz.platform.presentation.components.MissionHeaderCard
import com.quartz.platform.presentation.components.MissionPrimaryActionBar
import com.quartz.platform.presentation.components.MissionPrimaryActionButton
import com.quartz.platform.presentation.components.OperationalMessageCard
import com.quartz.platform.presentation.components.OperationalSectionCard
import com.quartz.platform.presentation.components.OperationalSeverity
import com.quartz.platform.presentation.components.OperationalSignal
import com.quartz.platform.presentation.components.OperationalSignalRow
import com.quartz.platform.presentation.components.OperationalStateBanner
import com.quartz.platform.presentation.sync.syncStateDescriptionRes
import com.quartz.platform.presentation.sync.syncStateLabelRes

@Composable
fun ReportDraftRoute(
    onBack: () -> Unit,
    onOpenReportList: (String) -> Unit,
    viewModel: ReportDraftViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    ReportDraftScreen(
        state = state,
        onBack = onBack,
        onOpenReportList = onOpenReportList,
        onTitleChanged = viewModel::onTitleChanged,
        onObservationChanged = viewModel::onObservationChanged,
        onSave = viewModel::onSaveClicked,
        onQueueSync = viewModel::onQueueSyncClicked,
        onSyncSimulationModeSelected = viewModel::onSyncSimulationModeSelected
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDraftScreen(
    state: ReportDraftUiState,
    onBack: () -> Unit,
    onOpenReportList: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onObservationChanged: (String) -> Unit,
    onSave: () -> Unit,
    onQueueSync: () -> Unit,
    onSyncSimulationModeSelected: (SyncSimulationMode) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_report_draft)) }
            )
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

        if (state.draft == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.message_draft_not_found_in_cache))
                Button(onClick = onBack) {
                    Text(stringResource(R.string.action_back_to_site))
                }
            }
            return@Scaffold
        }

        var showDeveloperTools by rememberSaveable { mutableStateOf(false) }
        var showClosureDetails by rememberSaveable { mutableStateOf(false) }
        var showTechnicalEvidence by rememberSaveable { mutableStateOf(false) }
        var showDraftEditor by rememberSaveable { mutableStateOf(false) }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MissionHeaderCard(
                    title = stringResource(R.string.report_draft_section_review_header_title),
                    subtitle = stringResource(R.string.label_site_id, state.draft.siteId),
                    signals = listOf(
                        OperationalSignal(stringResource(R.string.label_local_revision, state.draft.revision)),
                        OperationalSignal(
                            text = stringResource(
                                R.string.label_sync_state,
                                stringResource(syncStateLabelRes(state.syncState))
                            ),
                            severity = when (state.syncState) {
                                ReportSyncState.SYNCED -> OperationalSeverity.SUCCESS
                                ReportSyncState.FAILED -> OperationalSeverity.CRITICAL
                                ReportSyncState.PENDING -> OperationalSeverity.WARNING
                                ReportSyncState.LOCAL_ONLY -> OperationalSeverity.NORMAL
                            }
                        )
                    )
                ) {
                    Text(
                        text = stringResource(syncStateDescriptionRes(state.syncState)),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (state.hasUnsavedChanges) {
                        Text(
                            text = stringResource(R.string.message_unsaved_local_changes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                val reviewerSeverity = when {
                    state.syncState == ReportSyncState.FAILED -> OperationalSeverity.CRITICAL
                    state.hasUnsavedChanges -> OperationalSeverity.WARNING
                    state.syncState == ReportSyncState.PENDING -> OperationalSeverity.WARNING
                    state.closureProjections.isEmpty() -> OperationalSeverity.WARNING
                    else -> OperationalSeverity.SUCCESS
                }
                val reviewerMessage = when {
                    state.syncState == ReportSyncState.FAILED -> {
                        stringResource(R.string.report_draft_runtime_state_sync_failed)
                    }
                    state.hasUnsavedChanges -> {
                        stringResource(R.string.report_draft_runtime_state_unsaved)
                    }
                    state.syncState == ReportSyncState.PENDING -> {
                        stringResource(R.string.report_draft_runtime_state_pending)
                    }
                    state.closureProjections.isEmpty() -> {
                        stringResource(R.string.report_draft_runtime_state_missing_evidence)
                    }
                    else -> stringResource(R.string.report_draft_runtime_state_ready)
                }
                val reviewerHint = when {
                    state.hasUnsavedChanges -> stringResource(R.string.report_draft_runtime_state_unsaved_hint)
                    state.syncState == ReportSyncState.FAILED -> stringResource(R.string.report_draft_runtime_state_sync_failed_hint)
                    state.syncState == ReportSyncState.PENDING -> stringResource(R.string.report_draft_runtime_state_pending_hint)
                    else -> stringResource(R.string.report_draft_runtime_state_ready_hint)
                }
                OperationalStateBanner(
                    title = stringResource(R.string.report_draft_runtime_state_title),
                    message = reviewerMessage,
                    severity = reviewerSeverity,
                    hint = reviewerHint
                )
            }

            item {
                ReviewerCriticalFindingsCard(state = state)
            }

            item {
                OperationalSectionCard(
                    title = stringResource(R.string.report_draft_section_guided_evidence_title),
                    subtitle = stringResource(
                        R.string.report_draft_section_guided_evidence_hint,
                        state.closureProjections.size
                    )
                ) {
                    OperationalSignalRow(
                        signals = listOf(
                            OperationalSignal(
                                text = stringResource(
                                    R.string.report_draft_signal_guided_projection_count,
                                    state.closureProjections.size
                                ),
                                severity = if (state.closureProjections.isEmpty()) {
                                    OperationalSeverity.NORMAL
                                } else {
                                    OperationalSeverity.SUCCESS
                                }
                            )
                        )
                    )
                    AdvancedDisclosureButton(
                        expanded = showClosureDetails,
                        onToggle = { showClosureDetails = !showClosureDetails },
                        showLabel = stringResource(R.string.report_draft_action_show_guided_evidence),
                        hideLabel = stringResource(R.string.report_draft_action_hide_guided_evidence)
                    )
                }
            }

            if (showClosureDetails) {
                item {
                    GuidedClosureProjectionCard(
                        projections = state.closureProjections
                    )
                }
            }

            item {
                OperationalSectionCard(
                    title = stringResource(R.string.report_draft_section_actions_title),
                    subtitle = stringResource(R.string.report_draft_section_actions_hint)
                ) {
                    val shouldSaveFirst = state.hasUnsavedChanges
                    val canQueueSync = !state.isQueueingSync &&
                        !state.isSaving &&
                        !state.hasUnsavedChanges &&
                        state.syncState != ReportSyncState.PENDING
                    MissionPrimaryActionBar(
                        primaryAction = {
                            MissionPrimaryActionButton(
                                label = if (shouldSaveFirst) {
                                    if (state.isSaving) {
                                        stringResource(R.string.action_save_local_draft_loading)
                                    } else {
                                        stringResource(R.string.action_save_local_draft)
                                    }
                                } else {
                                    if (state.isQueueingSync) {
                                        stringResource(R.string.action_enqueue_sync_loading)
                                    } else {
                                        stringResource(R.string.action_enqueue_sync)
                                    }
                                },
                                onClick = if (shouldSaveFirst) onSave else onQueueSync,
                                enabled = if (shouldSaveFirst) !state.isSaving else canQueueSync
                            )
                        },
                        secondaryAction = {
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onOpenReportList(state.draft.siteId) }
                            ) {
                                Text(stringResource(R.string.action_open_site_reports))
                            }
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onBack
                            ) {
                                Text(stringResource(R.string.action_back_to_site))
                            }
                        }
                    )
                }
            }

            item {
                AdvancedDisclosureButton(
                    expanded = showTechnicalEvidence,
                    onToggle = { showTechnicalEvidence = !showTechnicalEvidence },
                    showLabel = stringResource(R.string.report_draft_action_show_technical_evidence),
                    hideLabel = stringResource(R.string.report_draft_action_hide_technical_evidence)
                )
            }

            if (showTechnicalEvidence) {
                item {
                    OperationalSectionCard(
                        title = stringResource(R.string.report_draft_section_technical_evidence_title),
                        subtitle = stringResource(R.string.report_draft_section_technical_evidence_hint)
                    ) {
                        SyncTraceabilityDetails(trace = state.syncTrace)
                    }
                }
            }

            item {
                AdvancedDisclosureButton(
                    expanded = showDraftEditor,
                    onToggle = { showDraftEditor = !showDraftEditor },
                    showLabel = stringResource(R.string.report_draft_action_show_editor),
                    hideLabel = stringResource(R.string.report_draft_action_hide_editor)
                )
            }

            if (showDraftEditor) {
                item {
                    OperationalSectionCard(
                        title = stringResource(R.string.report_draft_section_content_title),
                        subtitle = stringResource(R.string.report_draft_section_content_hint)
                    ) {
                        OutlinedTextField(
                            value = state.titleInput,
                            onValueChange = onTitleChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.input_label_report_title)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                        )

                        OutlinedTextField(
                            value = state.observationInput,
                            onValueChange = onObservationChanged,
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 5,
                            label = { Text(stringResource(R.string.input_label_report_observation)) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                        )
                    }
                }
            }

            if (state.isSyncSimulationControlVisible) {
                item {
                    AdvancedDisclosureButton(
                        expanded = showDeveloperTools,
                        onToggle = { showDeveloperTools = !showDeveloperTools },
                        showLabel = stringResource(R.string.report_draft_action_show_developer_tools),
                        hideLabel = stringResource(R.string.report_draft_action_hide_developer_tools)
                    )
                }
                if (showDeveloperTools) {
                    item {
                        DebugSyncSimulationCard(
                            mode = state.syncSimulationMode,
                            onModeSelected = onSyncSimulationModeSelected
                        )
                    }
                    item {
                        DebugLiveSyncTraceSnapshotCard(trace = state.syncTrace)
                    }
                    item {
                        DebugSyncDemoScriptCard()
                    }
                }
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

        }
    }
}

@Composable
private fun ReviewerCriticalFindingsCard(
    state: ReportDraftUiState
) {
    val findings = buildList {
        if (state.syncState == ReportSyncState.FAILED) {
            add(
                OperationalSignal(
                    text = stringResource(R.string.report_draft_finding_sync_failed),
                    severity = OperationalSeverity.CRITICAL
                )
            )
        }
        if (state.hasUnsavedChanges) {
            add(
                OperationalSignal(
                    text = stringResource(R.string.report_draft_finding_unsaved_changes),
                    severity = OperationalSeverity.WARNING
                )
            )
        }
        if (state.closureProjections.isEmpty()) {
            add(
                OperationalSignal(
                    text = stringResource(R.string.report_draft_finding_no_guided_evidence),
                    severity = OperationalSeverity.WARNING
                )
            )
        } else {
            add(
                OperationalSignal(
                    text = stringResource(
                        R.string.report_draft_signal_guided_projection_count,
                        state.closureProjections.size
                    ),
                    severity = OperationalSeverity.SUCCESS
                )
            )
        }
    }

    OperationalSectionCard(
        title = stringResource(R.string.report_draft_section_findings_title),
        subtitle = stringResource(R.string.report_draft_section_findings_hint)
    ) {
        OperationalSignalRow(signals = findings, maxVisibleSignals = 4)
    }
}
