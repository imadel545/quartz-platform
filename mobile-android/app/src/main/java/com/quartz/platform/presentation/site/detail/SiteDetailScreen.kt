package com.quartz.platform.presentation.site.detail

import androidx.compose.foundation.clickable
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
import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.model.SiteAntenna
import com.quartz.platform.domain.model.SiteCell
import com.quartz.platform.domain.model.SiteDetail
import com.quartz.platform.domain.model.SiteSector
import com.quartz.platform.presentation.components.AdvancedDisclosureButton
import com.quartz.platform.presentation.components.OperationalSectionCard
import com.quartz.platform.presentation.components.OperationalSeverity
import com.quartz.platform.presentation.components.OperationalSignal
import com.quartz.platform.presentation.components.OperationalSignalRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
                        Text(stringResource(R.string.action_back_to_list))
                    }
                }
            }

            state.site == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.empty_site_technical_snapshot),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = onBack) {
                        Text(stringResource(R.string.action_back_to_list))
                    }
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
                        SiteMissionHeaderCard(site = state.site)
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
                        Text(
                            text = stringResource(R.string.site_detail_section_guided_workflows),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    state.infoMessage?.let { info ->
                        item {
                            OperationalSectionCard(title = stringResource(R.string.home_runtime_info_title)) {
                                Text(
                                    text = info,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    if (state.site.sectors.isEmpty()) {
                        item {
                            OperationalSectionCard(title = stringResource(R.string.site_detail_section_guided_workflows)) {
                                Text(
                                    text = stringResource(R.string.empty_site_technical_structure),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
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
                                onOpenXfeederSession = onOpenXfeederSession,
                                onOpenRetSession = onOpenRetSession,
                                showWorkflowActions = false
                            )
                        }
                    }

                    item {
                        val latestDraftUpdatedAt = state.drafts.maxOfOrNull { it.updatedAtEpochMillis }
                        OperationalSectionCard(
                            title = stringResource(R.string.header_local_drafts),
                            subtitle = stringResource(R.string.site_detail_local_drafts_hint)
                        ) {
                            OperationalSignalRow(
                                signals = listOf(
                                    OperationalSignal(
                                        text = stringResource(
                                            R.string.site_detail_signal_local_draft_count,
                                            state.drafts.size
                                        ),
                                        severity = if (state.drafts.isEmpty()) {
                                            OperationalSeverity.NORMAL
                                        } else {
                                            OperationalSeverity.SUCCESS
                                        }
                                    ),
                                    OperationalSignal(
                                        text = stringResource(
                                            R.string.site_detail_signal_local_draft_latest_update,
                                            latestDraftUpdatedAt?.let(::formatEpoch)
                                                ?: stringResource(R.string.value_not_available)
                                        )
                                    )
                                )
                            )
                        }
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
                                OperationalSectionCard(title = stringResource(R.string.header_local_drafts)) {
                                    Text(
                                        text = stringResource(R.string.empty_local_drafts_for_site),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        } else {
                            items(state.drafts, key = { it.id }) { draft ->
                                ReportDraftSummaryCard(
                                    draft = draft,
                                    onClick = { onOpenDraft(draft.id) }
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

@Composable
private fun SiteMissionActionsCard(
    siteId: String,
    isCreatingDraft: Boolean,
    onCreateDraftClicked: () -> Unit,
    onOpenReportList: (String) -> Unit,
    onOpenPerformanceSession: (String) -> Unit
) {
    OperationalSectionCard(
        title = stringResource(R.string.site_detail_section_mission_actions),
        subtitle = stringResource(R.string.site_detail_mission_actions_hint)
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onOpenPerformanceSession(siteId) }
        ) {
            Text(stringResource(R.string.action_open_performance_session))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                enabled = !isCreatingDraft,
                onClick = onCreateDraftClicked
            ) {
                Text(
                    text = if (isCreatingDraft) {
                        stringResource(R.string.action_create_local_draft_loading)
                    } else {
                        stringResource(R.string.action_create_local_draft)
                    }
                )
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { onOpenReportList(siteId) }
            ) {
                Text(stringResource(R.string.action_open_site_local_reports))
            }
        }
    }
}

@Composable
private fun SectorMissionLaunchCard(
    siteId: String,
    sector: SiteSector,
    onOpenXfeederSession: (siteId: String, sectorId: String) -> Unit,
    onOpenRetSession: (siteId: String, sectorId: String) -> Unit
) {
    OperationalSectionCard(
        title = stringResource(R.string.label_sector_title, sector.code),
        subtitle = stringResource(
            R.string.site_detail_sector_mission_context,
            sector.cells.size,
            sector.antennas.size
        )
    ) {
        OperationalSignalRow(
            signals = listOf(
                OperationalSignal(
                    text = if (sector.hasConnectedCell) {
                        stringResource(R.string.label_sector_connected_cell_detected)
                    } else {
                        stringResource(R.string.label_sector_connected_cell_none)
                    },
                    severity = if (sector.hasConnectedCell) {
                        OperationalSeverity.SUCCESS
                    } else {
                        OperationalSeverity.WARNING
                    }
                )
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = { onOpenXfeederSession(siteId, sector.id) }
            ) {
                Text(stringResource(R.string.site_detail_action_launch_xfeeder_short))
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { onOpenRetSession(siteId, sector.id) }
            ) {
                Text(stringResource(R.string.site_detail_action_launch_ret_short))
            }
        }
    }
}

@Composable
private fun SiteMissionHeaderCard(site: SiteDetail) {
    OperationalSectionCard(
        title = site.name,
        subtitle = stringResource(R.string.label_site_code, site.externalCode)
    ) {
        OperationalSignalRow(
            signals = listOf(
                OperationalSignal(stringResource(R.string.label_site_status, site.status)),
                OperationalSignal(
                    text = stringResource(R.string.label_site_sectors_in_service, site.sectorsInService),
                    severity = OperationalSeverity.SUCCESS
                ),
                OperationalSignal(
                    text = stringResource(R.string.label_site_sectors_forecast, site.sectorsForecast),
                    severity = OperationalSeverity.WARNING
                )
            )
        )
        Text(
            text = if (site.indoorOnly) {
                stringResource(R.string.label_site_profile_indoor)
            } else {
                stringResource(R.string.label_site_profile_mixed)
            },
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(
                R.string.label_coordinates,
                site.latitude.toString(),
                site.longitude.toString()
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SiteTechnicalStructureCard(
    site: SiteDetail,
    onOpenXfeederSession: (siteId: String, sectorId: String) -> Unit,
    onOpenRetSession: (siteId: String, sectorId: String) -> Unit,
    showWorkflowActions: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.site_detail_section_technical_details),
                style = MaterialTheme.typography.titleMedium
            )

            if (site.sectors.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_site_technical_structure),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                site.sectors.forEach { sector ->
                    SectorDetailBlock(
                        siteId = site.id,
                        sector = sector,
                        onOpenXfeederSession = onOpenXfeederSession,
                        onOpenRetSession = onOpenRetSession,
                        showWorkflowActions = showWorkflowActions
                    )
                }
            }
        }
    }
}

@Composable
private fun SectorDetailBlock(
    siteId: String,
    sector: SiteSector,
    onOpenXfeederSession: (siteId: String, sectorId: String) -> Unit,
    onOpenRetSession: (siteId: String, sectorId: String) -> Unit,
    showWorkflowActions: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.label_sector_title, sector.code),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(
                    R.string.label_sector_azimuth,
                    sector.azimuthDegrees?.toString() ?: stringResource(R.string.value_not_available)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.label_sector_status, sector.status),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = if (sector.hasConnectedCell) {
                    stringResource(R.string.label_sector_connected_cell_detected)
                } else {
                    stringResource(R.string.label_sector_connected_cell_none)
                },
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = stringResource(R.string.header_sector_antennas),
                style = MaterialTheme.typography.labelLarge
            )
            if (sector.antennas.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_sector_antennas),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                sector.antennas.forEach { antenna ->
                    AntennaDetailBlock(antenna = antenna)
                }
            }

            Text(
                text = stringResource(R.string.header_sector_cells),
                style = MaterialTheme.typography.labelLarge
            )
            if (sector.cells.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_sector_cells),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                sector.cells.forEach { cell ->
                    CellDetailBlock(cell = cell)
                }
            }

            if (showWorkflowActions) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenXfeederSession(siteId, sector.id) }
                ) {
                    Text(stringResource(R.string.action_open_xfeeder_guided_session))
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenRetSession(siteId, sector.id) }
                ) {
                    Text(stringResource(R.string.action_open_ret_guided_session))
                }
            }
        }
    }
}

