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
        onOpenDraft = viewModel::onOpenDraftClicked,
        onOpenSite = viewModel::onOpenSiteClicked
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewerControlTowerScreen(
    state: ReviewerControlTowerUiState,
    onBack: () -> Unit,
    onFilterSelected: (ReviewerControlTowerFilter) -> Unit,
    onOpenDraft: (String) -> Unit,
    onOpenSite: (String) -> Unit
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
                        ControlTowerFilterRow(
                            selectedFilter = state.selectedFilter,
                            onFilterSelected = onFilterSelected
                        )
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

                    if (state.filteredItems.isEmpty()) {
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
                        items(state.filteredItems, key = { item -> item.draftId }) { item ->
                            ReviewerControlTowerItemCard(
                                item = item,
                                onOpenDraft = onOpenDraft,
                                onOpenSite = onOpenSite
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
private fun ReviewerControlTowerItemCard(
    item: ReviewerControlTowerItem,
    onOpenDraft: (String) -> Unit,
    onOpenSite: (String) -> Unit
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

            item.originWorkflowType?.let { workflow ->
                Text(
                    text = stringResource(
                        R.string.reviewer_control_tower_workflow,
                        when (workflow) {
                            ReportDraftOriginWorkflowType.XFEEDER -> "XFEEDER"
                            ReportDraftOriginWorkflowType.RET -> "RET"
                            ReportDraftOriginWorkflowType.PERFORMANCE -> "PERFORMANCE"
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (item.attentionSignals.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(item.attentionSignals.toList()) { signal ->
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
        }
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

private fun formatEpoch(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return formatter.format(
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    )
}
