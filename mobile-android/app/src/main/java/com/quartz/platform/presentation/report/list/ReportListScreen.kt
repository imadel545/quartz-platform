package com.quartz.platform.presentation.report.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.quartz.platform.domain.model.SiteReportListItem
import com.quartz.platform.presentation.components.OperationalEmptyStateCard
import com.quartz.platform.presentation.components.OperationalMessageCard
import com.quartz.platform.presentation.components.OperationalSeverity
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

                    item {
                        ReportListRuntimeStateBanner(
                            rankedReports = rankedReports,
                            selectedFilter = state.selectedFilter
                        )
                    }

                    topPriorityReport?.let { report ->
                        item {
                            ReportListFocusCard(
                                report = report,
                                isRetrying = report.draftId in state.retryingDraftIds,
                                onOpenDraft = onOpenDraft,
                                onRetryFailedSync = onRetryFailedSync
                            )
                        }
                    }

                    item {
                        ReportListQueueControlsCard(
                            selectedFilter = state.selectedFilter,
                            queueSize = rankedReports.size,
                            onFilterSelected = onFilterSelected
                        )
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
                            ReportQueueSectionCard(
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
