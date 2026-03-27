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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.SiteReportListItem
import com.quartz.platform.presentation.sync.syncStateLabelRes
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
        onRetryFailedSync = viewModel::onRetryFailedSyncClicked
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportListScreen(
    state: ReportListUiState,
    onBack: () -> Unit,
    onOpenDraft: (String) -> Unit,
    onRetryFailedSync: (String) -> Unit
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

                    items(state.reports, key = { it.draftId }) { report ->
                        ReportListItemCard(
                            report = report,
                            isRetrying = report.draftId in state.retryingDraftIds,
                            onOpenDraft = onOpenDraft,
                            onRetryFailedSync = onRetryFailedSync
                        )
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
