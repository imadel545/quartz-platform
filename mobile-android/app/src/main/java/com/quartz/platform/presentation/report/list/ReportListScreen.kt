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
import androidx.compose.material3.Button
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
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.model.QosExecutionEngineState
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import com.quartz.platform.domain.model.ReportListClosureSummary
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.SiteReportListItem
import com.quartz.platform.domain.model.XfeederSignal
import com.quartz.platform.presentation.components.MissionHeaderCard
import com.quartz.platform.presentation.components.OperationalEmptyStateCard
import com.quartz.platform.presentation.components.OperationalMessageCard
import com.quartz.platform.presentation.components.OperationalMetric
import com.quartz.platform.presentation.components.OperationalMetricRow
import com.quartz.platform.presentation.components.OperationalSectionCard
import com.quartz.platform.presentation.components.OperationalSeverity
import com.quartz.platform.presentation.components.OperationalSignal
import com.quartz.platform.presentation.components.OperationalSignalRow
import com.quartz.platform.presentation.performance.session.performanceWorkflowTypeLabelRes
import com.quartz.platform.presentation.ret.session.retResultOutcomeLabelRes
import com.quartz.platform.presentation.sync.syncStateLabelRes
import com.quartz.platform.presentation.xfeeder.session.xfeederSectorOutcomeLabelRes
import java.time.Duration
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
    val rankedReports = state.filteredReports.sortedWith(
        compareByDescending<SiteReportListItem> { reportPriorityScore(it) }
            .thenByDescending { it.updatedAtEpochMillis }
    )
    val topPriorityReport = rankedReports.firstOrNull()

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
                    OperationalEmptyStateCard(
                        title = stringResource(R.string.report_list_error_title),
                        message = state.errorMessage
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
                    OperationalEmptyStateCard(
                        title = stringResource(R.string.empty_local_reports_for_site),
                        message = stringResource(R.string.empty_local_reports_for_site_hint)
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
                        ReportListMissionHeader(
                            state = state,
                            rankedReports = rankedReports
                        )
                    }

                    topPriorityReport?.let { report ->
                        item {
                            ReportListPriorityCard(
                                report = report,
                                isRetrying = report.draftId in state.retryingDraftIds,
                                onOpenDraft = onOpenDraft,
                                onRetryFailedSync = onRetryFailedSync
                            )
                        }
                    }

                    item {
                        OperationalSectionCard(
                            title = stringResource(R.string.report_list_queue_title),
                            subtitle = stringResource(R.string.report_list_queue_hint)
                        ) {
                            ReportListFilterRow(
                                selectedFilter = state.selectedFilter,
                                onFilterSelected = onFilterSelected
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

                    state.errorMessage?.let { error ->
                        item {
                            OperationalMessageCard(
                                title = stringResource(R.string.home_runtime_alert_title),
                                message = error,
                                severity = OperationalSeverity.CRITICAL
                            )
                        }
                    }

                    if (state.isFilterEmpty) {
                        item {
                            OperationalEmptyStateCard(
                                title = stringResource(R.string.report_list_empty_filter_title),
                                message = stringResource(R.string.empty_filtered_local_reports)
                            )
                        }
                    } else {
                        items(rankedReports, key = { it.draftId }) { report ->
                            ReportQueueItemCard(
                                report = report,
                                isRetrying = report.draftId in state.retryingDraftIds,
                                onOpenDraft = onOpenDraft,
                                onRetryFailedSync = onRetryFailedSync
                            )
                        }
                    }

                    item {
                        OutlinedButton(
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
private fun ReportListMissionHeader(
    state: ReportListUiState,
    rankedReports: List<SiteReportListItem>
) {
    val failedSyncCount = rankedReports.count { it.syncState == ReportSyncState.FAILED }
    val guidedCount = rankedReports.count { it.originWorkflowType != null }
    val attentionCount = rankedReports.count(::reportNeedsAttention)
    MissionHeaderCard(
        title = stringResource(R.string.report_list_mission_title),
        subtitle = stringResource(R.string.label_site_id, state.siteId),
        signals = listOf(
            OperationalSignal(
                text = stringResource(
                    R.string.report_list_selected_filter,
                    reportFilterLabel(state.selectedFilter)
                ),
                severity = OperationalSeverity.NORMAL
            )
        ),
        metrics = listOf(
            OperationalMetric(
                value = rankedReports.size.toString(),
                label = stringResource(R.string.report_list_metric_visible),
                severity = OperationalSeverity.NORMAL
            ),
            OperationalMetric(
                value = guidedCount.toString(),
                label = stringResource(R.string.report_list_metric_guided),
                severity = if (guidedCount > 0) OperationalSeverity.SUCCESS else OperationalSeverity.NORMAL
            ),
            OperationalMetric(
                value = attentionCount.toString(),
                label = stringResource(R.string.report_list_metric_attention),
                severity = if (attentionCount > 0) OperationalSeverity.WARNING else OperationalSeverity.SUCCESS
            ),
            OperationalMetric(
                value = failedSyncCount.toString(),
                label = stringResource(R.string.report_list_metric_sync_failed),
                severity = if (failedSyncCount > 0) OperationalSeverity.CRITICAL else OperationalSeverity.SUCCESS
            )
        )
    ) {
        Text(
            text = stringResource(
                R.string.report_list_mission_summary,
                rankedReports.size,
                state.reports.size
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReportListPriorityCard(
    report: SiteReportListItem,
    isRetrying: Boolean,
    onOpenDraft: (String) -> Unit,
    onRetryFailedSync: (String) -> Unit
) {
    OperationalSectionCard(
        title = stringResource(R.string.report_list_priority_title),
        subtitle = stringResource(R.string.report_list_priority_hint)
    ) {
        ReportQueueCardBody(
            report = report,
            isRetrying = isRetrying,
            onOpenDraft = onOpenDraft,
            onRetryFailedSync = onRetryFailedSync,
            emphasizeTitle = true
        )
    }
}

@Composable
private fun ReportQueueItemCard(
    report: SiteReportListItem,
    isRetrying: Boolean,
    onOpenDraft: (String) -> Unit,
    onRetryFailedSync: (String) -> Unit
) {
    OperationalSectionCard(
        title = report.title,
        subtitle = stringResource(
            R.string.report_list_card_subtitle,
            reportWorkflowLabel(report),
            report.revision,
            formatEpoch(report.updatedAtEpochMillis),
            formatRelativeAge(report.updatedAtEpochMillis)
        )
    ) {
        ReportQueueCardBody(
            report = report,
            isRetrying = isRetrying,
            onOpenDraft = onOpenDraft,
            onRetryFailedSync = onRetryFailedSync,
            emphasizeTitle = false
        )
    }
}

@Composable
private fun ReportQueueCardBody(
    report: SiteReportListItem,
    isRetrying: Boolean,
    onOpenDraft: (String) -> Unit,
    onRetryFailedSync: (String) -> Unit,
    emphasizeTitle: Boolean
) {
    val dominantIssue = reportDominantIssue(report)
    val nextAction = reportNextAction(report)
    val issueSeverity = reportSeverity(report)

    if (emphasizeTitle) {
        Text(
            text = report.title,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(
                R.string.report_list_card_subtitle,
                reportWorkflowLabel(report),
                report.revision,
                formatEpoch(report.updatedAtEpochMillis),
                formatRelativeAge(report.updatedAtEpochMillis)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    OperationalSignalRow(
        signals = listOf(
            OperationalSignal(
                text = stringResource(
                    R.string.label_sync_state,
                    stringResource(syncStateLabelRes(report.syncState))
                ),
                severity = syncStateSeverity(report.syncState)
            ),
            OperationalSignal(
                text = reportWorkflowLabel(report),
                severity = if (report.originWorkflowType != null) {
                    OperationalSeverity.SUCCESS
                } else {
                    OperationalSeverity.NORMAL
                }
            ),
            OperationalSignal(
                text = dominantIssue,
                severity = issueSeverity
            )
        ),
        maxVisibleSignals = 3
    )

    Text(
        text = stringResource(R.string.report_list_issue_line, dominantIssue),
        style = MaterialTheme.typography.bodyMedium,
        color = if (issueSeverity == OperationalSeverity.CRITICAL) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    )
    Text(
        text = stringResource(R.string.report_list_next_action_line, nextAction),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    OperationalMetricRow(metrics = reportMetrics(report))

    reportFailureTraceMessage(report)?.let { failureTrace ->
        Text(
            text = failureTrace,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
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

@Composable
private fun ReportListFilterRow(
    selectedFilter: ReportListFilter,
    onFilterSelected: (ReportListFilter) -> Unit
) {
    val filters = ReportListFilter.values().toList()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filters) { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = { Text(reportFilterLabel(filter)) }
            )
        }
    }
}

@Composable
private fun reportFilterLabel(filter: ReportListFilter): String {
    return when (filter) {
        ReportListFilter.ALL -> stringResource(R.string.report_list_filter_all)
        ReportListFilter.XFEEDER -> stringResource(R.string.report_list_filter_xfeeder)
        ReportListFilter.RET -> stringResource(R.string.report_list_filter_ret)
        ReportListFilter.PERFORMANCE -> stringResource(R.string.report_list_filter_performance)
        ReportListFilter.NON_GUIDED -> stringResource(R.string.report_list_filter_non_guided)
    }
}

@Composable
private fun reportWorkflowLabel(report: SiteReportListItem): String {
    return when (val summary = report.closureSummary) {
        is ReportListClosureSummary.Xfeeder -> stringResource(R.string.report_list_closure_workflow_xfeeder)
        is ReportListClosureSummary.Ret -> stringResource(R.string.report_list_closure_workflow_ret)
        is ReportListClosureSummary.Throughput -> stringResource(
            R.string.report_list_closure_workflow_performance,
            stringResource(performanceWorkflowTypeLabelRes(PerformanceWorkflowType.THROUGHPUT))
        )
        is ReportListClosureSummary.Qos -> stringResource(
            R.string.report_list_closure_workflow_performance,
            stringResource(performanceWorkflowTypeLabelRes(PerformanceWorkflowType.QOS_SCRIPT))
        )
        null -> when (report.originWorkflowType) {
            ReportDraftOriginWorkflowType.XFEEDER -> stringResource(R.string.report_list_filter_xfeeder)
            ReportDraftOriginWorkflowType.RET -> stringResource(R.string.report_list_filter_ret)
            ReportDraftOriginWorkflowType.PERFORMANCE -> stringResource(R.string.report_list_filter_performance)
            null -> stringResource(R.string.report_list_workflow_non_guided)
        }
    }
}

@Composable
private fun reportDominantIssue(report: SiteReportListItem): String {
    if (report.syncState == ReportSyncState.FAILED) {
        return stringResource(R.string.report_list_issue_sync_failed)
    }
    return when (val summary = report.closureSummary) {
        is ReportListClosureSummary.Qos -> when {
            summary.failedFamilyCount > 0 || summary.blockedFamilyCount > 0 ->
                stringResource(R.string.report_list_issue_qos_execution)
            summary.dominantIssueCode != null || !summary.preconditionsReady ->
                stringResource(R.string.report_list_issue_qos_preflight)
            summary.pendingRunCount > 0 ->
                stringResource(R.string.report_list_issue_guided_follow_up)
            else -> stringResource(R.string.report_list_issue_ready_for_review)
        }
        is ReportListClosureSummary.Throughput -> {
            if (!summary.preconditionsReady) {
                stringResource(R.string.report_list_issue_performance_preflight)
            } else {
                stringResource(R.string.report_list_issue_ready_for_review)
            }
        }
        is ReportListClosureSummary.Ret -> {
            if (summary.completedRequiredStepCount < summary.requiredStepCount) {
                stringResource(R.string.report_list_issue_guided_follow_up)
            } else {
                stringResource(R.string.report_list_issue_ready_for_review)
            }
        }
        is ReportListClosureSummary.Xfeeder -> {
            when (summary.signal) {
                XfeederSignal.RELATED_SECTOR,
                XfeederSignal.UNRELIABLE,
                XfeederSignal.OBSERVED_MULTIPLE -> stringResource(R.string.report_list_issue_xfeeder_signal)
                null -> stringResource(R.string.report_list_issue_ready_for_review)
            }
        }
        null -> when (report.syncState) {
            ReportSyncState.PENDING -> stringResource(R.string.report_list_issue_sync_pending)
            ReportSyncState.LOCAL_ONLY -> stringResource(R.string.report_list_issue_local_review)
            ReportSyncState.SYNCED -> stringResource(R.string.report_list_issue_ready_for_review)
            ReportSyncState.FAILED -> stringResource(R.string.report_list_issue_sync_failed)
        }
    }
}

@Composable
private fun reportNextAction(report: SiteReportListItem): String {
    return when {
        report.syncState == ReportSyncState.FAILED -> {
            stringResource(R.string.report_list_next_action_retry_sync)
        }
        report.originWorkflowType != null -> {
            stringResource(R.string.report_list_next_action_open_guided)
        }
        else -> {
            stringResource(R.string.report_list_next_action_open_draft)
        }
    }
}

@Composable
private fun reportMetrics(report: SiteReportListItem): List<OperationalMetric> {
    val baseMetrics = mutableListOf(
        OperationalMetric(
            value = report.revision.toString(),
            label = stringResource(R.string.report_list_metric_revision),
            severity = OperationalSeverity.NORMAL
        )
    )
    when (val summary = report.closureSummary) {
        is ReportListClosureSummary.Xfeeder -> {
            baseMetrics += OperationalMetric(
                value = stringResource(xfeederSectorOutcomeLabelRes(summary.sectorOutcome)),
                label = stringResource(R.string.report_list_metric_result),
                severity = if (summary.signal == null) {
                    OperationalSeverity.SUCCESS
                } else {
                    OperationalSeverity.WARNING
                }
            )
        }
        is ReportListClosureSummary.Ret -> {
            baseMetrics += OperationalMetric(
                value = "${summary.completedRequiredStepCount}/${summary.requiredStepCount}",
                label = stringResource(R.string.report_list_metric_required_steps),
                severity = if (summary.completedRequiredStepCount == summary.requiredStepCount) {
                    OperationalSeverity.SUCCESS
                } else {
                    OperationalSeverity.WARNING
                }
            )
            baseMetrics += OperationalMetric(
                value = stringResource(retResultOutcomeLabelRes(summary.resultOutcome)),
                label = stringResource(R.string.report_list_metric_result),
                severity = OperationalSeverity.NORMAL
            )
        }
        is ReportListClosureSummary.Throughput -> {
            baseMetrics += OperationalMetric(
                value = "${summary.completedRequiredStepCount}/${summary.requiredStepCount}",
                label = stringResource(R.string.report_list_metric_required_steps),
                severity = if (summary.completedRequiredStepCount == summary.requiredStepCount) {
                    OperationalSeverity.SUCCESS
                } else {
                    OperationalSeverity.WARNING
                }
            )
            baseMetrics += OperationalMetric(
                value = if (summary.preconditionsReady) {
                    stringResource(R.string.value_yes)
                } else {
                    stringResource(R.string.value_no)
                },
                label = stringResource(R.string.report_list_metric_preflight),
                severity = if (summary.preconditionsReady) {
                    OperationalSeverity.SUCCESS
                } else {
                    OperationalSeverity.WARNING
                }
            )
        }
        is ReportListClosureSummary.Qos -> {
            baseMetrics += OperationalMetric(
                value = "${summary.completedFamilyCount}/${summary.testFamilyCount}",
                label = stringResource(R.string.report_list_metric_families),
                severity = if (summary.failedFamilyCount == 0 && summary.blockedFamilyCount == 0) {
                    OperationalSeverity.SUCCESS
                } else {
                    OperationalSeverity.WARNING
                }
            )
            baseMetrics += OperationalMetric(
                value = "${summary.familiesMeetingRequiredRepeatCount}/${summary.requiredRepeatCount}",
                label = stringResource(R.string.report_list_metric_repeat_coverage),
                severity = if (summary.pendingRunCount == 0) {
                    OperationalSeverity.SUCCESS
                } else {
                    OperationalSeverity.WARNING
                }
            )
        }
        null -> Unit
    }
    if (report.syncState == ReportSyncState.FAILED) {
        baseMetrics += OperationalMetric(
            value = report.syncTrace.retryCount.toString(),
            label = stringResource(R.string.report_list_metric_retries),
            severity = OperationalSeverity.CRITICAL
        )
    }
    return baseMetrics.take(3)
}

private fun reportFailureTraceMessage(report: SiteReportListItem): String? {
    if (report.syncState != ReportSyncState.FAILED) return null
    val parts = buildList {
        report.syncTrace.lastAttemptAtEpochMillis?.let {
            add("Dernière tentative ${formatEpoch(it)}")
        }
        add("Retries ${report.syncTrace.retryCount}")
        report.syncTrace.failureReason?.takeIf { it.isNotBlank() }?.let { reason ->
            add(reason)
        }
    }
    return parts.joinToString(" • ")
}

private fun reportPriorityScore(report: SiteReportListItem): Int {
    var score = 0
    when (report.syncState) {
        ReportSyncState.FAILED -> score += 400
        ReportSyncState.PENDING -> score += 180
        ReportSyncState.LOCAL_ONLY -> score += 90
        ReportSyncState.SYNCED -> Unit
    }
    when (val summary = report.closureSummary) {
        is ReportListClosureSummary.Qos -> {
            if (summary.failedFamilyCount > 0) score += 220
            if (summary.blockedFamilyCount > 0) score += 200
            if (!summary.preconditionsReady) score += 180
            if (summary.dominantIssueCode != null) score += 160
            if (summary.pendingRunCount > 0) score += 100
        }
        is ReportListClosureSummary.Throughput -> {
            if (!summary.preconditionsReady) score += 120
            if (summary.completedRequiredStepCount < summary.requiredStepCount) score += 80
        }
        is ReportListClosureSummary.Ret -> {
            if (summary.completedRequiredStepCount < summary.requiredStepCount) score += 110
        }
        is ReportListClosureSummary.Xfeeder -> {
            if (summary.signal != null) score += 100
        }
        null -> Unit
    }
    if (report.originWorkflowType != null) {
        score += 40
    }
    return score
}

private fun reportNeedsAttention(report: SiteReportListItem): Boolean {
    if (report.syncState == ReportSyncState.FAILED) return true
    return when (val summary = report.closureSummary) {
        is ReportListClosureSummary.Qos -> {
            summary.failedFamilyCount > 0 ||
                summary.blockedFamilyCount > 0 ||
                !summary.preconditionsReady ||
                summary.dominantIssueCode != null
        }
        is ReportListClosureSummary.Throughput -> !summary.preconditionsReady
        is ReportListClosureSummary.Ret -> summary.completedRequiredStepCount < summary.requiredStepCount
        is ReportListClosureSummary.Xfeeder -> summary.signal != null
        null -> report.syncState == ReportSyncState.PENDING
    }
}

@Composable
private fun reportSeverity(report: SiteReportListItem): OperationalSeverity {
    return when {
        report.syncState == ReportSyncState.FAILED -> OperationalSeverity.CRITICAL
        report.closureSummary is ReportListClosureSummary.Qos && isCriticalQos(report.closureSummary) ->
            OperationalSeverity.CRITICAL
        reportNeedsAttention(report) -> OperationalSeverity.WARNING
        report.originWorkflowType != null -> OperationalSeverity.SUCCESS
        else -> OperationalSeverity.NORMAL
    }
}

private fun isCriticalQos(summary: ReportListClosureSummary.Qos): Boolean {
    return summary.failedFamilyCount > 0 ||
        summary.blockedFamilyCount > 0 ||
        summary.executionEngineState in setOf(
            QosExecutionEngineState.FAILED,
            QosExecutionEngineState.BLOCKED,
            QosExecutionEngineState.PREFLIGHT_BLOCKED
        )
}

private fun syncStateSeverity(syncState: ReportSyncState): OperationalSeverity {
    return when (syncState) {
        ReportSyncState.LOCAL_ONLY -> OperationalSeverity.NORMAL
        ReportSyncState.PENDING -> OperationalSeverity.WARNING
        ReportSyncState.SYNCED -> OperationalSeverity.SUCCESS
        ReportSyncState.FAILED -> OperationalSeverity.CRITICAL
    }
}

private fun formatEpoch(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return formatter.format(
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    )
}

private fun formatRelativeAge(epochMillis: Long): String {
    val age = Duration.between(Instant.ofEpochMilli(epochMillis), Instant.now()).toHours()
    return when {
        age < 1 -> "<1h"
        age < 24 -> "${age}h"
        else -> "${age / 24}j"
    }
}
