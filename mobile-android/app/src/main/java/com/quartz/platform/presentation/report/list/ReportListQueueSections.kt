package com.quartz.platform.presentation.report.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quartz.platform.R
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.model.QosExecutionEngineState
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import com.quartz.platform.domain.model.ReportListClosureSummary
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.SiteReportListItem
import com.quartz.platform.domain.model.XfeederSignal
import com.quartz.platform.presentation.components.MissionHeaderCard
import com.quartz.platform.presentation.components.MissionPrimaryActionBar
import com.quartz.platform.presentation.components.MissionPrimaryActionButton
import com.quartz.platform.presentation.components.OperationalMetric
import com.quartz.platform.presentation.components.OperationalMetricRow
import com.quartz.platform.presentation.components.OperationalSectionCard
import com.quartz.platform.presentation.components.OperationalSeverity
import com.quartz.platform.presentation.components.OperationalSignal
import com.quartz.platform.presentation.components.OperationalSignalRow
import com.quartz.platform.presentation.components.OperationalStateBanner
import com.quartz.platform.presentation.performance.session.performanceWorkflowTypeLabelRes
import com.quartz.platform.presentation.ret.session.retResultOutcomeLabelRes
import com.quartz.platform.presentation.sync.syncStateLabelRes
import com.quartz.platform.presentation.xfeeder.session.xfeederSectorOutcomeLabelRes
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun ReportListMissionHeader(
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
internal fun ReportListRuntimeStateBanner(
    rankedReports: List<SiteReportListItem>,
    selectedFilter: ReportListFilter
) {
    val failedSyncCount = rankedReports.count { it.syncState == ReportSyncState.FAILED }
    val attentionCount = rankedReports.count(::reportNeedsAttention)
    val hasCritical = failedSyncCount > 0 || rankedReports.any {
        it.closureSummary is ReportListClosureSummary.Qos && isCriticalQos(it.closureSummary)
    }
    val severity = when {
        hasCritical -> OperationalSeverity.CRITICAL
        attentionCount > 0 -> OperationalSeverity.WARNING
        else -> OperationalSeverity.SUCCESS
    }
    val message = when {
        hasCritical -> stringResource(
            R.string.report_list_runtime_state_message_critical,
            failedSyncCount,
            attentionCount
        )
        attentionCount > 0 -> stringResource(
            R.string.report_list_runtime_state_message_attention,
            attentionCount
        )
        else -> stringResource(R.string.report_list_runtime_state_message_clear)
    }
    OperationalStateBanner(
        title = stringResource(R.string.report_list_runtime_state_title),
        message = message,
        hint = stringResource(
            R.string.report_list_runtime_state_hint,
            reportFilterLabel(selectedFilter)
        ),
        severity = severity
    )
}

@Composable
internal fun ReportListQueueControlsCard(
    selectedFilter: ReportListFilter,
    queueSize: Int,
    onFilterSelected: (ReportListFilter) -> Unit
) {
    OperationalSectionCard(
        title = stringResource(R.string.report_list_queue_title),
        subtitle = stringResource(R.string.report_list_queue_hint)
    ) {
        OperationalSignalRow(
            signals = listOf(
                OperationalSignal(
                    text = stringResource(
                        R.string.report_list_selected_filter,
                        reportFilterLabel(selectedFilter)
                    ),
                    severity = OperationalSeverity.NORMAL
                ),
                OperationalSignal(
                    text = stringResource(R.string.report_list_queue_visible_count, queueSize),
                    severity = if (queueSize > 0) OperationalSeverity.WARNING else OperationalSeverity.SUCCESS
                )
            )
        )
        ReportListFilterRow(
            selectedFilter = selectedFilter,
            onFilterSelected = onFilterSelected
        )
    }
}

@Composable
internal fun ReportListFocusCard(
    report: SiteReportListItem,
    isRetrying: Boolean,
    onOpenDraft: (String) -> Unit,
    onRetryFailedSync: (String) -> Unit
) {
    val dominantIssue = reportDominantIssue(report)
    val nextAction = reportNextAction(report)
    val severity = reportSeverity(report)

    OperationalSectionCard(
        title = stringResource(R.string.report_list_priority_title),
        subtitle = stringResource(R.string.report_list_priority_hint)
    ) {
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
        OperationalSignalRow(
            signals = listOf(
                OperationalSignal(
                    text = stringResource(R.string.report_list_issue_line, dominantIssue),
                    severity = severity
                ),
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
                )
            )
        )
        OperationalMetricRow(metrics = reportMetrics(report))
        Text(
            text = stringResource(R.string.report_list_next_action_line, nextAction),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        MissionPrimaryActionBar(
            primaryAction = {
                MissionPrimaryActionButton(
                    label = stringResource(R.string.action_open_draft),
                    onClick = { onOpenDraft(report.draftId) }
                )
            },
            secondaryAction = if (report.syncState == ReportSyncState.FAILED) {
                {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
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
            } else {
                null
            }
        )
    }
}

@Composable
internal fun ReportQueueSectionCard(
    report: SiteReportListItem,
    isRetrying: Boolean,
    onOpenDraft: (String) -> Unit,
    onRetryFailedSync: (String) -> Unit
) {
    val dominantIssue = reportDominantIssue(report)
    val severity = reportSeverity(report)

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
        OperationalSignalRow(
            signals = listOf(
                OperationalSignal(
                    text = dominantIssue,
                    severity = severity
                ),
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
                )
            )
        )
        OperationalMetricRow(metrics = reportMetrics(report))

        reportFailureTraceMessage(report)?.let { failureTrace ->
            Text(
                text = failureTrace,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        MissionPrimaryActionBar(
            primaryAction = {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenDraft(report.draftId) }
                ) {
                    Text(stringResource(R.string.action_open_draft))
                }
            },
            secondaryAction = if (report.syncState == ReportSyncState.FAILED) {
                {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
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
            } else {
                null
            }
        )
    }
}

@Composable
internal fun ReportListFilterRow(
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
internal fun reportFilterLabel(filter: ReportListFilter): String {
    return when (filter) {
        ReportListFilter.ALL -> stringResource(R.string.report_list_filter_all)
        ReportListFilter.XFEEDER -> stringResource(R.string.report_list_filter_xfeeder)
        ReportListFilter.RET -> stringResource(R.string.report_list_filter_ret)
        ReportListFilter.PERFORMANCE -> stringResource(R.string.report_list_filter_performance)
        ReportListFilter.NON_GUIDED -> stringResource(R.string.report_list_filter_non_guided)
    }
}

@Composable
internal fun reportWorkflowLabel(report: SiteReportListItem): String {
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
internal fun reportDominantIssue(report: SiteReportListItem): String {
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
internal fun reportNextAction(report: SiteReportListItem): String {
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
internal fun reportMetrics(report: SiteReportListItem): List<OperationalMetric> {
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

internal fun reportFailureTraceMessage(report: SiteReportListItem): String? {
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

internal fun reportPriorityScore(report: SiteReportListItem): Int {
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

internal fun reportNeedsAttention(report: SiteReportListItem): Boolean {
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
internal fun reportSeverity(report: SiteReportListItem): OperationalSeverity {
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

internal fun syncStateSeverity(syncState: ReportSyncState): OperationalSeverity {
    return when (syncState) {
        ReportSyncState.LOCAL_ONLY -> OperationalSeverity.NORMAL
        ReportSyncState.PENDING -> OperationalSeverity.WARNING
        ReportSyncState.SYNCED -> OperationalSeverity.SUCCESS
        ReportSyncState.FAILED -> OperationalSeverity.CRITICAL
    }
}

internal fun formatEpoch(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return formatter.format(
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    )
}

internal fun formatRelativeAge(epochMillis: Long): String {
    val age = Duration.between(Instant.ofEpochMilli(epochMillis), Instant.now()).toHours()
    return when {
        age < 1 -> "<1h"
        age < 24 -> "${age}h"
        else -> "${age / 24}j"
    }
}