@Composable
private fun AntennaDetailBlock(antenna: SiteAntenna) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(R.string.label_antenna_reference, antenna.reference),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(R.string.label_antenna_installed_state, antenna.installedState),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(
                R.string.label_antenna_forecast_state,
                antenna.forecastState ?: stringResource(R.string.value_not_available)
            ),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(
                R.string.label_antenna_tilt_configured,
                antenna.tiltConfiguredDegrees?.toString() ?: stringResource(R.string.value_not_available)
            ),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(
                R.string.label_antenna_tilt_observed,
                antenna.tiltObservedDegrees?.toString() ?: stringResource(R.string.value_not_available)
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun CellDetailBlock(cell: SiteCell) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(R.string.label_cell_name, cell.label),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(
                R.string.label_cell_technology_operator,
                cell.technology,
                cell.operatorName
            ),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(
                R.string.label_cell_band_pci,
                cell.band,
                cell.pci ?: stringResource(R.string.value_not_available)
            ),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(R.string.label_cell_status, cell.status),
            style = MaterialTheme.typography.bodySmall
        )
        if (cell.isConnected) {
            Text(
                text = stringResource(R.string.label_cell_connected),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ReportDraftSummaryCard(
    draft: ReportDraft,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = draft.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.label_revision, draft.revision),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.label_updated_at, formatEpoch(draft.updatedAtEpochMillis)),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatEpoch(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return formatter.format(
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    )
}
