package com.quartz.platform.presentation.report.list

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quartz.platform.R
import com.quartz.platform.domain.model.QosExecutionEngineState
import com.quartz.platform.domain.model.QosRecoveryState
import com.quartz.platform.domain.model.QosExecutionIssueCode
import com.quartz.platform.domain.model.QosTestFamily
import com.quartz.platform.domain.model.ReportListClosureSummary
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.SiteReportListItem
import com.quartz.platform.domain.model.XfeederSignal
import com.quartz.platform.presentation.performance.session.performanceSessionStatusLabelRes
import com.quartz.platform.presentation.performance.session.performanceWorkflowTypeLabelRes
import com.quartz.platform.presentation.ret.session.retResultOutcomeLabelRes
import com.quartz.platform.presentation.sync.syncStateLabelRes
import com.quartz.platform.presentation.xfeeder.session.xfeederSectorOutcomeLabelRes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ReportListRoute(
    onBack: () -> Unit,
    onOpenDraft: (String) -> Unit,
    viewModel: ReportListViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ReportListEvent.OpenDraft -> onOpenDraft(event.draftId)
            }
        }
    }

    ReportListScreen(
        state = state,
        onBack = onBack,
        onOpenDraft = viewModel::onOpenDraftClicked,
        onRetryFailedSync = viewModel::onRetryFailedSyncClicked,
        onFilterSelected = viewModel::onFilterSelected
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportListScreen(
    state: ReportListUiState,
    onBack: () -> Unit,
    onOpenDraft: (String) -> Unit,
    onRetryFailedSync: (String) -> Unit,
    onFilterSelected: (ReportListFilter) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_local_reports)) }
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

            state.errorMessage != null && state.reports.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = onBack) {
                        Text(stringResource(R.string.action_back_to_site))
                    }
                }
            }

            state.isEmpty -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.empty_local_reports_for_site),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.empty_local_reports_for_site_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onBack) {
                        Text(stringResource(R.string.action_back_to_site))
                    }
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
                        Text(
                            text = stringResource(R.string.label_site_id, state.siteId),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    item {
                        ReportListFilterRow(
                            selectedFilter = state.selectedFilter,
                            onFilterSelected = onFilterSelected
                        )
                    }

                    state.infoMessage?.let { info ->
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = info,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    state.errorMessage?.let { error ->
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = error,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (state.isFilterEmpty) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = stringResource(R.string.empty_filtered_local_reports),
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        items(state.filteredReports, key = { it.draftId }) { report ->
                            ReportListItemCard(
                                report = report,
                                isRetrying = report.draftId in state.retryingDraftIds,
                                onOpenDraft = onOpenDraft,
                                onRetryFailedSync = onRetryFailedSync
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
private fun ReportListFilterRow(
    selectedFilter: ReportListFilter,
    onFilterSelected: (ReportListFilter) -> Unit
) {
    val filters = ReportListFilter.values().toList()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.label_report_list_filter),
            style = MaterialTheme.typography.bodyMedium
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { filter ->
                FilterChip(
                    selected = filter == selectedFilter,
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            text = when (filter) {
                                ReportListFilter.ALL -> stringResource(R.string.report_list_filter_all)
                                ReportListFilter.XFEEDER -> stringResource(R.string.report_list_filter_xfeeder)
                                ReportListFilter.RET -> stringResource(R.string.report_list_filter_ret)
                                ReportListFilter.PERFORMANCE -> stringResource(R.string.report_list_filter_performance)
                                ReportListFilter.NON_GUIDED -> stringResource(R.string.report_list_filter_non_guided)
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ReportListItemCard(
    report: SiteReportListItem,
    isRetrying: Boolean,
    onOpenDraft: (String) -> Unit,
    onRetryFailedSync: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = report.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.label_revision, report.revision),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.label_updated_at, formatEpoch(report.updatedAtEpochMillis)),
                style = MaterialTheme.typography.bodySmall
            )
            SyncStateChip(syncState = report.syncState)
            report.closureSummary?.let { summary ->
                ReportClosureSummaryRow(summary = summary)
            }
            if (report.syncState == ReportSyncState.FAILED) {
                ReportFailureTrace(
                    lastAttemptAtEpochMillis = report.syncTrace.lastAttemptAtEpochMillis,
                    retryCount = report.syncTrace.retryCount,
                    failureReason = report.syncTrace.failureReason
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenDraft(report.draftId) }
                ) {
                    Text(stringResource(R.string.action_open_draft))
                }

                if (report.syncState == ReportSyncState.FAILED) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !isRetrying,
                        onClick = { onRetryFailedSync(report.draftId) }
                    ) {
                        Text(
                            if (isRetrying) {
                                stringResource(R.string.action_retry_sync_loading)
                            } else {
                                stringResource(R.string.action_retry_sync)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportClosureSummaryRow(summary: ReportListClosureSummary) {
    val workflowLabel = when (summary) {
        is ReportListClosureSummary.Xfeeder -> stringResource(R.string.report_list_closure_workflow_xfeeder)
        is ReportListClosureSummary.Ret -> stringResource(R.string.report_list_closure_workflow_ret)
        is ReportListClosureSummary.Throughput -> stringResource(
            R.string.report_list_closure_workflow_performance,
            stringResource(performanceWorkflowTypeLabelRes(com.quartz.platform.domain.model.PerformanceWorkflowType.THROUGHPUT))
        )
        is ReportListClosureSummary.Qos -> stringResource(
            R.string.report_list_closure_workflow_performance,
            stringResource(performanceWorkflowTypeLabelRes(com.quartz.platform.domain.model.PerformanceWorkflowType.QOS_SCRIPT))
        )
    }
    val resultLabel = when (summary) {
        is ReportListClosureSummary.Xfeeder -> {
            stringResource(xfeederSectorOutcomeLabelRes(summary.sectorOutcome))
        }

        is ReportListClosureSummary.Ret -> {
            stringResource(retResultOutcomeLabelRes(summary.resultOutcome))
        }

        is ReportListClosureSummary.Throughput -> {
            stringResource(
                performanceSessionStatusLabelRes(summary.sessionStatus)
            )
        }

        is ReportListClosureSummary.Qos -> {
            stringResource(
                performanceSessionStatusLabelRes(summary.sessionStatus)
            )
        }
    }
    val signalLabel = when (summary) {
        is ReportListClosureSummary.Xfeeder -> {
            when (summary.signal) {
                XfeederSignal.RELATED_SECTOR -> stringResource(R.string.report_list_closure_signal_related_sector)
                XfeederSignal.UNRELIABLE -> stringResource(R.string.report_list_closure_signal_unreliable)
                XfeederSignal.OBSERVED_MULTIPLE -> stringResource(R.string.report_list_closure_signal_observed_multiple)
                null -> null
            }
        }

        is ReportListClosureSummary.Ret -> {
            stringResource(
                R.string.report_list_closure_signal_ret_steps,
                summary.completedRequiredStepCount,
                summary.requiredStepCount
            )
        }

        is ReportListClosureSummary.Throughput -> {
            stringResource(
                R.string.report_list_closure_signal_performance_throughput,
                summary.completedRequiredStepCount,
                summary.requiredStepCount,
                if (summary.preconditionsReady) {
                    stringResource(R.string.value_yes)
                } else {
                    stringResource(R.string.value_no)
                },
                summary.downloadMbps?.toString() ?: "-",
                summary.uploadMbps?.toString() ?: "-",
                summary.latencyMs?.toString() ?: "-"
            )
        }

        is ReportListClosureSummary.Qos -> {
            val baseSignal = stringResource(
                R.string.report_list_closure_signal_performance_qos,
                summary.completedRequiredStepCount,
                summary.requiredStepCount,
                if (summary.preconditionsReady) {
                    stringResource(R.string.value_yes)
                } else {
                    stringResource(R.string.value_no)
                },
                summary.scriptName ?: stringResource(R.string.value_not_available),
                summary.configuredRepeatCount?.toString() ?: "-",
                summary.targetTechnology ?: stringResource(R.string.value_not_available),
                summary.configuredTechnologyCount,
                if (summary.targetTechnologyAligned) {
                    stringResource(R.string.value_yes)
                } else {
                    stringResource(R.string.value_no)
                },
                summary.testFamilyCount,
                summary.completedFamilyCount,
                summary.failedFamilyCount,
                summary.blockedFamilyCount,
                summary.timelineEventCount,
                summary.timelineFamilyCoverageCount,
                summary.requiredRepeatCount,
                summary.familiesMeetingRequiredRepeatCount,
                summary.passFailRunCount,
                summary.blockedRunCount,
                summary.plannedRunCount,
                summary.pendingRunCount,
                stringResource(qosEngineStateLabelRes(summary.executionEngineState)),
                stringResource(qosRecoveryStateLabelRes(summary.recoveryState)),
                summary.checkpointCount,
                summary.activeFamily?.let { family ->
                    stringResource(qosTestFamilyLabelRes(family))
                } ?: stringResource(R.string.value_not_available),
                summary.activeRepetitionIndex?.toString() ?: "-",
                summary.nextFamily?.let { family ->
                    stringResource(qosTestFamilyLabelRes(family))
                } ?: stringResource(R.string.value_not_available),
                summary.nextRepetitionIndex?.toString() ?: "-",
                summary.iterationCount,
                summary.successCount,
                summary.failureCount
            )
            summary.dominantIssueCode?.let { code ->
                "$baseSignal · ${
                    stringResource(
                        R.string.report_list_closure_signal_performance_qos_reason,
                        stringResource(qosIssueCodeLabelRes(code))
                    )
                }"
            } ?: baseSignal
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.report_list_closure_summary_title),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.report_list_closure_summary_line,
                    workflowLabel,
                    resultLabel
                ),
                style = MaterialTheme.typography.bodySmall
            )
            signalLabel?.let { signal ->
                Text(
                    text = stringResource(R.string.report_list_closure_summary_signal, signal),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ReportFailureTrace(
    lastAttemptAtEpochMillis: Long?,
    retryCount: Int,
    failureReason: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lastAttemptAtEpochMillis?.let {
            Text(
                text = stringResource(R.string.label_last_attempt, formatEpoch(it)),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = stringResource(R.string.label_retry_count, retryCount),
            style = MaterialTheme.typography.bodySmall
        )
        if (!failureReason.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.label_local_failure_reason, failureReason),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
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
        QosExecutionIssueCode.THRESHOLD_NOT_MET -> R.string.qos_issue_code_threshold_not_met
        QosExecutionIssueCode.OPERATOR_ABORTED -> R.string.qos_issue_code_operator_aborted
        QosExecutionIssueCode.UNKNOWN -> R.string.qos_issue_code_unknown
    }
}
