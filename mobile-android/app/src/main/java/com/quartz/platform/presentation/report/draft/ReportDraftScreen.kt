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
import androidx.compose.material3.AssistChip
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
import com.quartz.platform.domain.model.QosExecutionEventType
import com.quartz.platform.domain.model.QosExecutionEngineState
import com.quartz.platform.domain.model.QosExecutionIssueCode
import com.quartz.platform.domain.model.QosRecoveryState
import com.quartz.platform.domain.model.QosFamilyExecutionStatus
import com.quartz.platform.domain.model.NetworkStatus
import com.quartz.platform.domain.model.QosReportClosureProjection
import com.quartz.platform.domain.model.QosTestFamily
import com.quartz.platform.domain.model.ReportClosureProjection
import com.quartz.platform.domain.model.RetReportClosureProjection
import com.quartz.platform.domain.model.ThroughputReportClosureProjection
import com.quartz.platform.domain.model.XfeederReportClosureProjection
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.ReportSyncTrace
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederUnreliableReason
import com.quartz.platform.domain.model.qosExecutionEventSortOrder
import com.quartz.platform.presentation.performance.session.performanceSessionStatusLabelRes
import com.quartz.platform.presentation.performance.session.performanceWorkflowTypeLabelRes
import com.quartz.platform.presentation.components.AdvancedDisclosureButton
import com.quartz.platform.presentation.components.OperationalSectionCard
import com.quartz.platform.presentation.components.OperationalSeverity
import com.quartz.platform.presentation.components.OperationalSignal
import com.quartz.platform.presentation.components.OperationalSignalRow
import com.quartz.platform.presentation.ret.session.retResultOutcomeLabelRes
import com.quartz.platform.presentation.ret.session.retSessionStatusLabelRes
import com.quartz.platform.presentation.sync.syncStateDescriptionRes
import com.quartz.platform.presentation.sync.syncStateLabelRes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OperationalSectionCard(
                    title = stringResource(R.string.report_draft_section_review_header_title),
                    subtitle = stringResource(R.string.label_site_id, state.draft.siteId)
                ) {
                    OperationalSignalRow(
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
                    )
                    Text(
                        text = stringResource(syncStateDescriptionRes(state.syncState)),
                        style = MaterialTheme.typography.bodySmall
                    )
                    SyncTraceabilityDetails(trace = state.syncTrace)
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

            state.errorMessage?.let { error ->
                item {
                    OperationalSectionCard(
                        title = stringResource(R.string.home_runtime_alert_title)
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            state.infoMessage?.let { info ->
                item {
                    OperationalSectionCard(
                        title = stringResource(R.string.home_runtime_info_title)
                    ) {
                        Text(
                            text = info,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                OperationalSectionCard(
                    title = stringResource(R.string.report_draft_section_actions_title),
                    subtitle = stringResource(R.string.report_draft_section_actions_hint)
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving,
                        onClick = onSave
                    ) {
                        Text(
                            if (state.isSaving) {
                                stringResource(R.string.action_save_local_draft_loading)
                            } else {
                                stringResource(R.string.action_save_local_draft)
                            }
                        )
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isQueueingSync &&
                            !state.isSaving &&
                            !state.hasUnsavedChanges &&
                            state.syncState != ReportSyncState.PENDING,
                        onClick = onQueueSync
                    ) {
                        Text(
                            if (state.isQueueingSync) {
                                stringResource(R.string.action_enqueue_sync_loading)
                            } else {
                                stringResource(R.string.action_enqueue_sync)
                            }
                        )
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onBack
                    ) {
                        Text(stringResource(R.string.action_back_to_site))
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenReportList(state.draft.siteId) }
                    ) {
                        Text(stringResource(R.string.action_open_site_reports))
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidedClosureProjectionCard(
    projections: List<ReportClosureProjection>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.report_closure_projection_header),
                style = MaterialTheme.typography.titleSmall
            )

            if (projections.isEmpty()) {
                Text(
                    text = stringResource(R.string.report_closure_projection_empty),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                projections.forEach { projection ->
                    when (projection) {
                        is XfeederReportClosureProjection -> {
                            XfeederClosureProjectionContent(projection)
                        }

                        is RetReportClosureProjection -> {
                            RetClosureProjectionContent(projection)
                        }

                        is ThroughputReportClosureProjection -> {
                            ThroughputClosureProjectionContent(projection)
                        }

                        is QosReportClosureProjection -> {
                            QosClosureProjectionContent(projection)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun XfeederClosureProjectionContent(
    projection: XfeederReportClosureProjection
) {
    Text(
        text = stringResource(R.string.report_closure_workflow_xfeeder),
        style = MaterialTheme.typography.labelLarge
    )
    Text(
        text = stringResource(R.string.report_closure_sector, projection.sectorCode),
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        text = stringResource(
            R.string.report_closure_outcome,
            stringResource(xfeederOutcomeLabelRes(projection.sectorOutcome))
        ),
        style = MaterialTheme.typography.bodySmall
    )
    projection.relatedSectorCode?.let { related ->
        Text(
            text = stringResource(R.string.report_closure_related_sector, related),
            style = MaterialTheme.typography.bodySmall
        )
    }
    projection.unreliableReason?.let { reason ->
        Text(
            text = stringResource(
                R.string.report_closure_unreliable_reason,
                stringResource(xfeederUnreliableReasonLabelRes(reason))
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    projection.observedSectorCount?.let { observedCount ->
        Text(
            text = stringResource(R.string.report_closure_observed_sector_count, observedCount),
            style = MaterialTheme.typography.bodySmall
        )
    }
    Text(
        text = stringResource(R.string.label_updated_at, formatEpoch(projection.updatedAtEpochMillis)),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun RetClosureProjectionContent(
    projection: RetReportClosureProjection
) {
    Text(
        text = stringResource(R.string.report_closure_workflow_ret),
        style = MaterialTheme.typography.labelLarge
    )
    Text(
        text = stringResource(R.string.report_closure_sector, projection.sectorCode),
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        text = stringResource(
            R.string.report_closure_ret_result,
            stringResource(retResultOutcomeLabelRes(projection.resultOutcome))
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_ret_status,
            stringResource(retSessionStatusLabelRes(projection.sessionStatus))
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_ret_required_step_progress,
            projection.completedRequiredStepCount,
            projection.requiredStepCount
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_ret_measurement_zone,
            projection.measurementZoneRadiusMeters
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_ret_proximity_mode,
            if (projection.proximityModeEnabled) {
                stringResource(R.string.value_yes)
            } else {
                stringResource(R.string.value_no)
            }
        ),
        style = MaterialTheme.typography.bodySmall
    )
    projection.resultSummary?.let { summary ->
        Text(
            text = stringResource(R.string.report_closure_ret_result_summary, summary),
            style = MaterialTheme.typography.bodySmall
        )
    }
    Text(
        text = stringResource(R.string.label_updated_at, formatEpoch(projection.updatedAtEpochMillis)),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun ThroughputClosureProjectionContent(
    projection: ThroughputReportClosureProjection
) {
    Text(
        text = stringResource(
            R.string.report_closure_workflow_performance,
            stringResource(performanceWorkflowTypeLabelRes(com.quartz.platform.domain.model.PerformanceWorkflowType.THROUGHPUT))
        ),
        style = MaterialTheme.typography.labelLarge
    )
    Text(
        text = stringResource(R.string.report_closure_performance_site, projection.siteCode),
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        text = stringResource(
            R.string.report_closure_performance_status,
            stringResource(performanceSessionStatusLabelRes(projection.sessionStatus))
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_performance_required_step_progress,
            projection.completedRequiredStepCount,
            projection.requiredStepCount
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_performance_prerequisites,
            if (projection.preconditionsReady) {
                stringResource(R.string.value_yes)
            } else {
                stringResource(R.string.value_no)
            }
        ),
        style = MaterialTheme.typography.bodySmall
    )
    PerformanceObservedDiagnosticsSection(
        observedNetworkStatus = projection.observedNetworkStatus,
        observedBatteryLevelPercent = projection.observedBatteryLevelPercent,
        observedLocationAvailable = projection.observedLocationAvailable,
        observedSignalsCapturedAtEpochMillis = projection.observedSignalsCapturedAtEpochMillis
    )
    projection.downloadMbps?.let { value ->
        Text(
            text = stringResource(R.string.report_closure_performance_download, value),
            style = MaterialTheme.typography.bodySmall
        )
    }
    projection.uploadMbps?.let { value ->
        Text(
            text = stringResource(R.string.report_closure_performance_upload, value),
            style = MaterialTheme.typography.bodySmall
        )
    }
    projection.latencyMs?.let { value ->
        Text(
            text = stringResource(R.string.report_closure_performance_latency, value),
            style = MaterialTheme.typography.bodySmall
        )
    }
    if (projection.minDownloadMbps != null || projection.minUploadMbps != null || projection.maxLatencyMs != null) {
        Text(
            text = stringResource(
                R.string.report_closure_performance_thresholds,
                projection.minDownloadMbps?.toString() ?: "-",
                projection.minUploadMbps?.toString() ?: "-",
                projection.maxLatencyMs?.toString() ?: "-"
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    projection.resultSummary?.let { summary ->
        Text(
            text = stringResource(R.string.report_closure_performance_result_summary, summary),
            style = MaterialTheme.typography.bodySmall
        )
    }
    Text(
        text = stringResource(R.string.label_updated_at, formatEpoch(projection.updatedAtEpochMillis)),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun QosClosureProjectionContent(
    projection: QosReportClosureProjection
) {
    Text(
        text = stringResource(
            R.string.report_closure_workflow_performance,
            stringResource(performanceWorkflowTypeLabelRes(com.quartz.platform.domain.model.PerformanceWorkflowType.QOS_SCRIPT))
        ),
        style = MaterialTheme.typography.labelLarge
    )
    Text(
        text = stringResource(R.string.report_closure_performance_site, projection.siteCode),
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        text = stringResource(
            R.string.report_closure_performance_status,
            stringResource(performanceSessionStatusLabelRes(projection.sessionStatus))
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_performance_required_step_progress,
            projection.completedRequiredStepCount,
            projection.requiredStepCount
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_performance_prerequisites,
            if (projection.preconditionsReady) {
                stringResource(R.string.value_yes)
            } else {
                stringResource(R.string.value_no)
            }
        ),
        style = MaterialTheme.typography.bodySmall
    )
    PerformanceObservedDiagnosticsSection(
        observedNetworkStatus = projection.observedNetworkStatus,
        observedBatteryLevelPercent = projection.observedBatteryLevelPercent,
        observedLocationAvailable = projection.observedLocationAvailable,
        observedSignalsCapturedAtEpochMillis = projection.observedSignalsCapturedAtEpochMillis
    )
    Text(
        text = stringResource(
            R.string.report_closure_performance_qos_script,
            projection.scriptName ?: stringResource(R.string.value_not_available)
        ),
        style = MaterialTheme.typography.bodySmall
    )
    projection.configuredRepeatCount?.let { repeat ->
        Text(
            text = stringResource(R.string.report_closure_performance_qos_repeat_configured, repeat),
            style = MaterialTheme.typography.bodySmall
        )
    }
    if (projection.configuredTechnologies.isNotEmpty()) {
        Text(
            text = stringResource(
                R.string.report_closure_performance_qos_configured_technologies,
                projection.configuredTechnologies.sorted().joinToString(", ")
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    projection.scriptSnapshotUpdatedAtEpochMillis?.let { snapshotAt ->
        Text(
            text = stringResource(
                R.string.report_closure_performance_qos_script_snapshot_at,
                formatEpoch(snapshotAt)
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    if (projection.testFamilies.isNotEmpty()) {
        val familyLabels = buildList {
            for (family in projection.testFamilies) {
                add(stringResource(qosTestFamilyLabelRes(family)))
            }
        }
        Text(
            text = stringResource(
                R.string.report_closure_performance_qos_test_families,
                familyLabels.joinToString(", ")
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    projection.targetTechnology?.let { technology ->
        Text(
            text = stringResource(R.string.report_closure_performance_qos_target_technology, technology),
            style = MaterialTheme.typography.bodySmall
        )
        if (projection.configuredTechnologies.isNotEmpty() && technology !in projection.configuredTechnologies) {
            Text(
                text = stringResource(R.string.report_closure_performance_qos_target_technology_mismatch),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
    if (projection.familyExecutionResults.isNotEmpty()) {
        Text(
            text = stringResource(R.string.report_closure_performance_qos_family_results_header),
            style = MaterialTheme.typography.bodySmall
        )
        projection.familyExecutionResults
            .sortedBy { result -> result.family.name }
            .forEach { result ->
                val line = stringResource(
                    R.string.report_closure_performance_qos_family_result_line,
                    stringResource(qosTestFamilyLabelRes(result.family)),
                    stringResource(qosFamilyExecutionStatusLabelRes(result.status))
                )
                val classifiedReason = result.failureReasonCode?.let { code ->
                    stringResource(
                        R.string.report_closure_performance_qos_reason_code,
                        stringResource(qosIssueCodeLabelRes(code))
                    )
                }
                Text(
                    text = buildString {
                        append(line)
                        if (!classifiedReason.isNullOrBlank()) {
                            append(" (")
                            append(classifiedReason)
                            append(")")
                        }
                        result.failureReason?.takeIf { it.isNotBlank() }?.let { reason ->
                            append(" (")
                            append(stringResource(R.string.label_failure_reason, reason))
                            append(")")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
    }
    Text(
        text = stringResource(
            R.string.report_closure_performance_qos_run_coverage,
            projection.familiesMeetingRequiredRepeatCount,
            projection.selectedFamilyCount,
            projection.requiredRepeatCount,
            projection.passFailRunCount,
            projection.blockedRunCount
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_performance_qos_engine_state,
            stringResource(qosEngineStateLabelRes(projection.executionEngineState))
        ),
        style = MaterialTheme.typography.bodySmall
    )
    if (projection.recoveryState != QosRecoveryState.NONE) {
        Text(
            text = stringResource(
                R.string.report_closure_performance_qos_recovery_state,
                stringResource(qosRecoveryStateLabelRes(projection.recoveryState))
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    projection.activeFamily?.let { activeFamily ->
        Text(
            text = stringResource(
                R.string.report_closure_performance_qos_active_run,
                stringResource(qosTestFamilyLabelRes(activeFamily)),
                projection.activeRepetitionIndex ?: 1
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    projection.nextFamily?.let { nextFamily ->
        Text(
            text = stringResource(
                R.string.report_closure_performance_qos_next_run,
                stringResource(qosTestFamilyLabelRes(nextFamily)),
                projection.nextRepetitionIndex ?: 1
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    Text(
        text = stringResource(
            R.string.report_closure_performance_qos_plan_progress,
            projection.plannedRunCount,
            projection.pendingRunCount
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_performance_qos_checkpoint_count,
            projection.checkpointCount
        ),
        style = MaterialTheme.typography.bodySmall
    )
    if (projection.executionTimelineEvents.isNotEmpty()) {
        Text(
            text = stringResource(R.string.report_closure_performance_qos_timeline_header),
            style = MaterialTheme.typography.bodySmall
        )
        val orderedEvents = projection.executionTimelineEvents.sortedWith(
            compareByDescending<com.quartz.platform.domain.model.QosExecutionTimelineEvent> { event ->
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
        val visibleEvents = orderedEvents.take(12)
        visibleEvents.forEach { event ->
            val line = stringResource(
                R.string.report_closure_performance_qos_timeline_line,
                formatEpoch(event.occurredAtEpochMillis),
                stringResource(qosTestFamilyLabelRes(event.family)),
                event.repetitionIndex,
                stringResource(qosExecutionEventTypeLabelRes(event.eventType))
            )
            val classifiedReason = event.reasonCode?.let { code ->
                stringResource(
                    R.string.report_closure_performance_qos_reason_code,
                    stringResource(qosIssueCodeLabelRes(code))
                )
            }
            Text(
                text = buildString {
                    append(line)
                    if (!classifiedReason.isNullOrBlank()) {
                        append(" (")
                        append(classifiedReason)
                        append(")")
                    }
                    event.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                        append(" (")
                        append(stringResource(R.string.label_failure_reason, reason))
                        append(")")
                    }
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
        val hiddenCount = orderedEvents.size - visibleEvents.size
        if (hiddenCount > 0) {
            Text(
                text = stringResource(
                    R.string.report_closure_performance_qos_timeline_more,
                    hiddenCount
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    Text(
        text = stringResource(
            R.string.report_closure_performance_qos_results,
            projection.iterationCount,
            projection.successCount,
            projection.failureCount
        ),
        style = MaterialTheme.typography.bodySmall
    )
    projection.resultSummary?.let { summary ->
        Text(
            text = stringResource(R.string.report_closure_performance_result_summary, summary),
            style = MaterialTheme.typography.bodySmall
        )
    }
    Text(
        text = stringResource(R.string.label_updated_at, formatEpoch(projection.updatedAtEpochMillis)),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun PerformanceObservedDiagnosticsSection(
    observedNetworkStatus: NetworkStatus?,
    observedBatteryLevelPercent: Int?,
    observedLocationAvailable: Boolean?,
    observedSignalsCapturedAtEpochMillis: Long?
) {
    if (
        observedNetworkStatus == null &&
        observedBatteryLevelPercent == null &&
        observedLocationAvailable == null &&
        observedSignalsCapturedAtEpochMillis == null
    ) {
        return
    }

    observedNetworkStatus?.let { networkStatus ->
        Text(
            text = stringResource(
                R.string.report_closure_performance_device_network,
                stringResource(networkStatusLabelRes(networkStatus))
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    observedBatteryLevelPercent?.let { batteryPercent ->
        val batteryLabel = buildString {
            append("$batteryPercent%")
            append(" • ")
            append(
                if (batteryPercent >= com.quartz.platform.domain.model.PerformanceSession.MIN_RECOMMENDED_BATTERY_PERCENT) {
                    "OK"
                } else {
                    "LOW"
                }
            )
        }
        Text(
            text = stringResource(R.string.report_closure_performance_device_battery, batteryLabel),
            style = MaterialTheme.typography.bodySmall
        )
    }
    observedLocationAvailable?.let { locationAvailable ->
        Text(
            text = stringResource(
                R.string.report_closure_performance_device_location,
                if (locationAvailable) {
                    stringResource(R.string.value_yes)
                } else {
                    stringResource(R.string.value_no)
                }
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    observedSignalsCapturedAtEpochMillis?.let { capturedAt ->
        Text(
            text = stringResource(
                R.string.report_closure_performance_device_captured_at,
                formatEpoch(capturedAt)
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DebugLiveSyncTraceSnapshotCard(trace: ReportSyncTrace) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.debug_header_live_sync_snapshot),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(
                    R.string.label_sync_state,
                    stringResource(syncStateLabelRes(trace.state))
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.label_retry_count, trace.retryCount),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = trace.lastAttemptAtEpochMillis?.let { lastAttempt ->
                    stringResource(R.string.label_last_attempt, formatEpoch(lastAttempt))
                } ?: stringResource(R.string.label_last_attempt_none),
                style = MaterialTheme.typography.bodySmall
            )
            if (!trace.failureReason.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.label_failure_reason, trace.failureReason),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (trace.state == ReportSyncState.FAILED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun DebugSyncSimulationCard(
    mode: SyncSimulationMode,
    onModeSelected: (SyncSimulationMode) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.debug_header_sync_simulation),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(R.string.debug_label_mode_active, mode.name),
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = mode != SyncSimulationMode.NORMAL_SUCCESS,
                onClick = { onModeSelected(SyncSimulationMode.NORMAL_SUCCESS) }
            ) {
                Text(stringResource(R.string.debug_action_force_normal_success))
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = mode != SyncSimulationMode.FAIL_NEXT_RETRYABLE,
                onClick = { onModeSelected(SyncSimulationMode.FAIL_NEXT_RETRYABLE) }
            ) {
                Text(stringResource(R.string.debug_action_fail_next_retryable))
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = mode != SyncSimulationMode.FAIL_ONCE_THEN_SUCCESS,
                onClick = { onModeSelected(SyncSimulationMode.FAIL_ONCE_THEN_SUCCESS) }
            ) {
                Text(stringResource(R.string.debug_action_fail_once_then_success))
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = mode != SyncSimulationMode.FAIL_NEXT_TERMINAL,
                onClick = { onModeSelected(SyncSimulationMode.FAIL_NEXT_TERMINAL) }
            ) {
                Text(stringResource(R.string.debug_action_fail_next_terminal))
            }
        }
    }
}

@Composable
private fun DebugSyncDemoScriptCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.debug_header_demo_script),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(R.string.debug_demo_success_title),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.debug_demo_success_body),
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = stringResource(R.string.debug_demo_retryable_title),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.debug_demo_retryable_body),
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = stringResource(R.string.debug_demo_terminal_title),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.debug_demo_terminal_body),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SyncTraceabilityDetails(trace: ReportSyncTrace) {
    trace.lastAttemptAtEpochMillis?.let { lastAttempt ->
        Text(
            text = stringResource(R.string.label_last_attempt, formatEpoch(lastAttempt)),
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (trace.retryCount > 0) {
        Text(
            text = stringResource(R.string.label_retry_count, trace.retryCount),
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (!trace.failureReason.isNullOrBlank()) {
        val message = if (trace.state == ReportSyncState.FAILED) {
            stringResource(R.string.label_local_failure_reason, trace.failureReason)
        } else {
            stringResource(R.string.label_last_failure_reason, trace.failureReason)
        }
        val color = if (trace.state == ReportSyncState.FAILED) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
private fun SyncStateChip(syncState: ReportSyncState) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                stringResource(
                    R.string.label_sync_state,
                    stringResource(syncStateLabelRes(syncState))
                )
            )
        }
    )
}

private fun formatEpoch(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return formatter.format(
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    )
}

private fun xfeederOutcomeLabelRes(outcome: XfeederSectorOutcome): Int {
    return when (outcome) {
        XfeederSectorOutcome.NOT_TESTED -> R.string.xfeeder_outcome_not_tested
        XfeederSectorOutcome.WAITING_NETWORK -> R.string.xfeeder_outcome_waiting_network
        XfeederSectorOutcome.OK -> R.string.xfeeder_outcome_ok
        XfeederSectorOutcome.CROSSED -> R.string.xfeeder_outcome_crossed
        XfeederSectorOutcome.MIXFEEDER -> R.string.xfeeder_outcome_mixfeeder
        XfeederSectorOutcome.UNRELIABLE -> R.string.xfeeder_outcome_unreliable
    }
}

private fun networkStatusLabelRes(status: NetworkStatus): Int {
    return when (status) {
        NetworkStatus.AVAILABLE -> R.string.performance_device_network_available
        NetworkStatus.UNAVAILABLE -> R.string.performance_device_network_unavailable
    }
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

private fun qosEngineStateLabelRes(state: QosExecutionEngineState): Int {
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

private fun qosIssueCodeLabelRes(code: QosExecutionIssueCode): Int {
    return when (code) {
        QosExecutionIssueCode.PREREQUISITE_NOT_READY -> R.string.qos_issue_code_prerequisite_not_ready
        QosExecutionIssueCode.TARGET_TECHNOLOGY_MISMATCH -> R.string.qos_issue_code_target_technology_mismatch
        QosExecutionIssueCode.PHONE_TARGET_MISSING -> R.string.qos_issue_code_phone_target_missing
        QosExecutionIssueCode.NETWORK_UNAVAILABLE -> R.string.qos_issue_code_network_unavailable
        QosExecutionIssueCode.BATTERY_INSUFFICIENT -> R.string.qos_issue_code_battery_insufficient
        QosExecutionIssueCode.LOCATION_UNAVAILABLE -> R.string.qos_issue_code_location_unavailable
        QosExecutionIssueCode.THRESHOLD_NOT_MET -> R.string.qos_issue_code_threshold_not_met
        QosExecutionIssueCode.OPERATOR_ABORTED -> R.string.qos_issue_code_operator_aborted
        QosExecutionIssueCode.UNKNOWN -> R.string.qos_issue_code_unknown
    }
}

private fun xfeederUnreliableReasonLabelRes(reason: XfeederUnreliableReason): Int {
    return when (reason) {
        XfeederUnreliableReason.NO_MAJORITY_SECTOR -> {
            R.string.xfeeder_unreliable_reason_no_majority_sector
        }

        XfeederUnreliableReason.UNSTABLE_SECTOR_SWITCHING -> {
            R.string.xfeeder_unreliable_reason_unstable_sector_switching
        }
    }
}
