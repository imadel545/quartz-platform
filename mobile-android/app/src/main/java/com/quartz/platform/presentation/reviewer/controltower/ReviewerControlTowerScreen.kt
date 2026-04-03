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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.quartz.platform.domain.model.ReviewerDraftAgeBucket
import com.quartz.platform.domain.model.ReviewerUrgencyClass
import com.quartz.platform.domain.model.ReviewerUrgencyReason
import com.quartz.platform.domain.model.SupervisorQueueActionType
import com.quartz.platform.domain.model.SupervisorQueueStatus
import com.quartz.platform.presentation.components.AdvancedDisclosureButton
import com.quartz.platform.presentation.components.OperationalMessageCard
import com.quartz.platform.presentation.components.OperationalSectionCard
import com.quartz.platform.presentation.components.OperationalSeverity
import com.quartz.platform.presentation.components.OperationalSignal
import com.quartz.platform.presentation.components.OperationalSignalRow
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
        onQueueStatusFilterSelected = viewModel::onQueueStatusFilterSelected,
        onGroupingSelected = viewModel::onGroupingSelected,
        onPresetSelected = viewModel::onPresetSelected,
        onOpenTopPriority = viewModel::onOpenTopPriorityClicked,
        onResetQueueProgress = viewModel::onResetQueueProgressClicked,
        onRetryFailedVisibleSync = viewModel::onRetryFailedVisibleSyncClicked,
        onBulkMarkVisibleInReview = viewModel::onBulkMarkVisibleInReviewClicked,
        onOpenDraft = viewModel::onOpenDraftClicked,
        onOpenSite = viewModel::onOpenSiteClicked,
        onRetryDraftSync = viewModel::onRetryDraftSyncClicked,
        onMarkDraftInReview = viewModel::onMarkDraftInReviewClicked,
        onMarkDraftWaitingFeedback = viewModel::onMarkDraftWaitingFeedbackClicked,
        onMarkDraftResolved = viewModel::onMarkDraftResolvedClicked,
        onReopenDraft = viewModel::onReopenDraftClicked
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewerControlTowerScreen(
    state: ReviewerControlTowerUiState,
    onBack: () -> Unit,
    onFilterSelected: (ReviewerControlTowerFilter) -> Unit,
    onQueueStatusFilterSelected: (ReviewerQueueStatusFilter) -> Unit,
    onGroupingSelected: (ReviewerControlTowerGrouping) -> Unit,
    onPresetSelected: (ReviewerQueuePreset) -> Unit,
    onOpenTopPriority: () -> Unit,
    onResetQueueProgress: () -> Unit,
    onRetryFailedVisibleSync: () -> Unit,
    onBulkMarkVisibleInReview: () -> Unit,
    onOpenDraft: (String) -> Unit,
    onOpenSite: (String) -> Unit,
    onRetryDraftSync: (String) -> Unit,
    onMarkDraftInReview: (String) -> Unit,
    onMarkDraftWaitingFeedback: (String) -> Unit,
    onMarkDraftResolved: (String) -> Unit,
    onReopenDraft: (String) -> Unit
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
                var showAdvancedControls by rememberSaveable { mutableStateOf(false) }
                var showQueueTuning by rememberSaveable { mutableStateOf(false) }
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
                        OperationalSectionCard(
                            title = stringResource(R.string.reviewer_control_tower_section_queue_actions_title),
                            subtitle = stringResource(R.string.reviewer_control_tower_section_queue_actions_hint)
                        ) {
                            ControlTowerActionRow(
                                state = state,
                                onOpenTopPriority = onOpenTopPriority,
                                onResetQueueProgress = onResetQueueProgress,
                                onRetryFailedVisibleSync = onRetryFailedVisibleSync,
                                onBulkMarkVisibleInReview = onBulkMarkVisibleInReview
                            )
                        }
                    }

                    item {
                        OperationalSectionCard(
                            title = stringResource(R.string.reviewer_control_tower_section_lenses_title),
                            subtitle = stringResource(R.string.reviewer_control_tower_section_lenses_hint)
                        ) {
                            ControlTowerPresetRow(
                                selectedPreset = state.selectedPreset,
                                onPresetSelected = onPresetSelected
                            )
                            AdvancedDisclosureButton(
                                expanded = showQueueTuning,
                                onToggle = { showQueueTuning = !showQueueTuning },
                                showLabel = stringResource(R.string.reviewer_control_tower_action_show_queue_tuning),
                                hideLabel = stringResource(R.string.reviewer_control_tower_action_hide_queue_tuning)
                            )
                            if (showQueueTuning) {
                                ControlTowerFilterRow(
                                    selectedFilter = state.selectedFilter,
                                    onFilterSelected = onFilterSelected
                                )
                            }
                        }
                    }

                    item {
                        AdvancedDisclosureButton(
                            expanded = showAdvancedControls,
                            onToggle = { showAdvancedControls = !showAdvancedControls },
                            showLabel = stringResource(R.string.reviewer_control_tower_action_show_advanced_controls),
                            hideLabel = stringResource(R.string.reviewer_control_tower_action_hide_advanced_controls)
                        )
                    }

                    if (showAdvancedControls) {
                        item {
                            OperationalSectionCard(
                                title = stringResource(R.string.reviewer_control_tower_section_advanced_title)
                            ) {
                                ControlTowerQueueStatusFilterRow(
                                    selectedFilter = state.selectedQueueStatusFilter,
                                    onFilterSelected = onQueueStatusFilterSelected
                                )
                                ControlTowerGroupingRow(
                                    selectedGrouping = state.selectedGrouping,
                                    onGroupingSelected = onGroupingSelected
                                )
                            }
                        }

                        item {
                            ControlTowerMotifSection(
                                siteMotifs = state.siteMotifs,
                                workflowMotifs = state.workflowMotifs,
                                urgencyMotifs = state.urgencyMotifs,
                                statusMotifs = state.statusMotifs,
                                onOpenDraft = onOpenDraft
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
                                retryingDraftIds = state.retryingDraftIds,
                                transitioningDraftIds = state.transitioningQueueDraftIds,
                                onMarkDraftInReview = onMarkDraftInReview,
                                onMarkDraftWaitingFeedback = onMarkDraftWaitingFeedback,
                                onMarkDraftResolved = onMarkDraftResolved,
                                onReopenDraft = onReopenDraft
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
    OperationalSectionCard(
        title = stringResource(R.string.reviewer_control_tower_summary_title),
        subtitle = stringResource(
            R.string.reviewer_control_tower_summary_counts,
            state.summary.totalDraftCount,
            state.summary.guidedDraftCount,
            state.summary.nonGuidedDraftCount
        )
    ) {
        OperationalSignalRow(
            signals = listOf(
                OperationalSignal(
                    text = stringResource(
                        R.string.reviewer_control_tower_summary_sla,
                        state.summary.actNowCount,
                        state.summary.overdueCount
                    ),
                    severity = if (state.summary.actNowCount > 0 || state.summary.overdueCount > 0) {
                        OperationalSeverity.CRITICAL
                    } else {
                        OperationalSeverity.SUCCESS
                    }
                ),
                OperationalSignal(
                    text = stringResource(
                        R.string.reviewer_control_tower_summary_attention_short,
                        state.summary.attentionRequiredCount,
                        state.summary.syncFailedCount,
                        state.summary.qosRiskCount
                    ),
                    severity = if (state.summary.attentionRequiredCount > 0) {
                        OperationalSeverity.WARNING
                    } else {
                        OperationalSeverity.SUCCESS
                    }
                )
            )
        )
        Text(
            text = stringResource(
                R.string.reviewer_control_tower_summary_queue,
                state.summary.untriagedCount,
                state.summary.inReviewCount,
                state.summary.waitingFieldFeedbackCount,
                state.summary.resolvedCount
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ControlTowerActionRow(
    state: ReviewerControlTowerUiState,
    onOpenTopPriority: () -> Unit,
    onResetQueueProgress: () -> Unit,
    onRetryFailedVisibleSync: () -> Unit,
    onBulkMarkVisibleInReview: () -> Unit
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = state.visibleUntriagedCount > 0 && !state.isBulkQueueTransitionInProgress,
                onClick = onBulkMarkVisibleInReview
            ) {
                Text(
                    if (state.isBulkQueueTransitionInProgress) {
                        stringResource(R.string.action_queue_status_update_loading)
                    } else {
                        stringResource(
                            R.string.reviewer_control_tower_action_mark_visible_in_review,
                            state.visibleUntriagedCount
                        )
                    }
                )
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = state.progressedDraftIds.isNotEmpty(),
                onClick = onResetQueueProgress
            ) {
                Text(stringResource(R.string.reviewer_control_tower_action_reset_queue_progress))
            }
        }
    }
}

@Composable
private fun ControlTowerQueueStatusFilterRow(
    selectedFilter: ReviewerQueueStatusFilter,
    onFilterSelected: (ReviewerQueueStatusFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.reviewer_control_tower_queue_filter_label),
            style = MaterialTheme.typography.bodyMedium
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ReviewerQueueStatusFilter.entries) { filter ->
                FilterChip(
                    selected = filter == selectedFilter,
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            text = when (filter) {
                                ReviewerQueueStatusFilter.ALL -> stringResource(R.string.reviewer_control_tower_queue_filter_all)
                                ReviewerQueueStatusFilter.UNTRIAGED -> stringResource(R.string.reviewer_control_tower_queue_status_untriaged)
                                ReviewerQueueStatusFilter.IN_REVIEW -> stringResource(R.string.reviewer_control_tower_queue_status_in_review)
                                ReviewerQueueStatusFilter.WAITING_FIELD_FEEDBACK -> stringResource(R.string.reviewer_control_tower_queue_status_waiting_feedback)
                                ReviewerQueueStatusFilter.RESOLVED -> stringResource(R.string.reviewer_control_tower_queue_status_resolved)
                            }
                        )
                    }
                )
            }
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
                                ReviewerQueuePreset.ACT_NOW_OVERDUE -> stringResource(R.string.reviewer_control_tower_preset_act_now_overdue)
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
    urgencyMotifs: List<ReviewerQueueUrgencyMotif>,
    statusMotifs: List<ReviewerQueueStatusMotif>,
    onOpenDraft: (String) -> Unit
) {
    if (siteMotifs.isEmpty() && workflowMotifs.isEmpty() && urgencyMotifs.isEmpty() && statusMotifs.isEmpty()) return

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
                            subtitle = buildString {
                                append(
                                    stringResource(
                                        R.string.reviewer_control_tower_motif_counts,
                                        motif.actNowCount,
                                        motif.overdueCount
                                    )
                                )
                                motif.dominantAttentionSignal?.let { signal ->
                                    append(" • ")
                                    append(stringResource(attentionSignalLabelRes(signal)))
                                }
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
                            subtitle = buildString {
                                append(
                                    stringResource(
                                        R.string.reviewer_control_tower_motif_counts,
                                        motif.actNowCount,
                                        motif.overdueCount
                                    )
                                )
                                motif.dominantAttentionSignal?.let { signal ->
                                    append(" • ")
                                    append(stringResource(attentionSignalLabelRes(signal)))
                                }
                            },
                            onOpen = motif.topDraftId?.let { draftId -> { onOpenDraft(draftId) } }
                        )
                    }
                }
            }

            if (urgencyMotifs.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.reviewer_control_tower_motif_by_urgency),
                    style = MaterialTheme.typography.labelMedium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(urgencyMotifs) { motif ->
                        MotifCard(
                            title = "${stringResource(urgencyClassLabelRes(motif.urgencyClass))} (${motif.draftCount})",
                            subtitle = buildString {
                                append(
                                    stringResource(
                                        R.string.reviewer_control_tower_motif_urgency_counts,
                                        motif.draftCount,
                                        motif.overdueCount
                                    )
                                )
                                motif.dominantAttentionSignal?.let { signal ->
                                    append(" • ")
                                    append(stringResource(attentionSignalLabelRes(signal)))
                                }
                            },
                            onOpen = motif.topDraftId?.let { draftId -> { onOpenDraft(draftId) } }
                        )
                    }
                }
            }

            if (statusMotifs.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.reviewer_control_tower_motif_by_queue_status),
                    style = MaterialTheme.typography.labelMedium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(statusMotifs) { motif ->
                        MotifCard(
                            title = "${stringResource(queueStatusLabelRes(motif.queueStatus))} (${motif.draftCount})",
                            subtitle = motif.dominantActionType?.let { actionType ->
                                stringResource(
                                    R.string.reviewer_control_tower_motif_dominant_action,
                                    stringResource(queueActionLabelRes(actionType))
                                )
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
    retryingDraftIds: Set<String>,
    transitioningDraftIds: Set<String>,
    onMarkDraftInReview: (String) -> Unit,
    onMarkDraftWaitingFeedback: (String) -> Unit,
    onMarkDraftResolved: (String) -> Unit,
    onReopenDraft: (String) -> Unit
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
                    isQueueTransitioning = item.draftId in transitioningDraftIds,
                    onOpenDraft = onOpenDraft,
                    onOpenSite = onOpenSite,
                    onRetryDraftSync = onRetryDraftSync,
                    onMarkDraftInReview = onMarkDraftInReview,
                    onMarkDraftWaitingFeedback = onMarkDraftWaitingFeedback,
                    onMarkDraftResolved = onMarkDraftResolved,
                    onReopenDraft = onReopenDraft
                )
            }
        }
    }
}

