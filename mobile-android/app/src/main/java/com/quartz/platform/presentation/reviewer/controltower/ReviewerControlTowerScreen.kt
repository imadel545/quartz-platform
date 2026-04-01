package com.quartz.platform.presentation.reviewer.controltower

import androidx.compose.foundation.layout.Arrangement
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
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.ReviewerAttentionSignal
import com.quartz.platform.domain.model.ReviewerControlTowerGroupKey
import com.quartz.platform.domain.model.ReviewerControlTowerItem
import com.quartz.platform.presentation.sync.syncStateLabelRes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ReviewerControlTowerRoute(
    onBack: () -> Unit,
    onOpenDraft: (String) -> Unit,
    onOpenSite: (String) -> Unit,
    viewModel: ReviewerControlTowerViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ReviewerControlTowerEvent.OpenDraft -> onOpenDraft(event.draftId)
                is ReviewerControlTowerEvent.OpenSite -> onOpenSite(event.siteId)
            }
        }
    }

    ReviewerControlTowerScreen(
        state = state,
        onBack = onBack,
        onFilterSelected = viewModel::onFilterSelected,
        onGroupingSelected = viewModel::onGroupingSelected,
        onPresetSelected = viewModel::onPresetSelected,
        onOpenTopPriority = viewModel::onOpenTopPriorityClicked,
        onResetQueueProgress = viewModel::onResetQueueProgressClicked,
        onRetryFailedVisibleSync = viewModel::onRetryFailedVisibleSyncClicked,
        onOpenDraft = viewModel::onOpenDraftClicked,
        onOpenSite = viewModel::onOpenSiteClicked,
        onRetryDraftSync = viewModel::onRetryDraftSyncClicked
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewerControlTowerScreen(
    state: ReviewerControlTowerUiState,
    onBack: () -> Unit,
    onFilterSelected: (ReviewerControlTowerFilter) -> Unit,
    onGroupingSelected: (ReviewerControlTowerGrouping) -> Unit,
    onPresetSelected: (ReviewerQueuePreset) -> Unit,
    onOpenTopPriority: () -> Unit,
    onResetQueueProgress: () -> Unit,
    onRetryFailedVisibleSync: () -> Unit,
    onOpenDraft: (String) -> Unit,
    onOpenSite: (String) -> Unit,
    onRetryDraftSync: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_reviewer_control_tower)) }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
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
                        ControlTowerSummaryCard(state = state)
                    }

                    item {
                        ControlTowerActionRow(
                            state = state,
                            onOpenTopPriority = onOpenTopPriority,
                            onResetQueueProgress = onResetQueueProgress,
                            onRetryFailedVisibleSync = onRetryFailedVisibleSync
                        )
                    }

                    item {
                        ControlTowerFilterRow(
                            selectedFilter = state.selectedFilter,
                            onFilterSelected = onFilterSelected
                        )
                    }

                    item {
                        ControlTowerPresetRow(
                            selectedPreset = state.selectedPreset,
                            onPresetSelected = onPresetSelected
                        )
                    }

                    item {
                        ControlTowerGroupingRow(
                            selectedGrouping = state.selectedGrouping,
                            onGroupingSelected = onGroupingSelected
                        )
                    }

                    item {
                        ControlTowerMotifSection(
                            siteMotifs = state.siteMotifs,
                            workflowMotifs = state.workflowMotifs,
                            onOpenDraft = onOpenDraft
                        )
                    }

                    state.infoMessage?.let { info ->
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = info,
                                    modifier = Modifier.padding(12.dp),
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
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (state.activeQueueItems.isEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = stringResource(R.string.reviewer_control_tower_empty),
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        items(state.groupedItems, key = { group -> group.key.name }) { group ->
                            ControlTowerGroupSection(
                                group = group,
                                onOpenDraft = onOpenDraft,
                                onOpenSite = onOpenSite,
                                onRetryDraftSync = onRetryDraftSync,
                                retryingDraftIds = state.retryingDraftIds
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
private fun ControlTowerSummaryCard(state: ReviewerControlTowerUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.reviewer_control_tower_summary_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(
                    R.string.reviewer_control_tower_summary_counts,
                    state.summary.totalDraftCount,
                    state.summary.guidedDraftCount,
                    state.summary.nonGuidedDraftCount
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.reviewer_control_tower_summary_attention,
                    state.summary.attentionRequiredCount,
                    state.summary.syncFailedCount,
                    state.summary.syncPendingCount,
                    state.summary.qosRiskCount,
                    state.summary.staleDraftCount
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ControlTowerActionRow(
    state: ReviewerControlTowerUiState,
    onOpenTopPriority: () -> Unit,
    onResetQueueProgress: () -> Unit,
    onRetryFailedVisibleSync: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(
                R.string.reviewer_control_tower_queue_progress,
                state.activeQueueItems.size,
                state.queuedItems.size
            ),
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.weight(1f),
                enabled = state.queueTopDraftId != null,
                onClick = onOpenTopPriority
            ) {
                Text(stringResource(R.string.reviewer_control_tower_action_open_top_priority))
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = state.visibleSyncFailedCount > 0 && !state.isBulkRetryInProgress,
                onClick = onRetryFailedVisibleSync
            ) {
                Text(
                    if (state.isBulkRetryInProgress) {
                        stringResource(R.string.action_retry_sync_loading)
                    } else {
                        stringResource(
                            R.string.reviewer_control_tower_action_retry_visible_failed,
                            state.visibleSyncFailedCount
                        )
                    }
                )
            }
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = state.progressedDraftIds.isNotEmpty(),
            onClick = onResetQueueProgress
        ) {
            Text(stringResource(R.string.reviewer_control_tower_action_reset_queue_progress))
        }
    }
}

@Composable
private fun ControlTowerFilterRow(
    selectedFilter: ReviewerControlTowerFilter,
    onFilterSelected: (ReviewerControlTowerFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.reviewer_control_tower_filter_label),
            style = MaterialTheme.typography.bodyMedium
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ReviewerControlTowerFilter.entries) { filter ->
                FilterChip(
                    selected = filter == selectedFilter,
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            text = when (filter) {
                                ReviewerControlTowerFilter.ALL -> stringResource(R.string.reviewer_control_tower_filter_all)
                                ReviewerControlTowerFilter.NEEDS_ATTENTION -> stringResource(R.string.reviewer_control_tower_filter_attention)
                                ReviewerControlTowerFilter.SYNC_FAILED -> stringResource(R.string.reviewer_control_tower_filter_sync_failed)
                                ReviewerControlTowerFilter.QOS_RISK -> stringResource(R.string.reviewer_control_tower_filter_qos_risk)
                                ReviewerControlTowerFilter.GUIDED -> stringResource(R.string.reviewer_control_tower_filter_guided)
                                ReviewerControlTowerFilter.NON_GUIDED -> stringResource(R.string.reviewer_control_tower_filter_non_guided)
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ControlTowerPresetRow(
    selectedPreset: ReviewerQueuePreset,
    onPresetSelected: (ReviewerQueuePreset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.reviewer_control_tower_preset_label),
            style = MaterialTheme.typography.bodyMedium
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ReviewerQueuePreset.entries) { preset ->
                FilterChip(
                    selected = preset == selectedPreset,
                    onClick = { onPresetSelected(preset) },
                    label = {
                        Text(
                            text = when (preset) {
                                ReviewerQueuePreset.NEEDS_ATTENTION_NOW -> stringResource(R.string.reviewer_control_tower_preset_attention_now)
                                ReviewerQueuePreset.SYNC_FAILURES_FIRST -> stringResource(R.string.reviewer_control_tower_preset_sync_failures)
                                ReviewerQueuePreset.QOS_RISK_FIRST -> stringResource(R.string.reviewer_control_tower_preset_qos_risk)
                                ReviewerQueuePreset.STALE_GUIDED_WORK -> stringResource(R.string.reviewer_control_tower_preset_stale_guided)
                                ReviewerQueuePreset.GUIDED_UNRESOLVED -> stringResource(R.string.reviewer_control_tower_preset_guided_unresolved)
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ControlTowerMotifSection(
    siteMotifs: List<ReviewerQueueSiteMotif>,
    workflowMotifs: List<ReviewerQueueWorkflowMotif>,
    onOpenDraft: (String) -> Unit
) {
    if (siteMotifs.isEmpty() && workflowMotifs.isEmpty()) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.reviewer_control_tower_motif_title),
                style = MaterialTheme.typography.titleSmall
            )

            if (siteMotifs.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.reviewer_control_tower_motif_by_site),
                    style = MaterialTheme.typography.labelMedium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(siteMotifs) { motif ->
                        MotifCard(
                            title = "${motif.siteCode} (${motif.draftCount})",
                            subtitle = motif.dominantAttentionSignal?.let { signal ->
                                stringResource(attentionSignalLabelRes(signal))
                            },
                            onOpen = motif.topDraftId?.let { draftId -> { onOpenDraft(draftId) } }
                        )
                    }
                }
            }

            if (workflowMotifs.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.reviewer_control_tower_motif_by_workflow),
                    style = MaterialTheme.typography.labelMedium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(workflowMotifs) { motif ->
                        MotifCard(
                            title = "${workflowLabel(motif.workflowType)} (${motif.draftCount})",
                            subtitle = motif.dominantAttentionSignal?.let { signal ->
                                stringResource(attentionSignalLabelRes(signal))
                            },
                            onOpen = motif.topDraftId?.let { draftId -> { onOpenDraft(draftId) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MotifCard(
    title: String,
    subtitle: String?,
    onOpen: (() -> Unit)?
) {
    Card {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            subtitle?.let { Text(text = it, style = MaterialTheme.typography.labelSmall) }
            onOpen?.let {
                OutlinedButton(onClick = it) {
                    Text(stringResource(R.string.action_open_draft))
                }
            }
        }
    }
}

@Composable
private fun ControlTowerGroupingRow(
    selectedGrouping: ReviewerControlTowerGrouping,
    onGroupingSelected: (ReviewerControlTowerGrouping) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.reviewer_control_tower_grouping_label),
            style = MaterialTheme.typography.bodyMedium
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ReviewerControlTowerGrouping.entries) { grouping ->
                FilterChip(
                    selected = grouping == selectedGrouping,
                    onClick = { onGroupingSelected(grouping) },
                    label = {
                        Text(
                            text = when (grouping) {
                                ReviewerControlTowerGrouping.ATTENTION -> stringResource(R.string.reviewer_control_tower_grouping_attention)
                                ReviewerControlTowerGrouping.WORKFLOW -> stringResource(R.string.reviewer_control_tower_grouping_workflow)
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ControlTowerGroupSection(
    group: ReviewerControlTowerGroup,
    onOpenDraft: (String) -> Unit,
    onOpenSite: (String) -> Unit,
    onRetryDraftSync: (String) -> Unit,
    retryingDraftIds: Set<String>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = groupTitle(group.key),
                style = MaterialTheme.typography.titleSmall
            )
            group.items.forEach { item ->
                ReviewerControlTowerItemCard(
                    item = item,
                    isRetrying = item.draftId in retryingDraftIds,
                    onOpenDraft = onOpenDraft,
                    onOpenSite = onOpenSite,
                    onRetryDraftSync = onRetryDraftSync
                )
            }
        }
    }
}

@Composable
private fun ReviewerControlTowerItemCard(
    item: ReviewerControlTowerItem,
    isRetrying: Boolean,
    onOpenDraft: (String) -> Unit,
    onOpenSite: (String) -> Unit,
    onRetryDraftSync: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${item.siteCode} • ${item.siteName}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.label_updated_at, formatEpoch(item.updatedAtEpochMillis)),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.label_sync_state,
                    stringResource(syncStateLabelRes(item.syncTrace.state))
                ),
                style = MaterialTheme.typography.bodySmall
            )
            item.dominantAttentionSignal?.let { dominant ->
                Text(
                    text = stringResource(
                        R.string.reviewer_control_tower_dominant_signal,
                        stringResource(attentionSignalLabelRes(dominant))
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (ReviewerAttentionSignal.STALE_DRAFT in item.attentionSignals) {
                Text(
                    text = stringResource(R.string.reviewer_control_tower_stale_age, item.staleAgeHours),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            item.originWorkflowType?.let { workflow ->
                Text(
                    text = stringResource(
                        R.string.reviewer_control_tower_workflow,
                        workflowLabel(workflow)
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (item.attentionSignals.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(item.attentionSignals.toList().sortedByDescending(::attentionSignalSeverity)) { signal ->
                        Card {
                            Text(
                                text = stringResource(attentionSignalLabelRes(signal)),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenDraft(item.draftId) }
                ) {
                    Text(stringResource(R.string.action_open_draft))
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSite(item.siteId) }
                ) {
                    Text(stringResource(R.string.reviewer_control_tower_action_open_site))
                }
            }
            if (item.syncTrace.state == ReportSyncState.FAILED) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRetrying,
                    onClick = { onRetryDraftSync(item.draftId) }
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

@Composable
private fun groupTitle(key: ReviewerControlTowerGroupKey): String {
    return when (key) {
        ReviewerControlTowerGroupKey.SYNC_FAILED -> stringResource(R.string.reviewer_control_tower_group_sync_failed)
        ReviewerControlTowerGroupKey.QOS_RISK -> stringResource(R.string.reviewer_control_tower_group_qos_risk)
        ReviewerControlTowerGroupKey.SYNC_PENDING -> stringResource(R.string.reviewer_control_tower_group_sync_pending)
        ReviewerControlTowerGroupKey.STALE -> stringResource(R.string.reviewer_control_tower_group_stale)
        ReviewerControlTowerGroupKey.NO_ATTENTION -> stringResource(R.string.reviewer_control_tower_group_no_attention)
        ReviewerControlTowerGroupKey.WORKFLOW_XFEEDER -> stringResource(R.string.reviewer_control_tower_group_workflow_xfeeder)
        ReviewerControlTowerGroupKey.WORKFLOW_RET -> stringResource(R.string.reviewer_control_tower_group_workflow_ret)
        ReviewerControlTowerGroupKey.WORKFLOW_PERFORMANCE -> stringResource(R.string.reviewer_control_tower_group_workflow_performance)
        ReviewerControlTowerGroupKey.WORKFLOW_NON_GUIDED -> stringResource(R.string.reviewer_control_tower_group_workflow_non_guided)
    }
}

private fun attentionSignalLabelRes(signal: ReviewerAttentionSignal): Int {
    return when (signal) {
        ReviewerAttentionSignal.SYNC_FAILED -> R.string.reviewer_control_tower_signal_sync_failed
        ReviewerAttentionSignal.SYNC_PENDING -> R.string.reviewer_control_tower_signal_sync_pending
        ReviewerAttentionSignal.QOS_FAILED_OR_BLOCKED -> R.string.reviewer_control_tower_signal_qos_failed_or_blocked
        ReviewerAttentionSignal.QOS_PREREQUISITES_NOT_READY -> R.string.reviewer_control_tower_signal_qos_preflight_blocked
        ReviewerAttentionSignal.STALE_DRAFT -> R.string.reviewer_control_tower_signal_stale_draft
    }
}

private fun attentionSignalSeverity(signal: ReviewerAttentionSignal): Int {
    return when (signal) {
        ReviewerAttentionSignal.SYNC_FAILED -> 5
        ReviewerAttentionSignal.QOS_FAILED_OR_BLOCKED -> 4
        ReviewerAttentionSignal.QOS_PREREQUISITES_NOT_READY -> 3
        ReviewerAttentionSignal.SYNC_PENDING -> 2
        ReviewerAttentionSignal.STALE_DRAFT -> 1
    }
}

private fun workflowLabel(workflowType: ReportDraftOriginWorkflowType?): String {
    return when (workflowType) {
        ReportDraftOriginWorkflowType.XFEEDER -> "XFEEDER"
        ReportDraftOriginWorkflowType.RET -> "RET"
        ReportDraftOriginWorkflowType.PERFORMANCE -> "PERFORMANCE"
        null -> "NON_GUIDED"
    }
}

private fun formatEpoch(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return formatter.format(
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    )
}
