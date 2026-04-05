package com.quartz.platform.presentation.site.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quartz.platform.R
import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.model.SiteAntenna
import com.quartz.platform.domain.model.SiteCell
import com.quartz.platform.domain.model.SiteDetail
import com.quartz.platform.domain.model.SiteSector
import com.quartz.platform.presentation.components.MissionHeaderCard
import com.quartz.platform.presentation.components.OperationalEmptyStateCard
import com.quartz.platform.presentation.components.OperationalMessageCard
import com.quartz.platform.presentation.components.OperationalMetric
import com.quartz.platform.presentation.components.OperationalSectionCard
import com.quartz.platform.presentation.components.OperationalSeverity
import com.quartz.platform.presentation.components.OperationalSignal
import com.quartz.platform.presentation.components.OperationalSignalRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SiteMissionActionsCard(
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
fun SiteGuidedWorkflowsHeaderCard() {
    OperationalSectionCard(
        title = stringResource(R.string.site_detail_section_guided_workflows),
        subtitle = stringResource(R.string.site_detail_guided_workflows_hint)
    ) {
        Text(
            text = stringResource(R.string.site_detail_guided_workflows_body),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun SiteEmptySectorsCard() {
    OperationalEmptyStateCard(
        title = stringResource(R.string.site_detail_section_guided_workflows),
        message = stringResource(R.string.empty_site_technical_structure)
    )
}

@Composable
fun SectorMissionLaunchCard(
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
                ),
                OperationalSignal(
                    text = stringResource(
                        R.string.label_sector_azimuth,
                        sector.azimuthDegrees?.toString() ?: stringResource(R.string.value_not_available)
                    )
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
fun SiteMissionHeaderCard(site: SiteDetail, localDraftCount: Int) {
    MissionHeaderCard(
        title = site.name,
        subtitle = stringResource(R.string.label_site_code, site.externalCode),
        signals = listOf(
            OperationalSignal(stringResource(R.string.label_site_status, site.status)),
            OperationalSignal(
                text = if (site.indoorOnly) {
                    stringResource(R.string.label_site_profile_indoor)
                } else {
                    stringResource(R.string.label_site_profile_mixed)
                }
            )
        ),
        metrics = listOf(
            OperationalMetric(
                value = site.sectorsInService.toString(),
                label = stringResource(R.string.site_detail_metric_sectors_in_service),
                severity = OperationalSeverity.SUCCESS
            ),
            OperationalMetric(
                value = site.sectorsForecast.toString(),
                label = stringResource(R.string.site_detail_metric_sectors_forecast),
                severity = if (site.sectorsForecast == 0) {
                    OperationalSeverity.NORMAL
                } else {
                    OperationalSeverity.WARNING
                }
            ),
            OperationalMetric(
                value = localDraftCount.toString(),
                label = stringResource(R.string.site_detail_metric_local_drafts)
            )
        )
    ) {
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
fun SiteRuntimeStateBanner(site: SiteDetail, drafts: List<ReportDraft>) {
    val banner = when {
        site.sectors.isEmpty() -> Triple(
            stringResource(R.string.site_detail_runtime_state_title),
            stringResource(R.string.site_detail_runtime_state_no_sector_message),
            OperationalSeverity.CRITICAL
        )
        drafts.isEmpty() -> Triple(
            stringResource(R.string.site_detail_runtime_state_title),
            stringResource(R.string.site_detail_runtime_state_no_draft_message),
            OperationalSeverity.WARNING
        )
        else -> Triple(
            stringResource(R.string.site_detail_runtime_state_title),
            stringResource(R.string.site_detail_runtime_state_ready_message),
            OperationalSeverity.SUCCESS
        )
    }
    OperationalMessageCard(
        title = banner.first,
        message = banner.second,
        severity = banner.third
    )
}

@Composable
fun SiteTechnicalStructureCard(
    site: SiteDetail,
    showWorkflowActions: Boolean
) {
    OperationalSectionCard(
        title = stringResource(R.string.site_detail_section_technical_details),
        subtitle = stringResource(R.string.site_detail_technical_details_hint)
    ) {
        if (site.sectors.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_site_technical_structure),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            site.sectors.forEach { sector ->
                SectorDetailBlock(
                    sector = sector,
                    showWorkflowActions = showWorkflowActions
                )
            }
        }
    }
}

@Composable
private fun SectorDetailBlock(
    sector: SiteSector,
    showWorkflowActions: Boolean
) {
    OperationalSectionCard(
        title = stringResource(R.string.label_sector_title, sector.code),
        subtitle = stringResource(
            R.string.label_sector_status,
            sector.status
        )
    ) {
        Text(
            text = stringResource(
                R.string.label_sector_azimuth,
                sector.azimuthDegrees?.toString() ?: stringResource(R.string.value_not_available)
            ),
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
            Text(
                text = stringResource(R.string.site_detail_runtime_state_technical_actions_hidden),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
fun SiteLocalDraftSummaryCard(drafts: List<ReportDraft>) {
    val latestDraftUpdatedAt = drafts.maxOfOrNull { it.updatedAtEpochMillis }
    OperationalSectionCard(
        title = stringResource(R.string.header_local_drafts),
        subtitle = stringResource(R.string.site_detail_local_drafts_hint)
    ) {
        OperationalSignalRow(
            signals = listOf(
                OperationalSignal(
                    text = stringResource(
                        R.string.site_detail_signal_local_draft_count,
                        drafts.size
                    ),
                    severity = if (drafts.isEmpty()) {
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

@Composable
fun SiteEmptyDraftsCard() {
    OperationalEmptyStateCard(
        title = stringResource(R.string.header_local_drafts),
        message = stringResource(R.string.empty_local_drafts_for_site)
    )
}

@Composable
fun ReportDraftSummaryCard(
    draft: ReportDraft,
    onOpenDraft: () -> Unit
) {
    OperationalSectionCard(
        title = draft.title,
        subtitle = stringResource(
            R.string.site_detail_draft_card_subtitle,
            draft.revision,
            formatEpoch(draft.updatedAtEpochMillis)
        )
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenDraft
        ) {
            Text(stringResource(R.string.action_open_draft))
        }
    }
}

private val siteDateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())

private fun formatEpoch(epochMillis: Long): String {
    return siteDateFormatter.format(Instant.ofEpochMilli(epochMillis))
}
