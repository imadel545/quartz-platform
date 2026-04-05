package com.quartz.platform.presentation.site.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.quartz.platform.presentation.components.AdvancedDisclosureButton
import com.quartz.platform.presentation.components.OperationalEmptyStateCard
import com.quartz.platform.presentation.components.OperationalMessageCard
import com.quartz.platform.presentation.components.OperationalSeverity
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SiteDetailRoute(
    onBack: () -> Unit,
    onOpenDraft: (String) -> Unit,
    onOpenReportList: (String) -> Unit,
    onOpenXfeederSession: (siteId: String, sectorId: String) -> Unit,
    onOpenRetSession: (siteId: String, sectorId: String) -> Unit,
    onOpenPerformanceSession: (siteId: String) -> Unit,
    viewModel: SiteDetailViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SiteDetailEvent.OpenDraft -> onOpenDraft(event.draftId)
            }
        }
    }

    SiteDetailScreen(
        state = state,
        onBack = onBack,
        onCreateDraftClicked = viewModel::onCreateDraftClicked,
        onOpenDraft = onOpenDraft,
        onOpenReportList = onOpenReportList,
        onOpenXfeederSession = onOpenXfeederSession,
        onOpenRetSession = onOpenRetSession,
        onOpenPerformanceSession = onOpenPerformanceSession
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteDetailScreen(
    state: SiteDetailUiState,
    onBack: () -> Unit,
    onCreateDraftClicked: () -> Unit,
    onOpenDraft: (String) -> Unit,
    onOpenReportList: (String) -> Unit,
    onOpenXfeederSession: (siteId: String, sectorId: String) -> Unit,
    onOpenRetSession: (siteId: String, sectorId: String) -> Unit,
    onOpenPerformanceSession: (siteId: String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_site_detail)) }
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

            state.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    OperationalMessageCard(
                        title = stringResource(R.string.site_detail_runtime_state_title),
                        message = state.errorMessage,
                        severity = OperationalSeverity.CRITICAL
                    )
                }
            }

            state.site == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    OperationalEmptyStateCard(
                        title = stringResource(R.string.empty_site_technical_snapshot),
                        message = stringResource(R.string.site_detail_runtime_state_empty_site_hint),
                        action = {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onBack
                            ) {
                                Text(stringResource(R.string.action_back_to_list))
                            }
                        }
                    )
                }
            }

            else -> {
                var showTechnicalDetails by rememberSaveable { mutableStateOf(false) }
                var showLocalDrafts by rememberSaveable { mutableStateOf(false) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SiteMissionHeaderCard(
                            site = state.site,
                            localDraftCount = state.drafts.size
                        )
                    }

                    item {
                        SiteRuntimeStateBanner(
                            site = state.site,
                            drafts = state.drafts
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

                    item {
                        SiteMissionActionsCard(
                            siteId = state.site.id,
                            isCreatingDraft = state.isCreatingDraft,
                            onCreateDraftClicked = onCreateDraftClicked,
                            onOpenReportList = onOpenReportList,
                            onOpenPerformanceSession = onOpenPerformanceSession
                        )
                    }

                    item {
                        SiteGuidedWorkflowsHeaderCard()
                    }

                    if (state.site.sectors.isEmpty()) {
                        item {
                            SiteEmptySectorsCard()
                        }
                    } else {
                        items(state.site.sectors, key = { it.id }) { sector ->
                            SectorMissionLaunchCard(
                                siteId = state.site.id,
                                sector = sector,
                                onOpenXfeederSession = onOpenXfeederSession,
                                onOpenRetSession = onOpenRetSession
                            )
                        }
                    }

                    item {
                        AdvancedDisclosureButton(
                            expanded = showTechnicalDetails,
                            onToggle = { showTechnicalDetails = !showTechnicalDetails },
                            showLabel = stringResource(R.string.site_detail_action_show_technical_details),
                            hideLabel = stringResource(R.string.site_detail_action_hide_technical_details)
                        )
                    }

                    if (showTechnicalDetails) {
                        item {
                            SiteTechnicalStructureCard(
                                site = state.site,
                                showWorkflowActions = false
                            )
                        }
                    }

                    item {
                        SiteLocalDraftSummaryCard(drafts = state.drafts)
                    }

                    item {
                        AdvancedDisclosureButton(
                            expanded = showLocalDrafts,
                            onToggle = { showLocalDrafts = !showLocalDrafts },
                            showLabel = stringResource(R.string.site_detail_action_show_local_drafts),
                            hideLabel = stringResource(R.string.site_detail_action_hide_local_drafts)
                        )
                    }

                    if (showLocalDrafts) {
                        if (state.drafts.isEmpty()) {
                            item {
                                SiteEmptyDraftsCard()
                            }
                        } else {
                            items(state.drafts, key = { it.id }) { draft ->
                                ReportDraftSummaryCard(
                                    draft = draft,
                                    onOpenDraft = { onOpenDraft(draft.id) }
                                )
                            }
                        }
                    }

                    item {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onBack
                        ) {
                            Text(stringResource(R.string.action_back_to_list))
                        }
                    }
                }
            }
        }
    }
}
