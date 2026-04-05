package com.quartz.platform.presentation.xfeeder.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quartz.platform.R
import com.quartz.platform.domain.model.XfeederGuidedSession
import com.quartz.platform.domain.model.XfeederGuidedStep
import com.quartz.platform.domain.model.XfeederStepCode
import com.quartz.platform.domain.model.XfeederStepStatus
import com.quartz.platform.presentation.components.OperationalSectionCard
import com.quartz.platform.presentation.components.OperationalSeverity
import com.quartz.platform.presentation.components.OperationalSignal
import com.quartz.platform.presentation.components.OperationalSignalRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun SectorCellsContextCard(state: XfeederGuidedSessionUiState) {
    OperationalSectionCard(
        title = stringResource(R.string.xfeeder_header_sector_cells_context),
        subtitle = stringResource(R.string.xfeeder_section_advanced_context_hint)
    ) {
        if (state.systemOperatorContexts.isEmpty()) {
            Text(
                text = stringResource(R.string.xfeeder_empty_sector_system_context),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            state.systemOperatorContexts.forEach { context ->
                Text(
                    text = stringResource(
                        R.string.xfeeder_label_system_context_item,
                        context.technology,
                        context.operatorName,
                        context.band,
                        context.connectedCells,
                        context.totalCells
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Text(
            text = stringResource(R.string.xfeeder_header_linked_cells),
            style = MaterialTheme.typography.labelLarge
        )
        if (state.sectorCells.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_sector_cells),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            state.sectorCells.forEach { cell ->
                Text(
                    text = stringResource(
                        R.string.xfeeder_label_cell_context_item,
                        cell.label,
                        cell.technology,
                        cell.operatorName,
                        cell.band,
                        if (cell.isConnected) {
                            stringResource(R.string.xfeeder_value_cell_connected)
                        } else {
                            stringResource(R.string.xfeeder_value_cell_not_connected)
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
internal fun SessionEntryChoiceCard(
    hasLatest: Boolean,
    isCreating: Boolean,
    onResumeLatest: () -> Unit,
    onCreateSession: () -> Unit
) {
    OperationalSectionCard(
        title = stringResource(R.string.xfeeder_entry_choice_title),
        subtitle = stringResource(R.string.xfeeder_entry_choice_hint)
    ) {
        if (hasLatest) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onResumeLatest
            ) {
                Text(stringResource(R.string.xfeeder_action_resume_latest))
            }
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCreating,
            onClick = onCreateSession
        ) {
            Text(
                text = if (isCreating) {
                    stringResource(R.string.xfeeder_action_create_session_loading)
                } else {
                    stringResource(R.string.xfeeder_action_create_session)
                }
            )
        }
    }
}

@Composable
internal fun SessionHistoryItemCard(
    session: XfeederGuidedSession,
    isSelected: Boolean,
    onOpen: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.xfeeder_label_history_item,
                    stringResource(xfeederSessionStatusLabelRes(session.status)),
                    formatHistoryEpoch(session.updatedAtEpochMillis)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.xfeeder_label_sector_outcome,
                    stringResource(xfeederSectorOutcomeLabelRes(session.sectorOutcome))
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSelected,
                onClick = onOpen
            ) {
                Text(
                    text = if (isSelected) {
                        stringResource(R.string.xfeeder_action_session_opened)
                    } else {
                        stringResource(R.string.xfeeder_action_open_session)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StepChecklistCard(
    step: XfeederGuidedStep,
    onStepStatusSelected: (XfeederStepCode, XfeederStepStatus) -> Unit
) {
    OperationalSectionCard(
        title = stringResource(xfeederStepCodeLabelRes(step.code)),
        subtitle = if (step.required) {
            stringResource(R.string.xfeeder_label_required_step)
        } else {
            stringResource(R.string.xfeeder_label_optional_step)
        }
    ) {
        OperationalSignalRow(
            signals = listOf(
                OperationalSignal(
                    text = stringResource(
                        R.string.xfeeder_label_step_status,
                        stringResource(xfeederStepStatusLabelRes(step.status))
                    ),
                    severity = stepStatusSeverity(step.status)
                )
            )
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            XfeederStepStatus.entries.forEach { status ->
                FilterChip(
                    selected = status == step.status,
                    onClick = { onStepStatusSelected(step.code, status) },
                    label = { Text(stringResource(xfeederStepStatusLabelRes(status))) }
                )
            }
        }
    }
}

private fun stepStatusSeverity(status: XfeederStepStatus): OperationalSeverity {
    return when (status) {
        XfeederStepStatus.DONE -> OperationalSeverity.SUCCESS
        XfeederStepStatus.BLOCKED -> OperationalSeverity.CRITICAL
        XfeederStepStatus.IN_PROGRESS -> OperationalSeverity.WARNING
        XfeederStepStatus.TODO -> OperationalSeverity.NORMAL
    }
}

private fun formatHistoryEpoch(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return formatter.format(
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    )
}
