package com.quartz.platform.presentation.report.draft

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quartz.platform.R
import com.quartz.platform.data.remote.simulation.SyncSimulationMode
import com.quartz.platform.domain.model.QosReportClosureProjection
import com.quartz.platform.domain.model.ReportClosureProjection
import com.quartz.platform.domain.model.RetReportClosureProjection
import com.quartz.platform.domain.model.ThroughputReportClosureProjection
import com.quartz.platform.domain.model.XfeederReportClosureProjection
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.ReportSyncTrace
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederUnreliableReason
import com.quartz.platform.presentation.performance.session.performanceSessionStatusLabelRes
import com.quartz.platform.presentation.performance.session.performanceWorkflowTypeLabelRes
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.label_site_id, state.draft.siteId),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                stringResource(R.string.label_local_revision, state.draft.revision),
                style = MaterialTheme.typography.bodyMedium
            )
            if (state.hasUnsavedChanges) {
                Text(
                    text = stringResource(R.string.message_unsaved_local_changes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            SyncStateChip(syncState = state.syncState)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(syncStateDescriptionRes(state.syncState)),
                        style = MaterialTheme.typography.bodySmall
                    )
                    SyncTraceabilityDetails(trace = state.syncTrace)
                }
            }

            GuidedClosureProjectionCard(
                projections = state.closureProjections
            )

            if (state.isSyncSimulationControlVisible) {
                DebugSyncSimulationCard(
                    mode = state.syncSimulationMode,
                    onModeSelected = onSyncSimulationModeSelected
                )
                DebugLiveSyncTraceSnapshotCard(trace = state.syncTrace)
                DebugSyncDemoScriptCard()
            }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text(stringResource(R.string.input_label_report_observation)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (state.infoMessage != null) {
                Text(
                    text = state.infoMessage,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

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

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBack
            ) {
                Text(stringResource(R.string.action_back_to_site))
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenReportList(state.draft.siteId) }
            ) {
                Text(stringResource(R.string.action_open_site_reports))
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
    Text(
        text = stringResource(
            R.string.report_closure_performance_qos_script,
            projection.scriptName ?: stringResource(R.string.value_not_available)
        ),
        style = MaterialTheme.typography.bodySmall
    )
    projection.targetTechnology?.let { technology ->
        Text(
            text = stringResource(R.string.report_closure_performance_qos_target_technology, technology),
            style = MaterialTheme.typography.bodySmall
        )
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