@Composable
private fun ReviewerControlTowerItemCard(
    item: ReviewerControlTowerItem,
    isRetrying: Boolean,
    isQueueTransitioning: Boolean,
    onOpenDraft: (String) -> Unit,
    onOpenSite: (String) -> Unit,
    onRetryDraftSync: (String) -> Unit,
    onMarkDraftInReview: (String) -> Unit,
    onMarkDraftWaitingFeedback: (String) -> Unit,
    onMarkDraftResolved: (String) -> Unit,
    onReopenDraft: (String) -> Unit
) {
    OperationalSectionCard(
        title = "${item.siteCode} • ${item.siteName}",
        subtitle = item.title
    ) {
        val urgencySeverity = when (item.urgencyClass) {
            ReviewerUrgencyClass.ACT_NOW -> OperationalSeverity.CRITICAL
            ReviewerUrgencyClass.HIGH -> OperationalSeverity.WARNING
            ReviewerUrgencyClass.WATCH -> OperationalSeverity.NORMAL
            ReviewerUrgencyClass.NORMAL -> OperationalSeverity.SUCCESS
        }
        OperationalSignalRow(
            signals = buildList {
                add(
                    OperationalSignal(
                        text = stringResource(
                            R.string.reviewer_control_tower_urgency_short,
                            stringResource(urgencyClassLabelRes(item.urgencyClass)),
                            stringResource(ageBucketLabelRes(item.ageBucket))
                        ),
                        severity = urgencySeverity
                    )
                )
                add(
                    OperationalSignal(
                        text = stringResource(
                            R.string.reviewer_control_tower_queue_status_short,
                            stringResource(queueStatusLabelRes(item.queueStatus))
                        )
                    )
                )
                item.originWorkflowType?.let { workflow ->
                    add(
                        OperationalSignal(
                            text = stringResource(
                                R.string.reviewer_control_tower_workflow_short,
                                workflowLabel(workflow)
                            )
                        )
                    )
                }
            }
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
        Text(
            text = stringResource(
                R.string.reviewer_control_tower_urgency_reason_short,
                stringResource(urgencyReasonLabelRes(item.urgencyReason))
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

        if (item.attentionSignals.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(item.attentionSignals.toList().sortedByDescending(::attentionSignalSeverity).take(3)) { signal ->
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(attentionSignalLabelRes(signal))) }
                    )
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (item.queueStatus) {
                SupervisorQueueStatus.UNTRIAGED -> {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !isQueueTransitioning,
                        onClick = { onMarkDraftInReview(item.draftId) }
                    ) {
                        Text(stringResource(R.string.reviewer_control_tower_action_mark_in_review))
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !isQueueTransitioning,
                        onClick = { onMarkDraftResolved(item.draftId) }
                    ) {
                        Text(stringResource(R.string.reviewer_control_tower_action_mark_resolved))
                    }
                }
                SupervisorQueueStatus.IN_REVIEW -> {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !isQueueTransitioning,
                        onClick = { onMarkDraftWaitingFeedback(item.draftId) }
                    ) {
                        Text(stringResource(R.string.reviewer_control_tower_action_mark_waiting_feedback))
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !isQueueTransitioning,
                        onClick = { onMarkDraftResolved(item.draftId) }
                    ) {
                        Text(stringResource(R.string.reviewer_control_tower_action_mark_resolved))
                    }
                }
                SupervisorQueueStatus.WAITING_FIELD_FEEDBACK -> {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !isQueueTransitioning,
                        onClick = { onMarkDraftInReview(item.draftId) }
                    ) {
                        Text(stringResource(R.string.reviewer_control_tower_action_mark_in_review))
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !isQueueTransitioning,
                        onClick = { onMarkDraftResolved(item.draftId) }
                    ) {
                        Text(stringResource(R.string.reviewer_control_tower_action_mark_resolved))
                    }
                }
                SupervisorQueueStatus.RESOLVED -> {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isQueueTransitioning,
                        onClick = { onReopenDraft(item.draftId) }
                    ) {
                        Text(stringResource(R.string.reviewer_control_tower_action_reopen))
                    }
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

private fun urgencyClassLabelRes(urgencyClass: ReviewerUrgencyClass): Int {
    return when (urgencyClass) {
        ReviewerUrgencyClass.ACT_NOW -> R.string.reviewer_control_tower_urgency_act_now
        ReviewerUrgencyClass.HIGH -> R.string.reviewer_control_tower_urgency_high
        ReviewerUrgencyClass.WATCH -> R.string.reviewer_control_tower_urgency_watch
        ReviewerUrgencyClass.NORMAL -> R.string.reviewer_control_tower_urgency_normal
    }
}

private fun ageBucketLabelRes(ageBucket: ReviewerDraftAgeBucket): Int {
    return when (ageBucket) {
        ReviewerDraftAgeBucket.FRESH -> R.string.reviewer_control_tower_age_fresh
        ReviewerDraftAgeBucket.AGING -> R.string.reviewer_control_tower_age_aging
        ReviewerDraftAgeBucket.STALE -> R.string.reviewer_control_tower_age_stale
        ReviewerDraftAgeBucket.OVERDUE -> R.string.reviewer_control_tower_age_overdue
    }
}

private fun urgencyReasonLabelRes(reason: ReviewerUrgencyReason): Int {
    return when (reason) {
        ReviewerUrgencyReason.SYNC_FAILED -> R.string.reviewer_control_tower_urgency_reason_sync_failed
        ReviewerUrgencyReason.QOS_FAILED_OR_BLOCKED -> R.string.reviewer_control_tower_urgency_reason_qos_failed
        ReviewerUrgencyReason.QOS_PREREQUISITES_NOT_READY -> R.string.reviewer_control_tower_urgency_reason_qos_preflight
        ReviewerUrgencyReason.STALE_DRAFT -> R.string.reviewer_control_tower_urgency_reason_stale_draft
        ReviewerUrgencyReason.STALE_GUIDED_WORK -> R.string.reviewer_control_tower_urgency_reason_stale_guided
        ReviewerUrgencyReason.STALE_PENDING_SYNC -> R.string.reviewer_control_tower_urgency_reason_stale_pending
        ReviewerUrgencyReason.NONE -> R.string.reviewer_control_tower_urgency_reason_none
    }
}

private fun queueStatusLabelRes(status: SupervisorQueueStatus): Int {
    return when (status) {
        SupervisorQueueStatus.UNTRIAGED -> R.string.reviewer_control_tower_queue_status_untriaged
        SupervisorQueueStatus.IN_REVIEW -> R.string.reviewer_control_tower_queue_status_in_review
        SupervisorQueueStatus.WAITING_FIELD_FEEDBACK -> R.string.reviewer_control_tower_queue_status_waiting_feedback
        SupervisorQueueStatus.RESOLVED -> R.string.reviewer_control_tower_queue_status_resolved
    }
}

private fun queueActionLabelRes(actionType: SupervisorQueueActionType): Int {
    return when (actionType) {
        SupervisorQueueActionType.MARK_IN_REVIEW -> R.string.reviewer_control_tower_action_mark_in_review
        SupervisorQueueActionType.BULK_MARK_IN_REVIEW -> R.string.reviewer_control_tower_action_bulk_mark_in_review
        SupervisorQueueActionType.MARK_WAITING_FIELD_FEEDBACK -> R.string.reviewer_control_tower_action_mark_waiting_feedback
        SupervisorQueueActionType.MARK_RESOLVED -> R.string.reviewer_control_tower_action_mark_resolved
        SupervisorQueueActionType.REOPEN_TO_UNTRIAGED -> R.string.reviewer_control_tower_action_reopen
        SupervisorQueueActionType.RETRY_SYNC -> R.string.action_retry_sync
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
