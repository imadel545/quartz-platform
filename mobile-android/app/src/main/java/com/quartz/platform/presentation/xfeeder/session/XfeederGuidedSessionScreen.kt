package com.quartz.platform.presentation.xfeeder.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quartz.platform.R
import com.quartz.platform.domain.model.XfeederGuidedSession
import com.quartz.platform.domain.model.XfeederGuidedStep
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederSessionStatus
import com.quartz.platform.domain.model.XfeederStepCode
import com.quartz.platform.domain.model.XfeederStepStatus
import com.quartz.platform.domain.model.XfeederUnreliableReason
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.collectLatest

@Composable
fun XfeederGuidedSessionRoute(
    onBack: () -> Unit,
    onOpenDraft: (String) -> Unit,
    viewModel: XfeederGuidedSessionViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is XfeederGuidedSessionEvent.OpenDraft -> onOpenDraft(event.draftId)
            }
        }
    }

    XfeederGuidedSessionScreen(
        state = state,
        onBack = onBack,
        onCreateSession = viewModel::onCreateSessionClicked,
        onResumeLatest = viewModel::onResumeLatestClicked,
        onOpenHistorySession = viewModel::onSelectHistorySessionClicked,
        onStepStatusSelected = viewModel::onStepStatusSelected,
        onSessionStatusSelected = viewModel::onSessionStatusSelected,
        onSectorOutcomeSelected = viewModel::onSectorOutcomeSelected,
        onRelatedSectorCodeChanged = viewModel::onRelatedSectorCodeChanged,
        onUnreliableReasonSelected = viewModel::onUnreliableReasonSelected,
        onObservedSectorCountChanged = viewModel::onObservedSectorCountChanged,
        onNotesChanged = viewModel::onNotesChanged,
        onResultSummaryChanged = viewModel::onResultSummaryChanged,
        onSaveSummary = viewModel::onSaveSummaryClicked,
        onCreateReportDraft = viewModel::onCreateReportDraftClicked
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XfeederGuidedSessionScreen(
    state: XfeederGuidedSessionUiState,
    onBack: () -> Unit,
    onCreateSession: () -> Unit,
    onResumeLatest: () -> Unit,
    onOpenHistorySession: (String) -> Unit,
    onStepStatusSelected: (XfeederStepCode, XfeederStepStatus) -> Unit,
    onSessionStatusSelected: (XfeederSessionStatus) -> Unit,
    onSectorOutcomeSelected: (XfeederSectorOutcome) -> Unit,
    onRelatedSectorCodeChanged: (String) -> Unit,
    onUnreliableReasonSelected: (XfeederUnreliableReason?) -> Unit,
    onObservedSectorCountChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onResultSummaryChanged: (String) -> Unit,
    onSaveSummary: () -> Unit,
    onCreateReportDraft: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_xfeeder_guided_session)) }
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

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.xfeeder_label_site_sector,
                                        if (state.siteLabel.isBlank()) state.siteId else state.siteLabel,
                                        state.sectorCode.ifBlank { state.sectorId }
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = stringResource(R.string.xfeeder_shell_disclaimer),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    state.errorMessage?.let { error ->
                        item {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    state.infoMessage?.let { info ->
                        item {
                            Text(
                                text = info,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    state.completionGuardMessage?.let { guardMessage ->
                        item {
                            Text(
                                text = guardMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    item {
                        SessionEntryChoiceCard(
                            hasLatest = state.sessionHistory.isNotEmpty(),
                            isCreating = state.isCreatingSession,
                            onResumeLatest = onResumeLatest,
                            onCreateSession = onCreateSession
                        )
                    }

                    if (state.session == null) {
                        item {
                            Text(
                                text = stringResource(R.string.xfeeder_empty_session),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = stringResource(
                                    R.string.xfeeder_header_session_history,
                                    state.sessionHistory.size
                                ),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        items(state.sessionHistory, key = { it.id }) { historyItem ->
                            SessionHistoryItemCard(
                                session = historyItem,
                                isSelected = historyItem.id == state.session.id,
                                onOpen = { onOpenHistorySession(historyItem.id) }
                            )
                        }

                        item {
                            SessionSummaryCard(state = state, onSessionStatusSelected = onSessionStatusSelected)
                        }

                        item {
                            SectorOutcomeCard(
                                selectedOutcome = state.selectedOutcome,
                                onSectorOutcomeSelected = onSectorOutcomeSelected
                            )
                        }

                        item {
                            ClosureEvidenceCard(
                                state = state,
                                onRelatedSectorCodeChanged = onRelatedSectorCodeChanged,
                                onUnreliableReasonSelected = onUnreliableReasonSelected,
                                onObservedSectorCountChanged = onObservedSectorCountChanged
                            )
                        }

                        item {
                            Text(
                                text = stringResource(R.string.xfeeder_header_checklist),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        items(state.session.steps, key = { it.code.name }) { step ->
                            StepChecklistCard(
                                step = step,
                                onStepStatusSelected = onStepStatusSelected
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = state.notesInput,
                                onValueChange = onNotesChanged,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.xfeeder_input_notes)) }
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = state.resultSummaryInput,
                                onValueChange = onResultSummaryChanged,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.xfeeder_input_result_summary)) }
                            )
                        }

                        item {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.hasUnsavedChanges && !state.isSavingSummary,
                                onClick = onSaveSummary
                            ) {
                                Text(
                                    text = if (state.isSavingSummary) {
                                        stringResource(R.string.xfeeder_action_save_summary_loading)
                                    } else {
                                        stringResource(R.string.xfeeder_action_save_summary)
                                    }
                                )
                            }
                        }

                        item {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.isCreatingDraft,
                                onClick = onCreateReportDraft
                            ) {
                                Text(
                                    text = if (state.isCreatingDraft) {
                                        stringResource(R.string.xfeeder_action_create_report_draft_loading)
                                    } else {
                                        stringResource(R.string.xfeeder_action_create_report_draft)
                                    }
                                )
                            }
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
private fun ClosureEvidenceCard(
    state: XfeederGuidedSessionUiState,
    onRelatedSectorCodeChanged: (String) -> Unit,
    onUnreliableReasonSelected: (XfeederUnreliableReason?) -> Unit,
    onObservedSectorCountChanged: (String) -> Unit
) {
    val requiresRelatedSector = state.selectedOutcome == XfeederSectorOutcome.CROSSED ||
        state.selectedOutcome == XfeederSectorOutcome.MIXFEEDER
    val requiresUnreliableDetails = state.selectedOutcome == XfeederSectorOutcome.UNRELIABLE

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.xfeeder_header_closure_evidence),
                style = MaterialTheme.typography.titleSmall
            )

            if (!requiresRelatedSector && !requiresUnreliableDetails) {
                Text(
                    text = stringResource(R.string.xfeeder_closure_no_additional_evidence),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (requiresRelatedSector) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.relatedSectorCodeInput,
                    onValueChange = onRelatedSectorCodeChanged,
                    label = { Text(stringResource(R.string.xfeeder_input_related_sector_code)) },
                    supportingText = { Text(stringResource(R.string.xfeeder_hint_related_sector_code)) }
                )
            }

            if (requiresUnreliableDetails) {
                Text(
                    text = stringResource(R.string.xfeeder_label_unreliable_reason),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = stringResource(
                        R.string.xfeeder_label_selected_unreliable_reason,
                        state.selectedUnreliableReason?.let { reason ->
                            stringResource(xfeederUnreliableReasonLabelRes(reason))
                        } ?: stringResource(R.string.value_not_available)
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                XfeederUnreliableReason.entries.forEach { reason ->
                    AssistChip(
                        onClick = { onUnreliableReasonSelected(reason) },
                        label = { Text(stringResource(xfeederUnreliableReasonLabelRes(reason))) }
                    )
                }
                AssistChip(
                    onClick = { onUnreliableReasonSelected(null) },
                    label = { Text(stringResource(R.string.xfeeder_action_clear_unreliable_reason)) }
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.observedSectorCountInput,
                    onValueChange = onObservedSectorCountChanged,
                    label = { Text(stringResource(R.string.xfeeder_input_observed_sector_count)) },
                    supportingText = { Text(stringResource(R.string.xfeeder_hint_observed_sector_count)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }
}

@Composable
private fun SessionEntryChoiceCard(
    hasLatest: Boolean,
    isCreating: Boolean,
    onResumeLatest: () -> Unit,
    onCreateSession: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.xfeeder_entry_choice_title),
                style = MaterialTheme.typography.titleSmall
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = hasLatest,
                onClick = onResumeLatest
            ) {
                Text(stringResource(R.string.xfeeder_action_resume_latest))
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
}

@Composable
private fun SessionHistoryItemCard(
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
                    formatEpoch(session.updatedAtEpochMillis)
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

@Composable
private fun SessionSummaryCard(
    state: XfeederGuidedSessionUiState,
    onSessionStatusSelected: (XfeederSessionStatus) -> Unit
) {
    val session = requireNotNull(state.session)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        stringResource(
                            R.string.xfeeder_label_session_status,
                            stringResource(xfeederSessionStatusLabelRes(session.status))
                        )
                    )
                }
            )
            Text(
                text = stringResource(
                    R.string.label_updated_at,
                    formatEpoch(session.updatedAtEpochMillis)
                ),
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = stringResource(R.string.xfeeder_label_status_update),
                style = MaterialTheme.typography.labelLarge
            )
            XfeederSessionStatus.entries.forEach { status ->
                AssistChip(
                    onClick = { onSessionStatusSelected(status) },
                    label = { Text(stringResource(xfeederSessionStatusLabelRes(status))) }
                )
            }
        }
    }
}

@Composable
private fun SectorOutcomeCard(
    selectedOutcome: XfeederSectorOutcome,
    onSectorOutcomeSelected: (XfeederSectorOutcome) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.xfeeder_label_sector_outcome,
                    stringResource(xfeederSectorOutcomeLabelRes(selectedOutcome))
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            XfeederSectorOutcome.entries.forEach { outcome ->
                AssistChip(
                    onClick = { onSectorOutcomeSelected(outcome) },
                    label = { Text(stringResource(xfeederSectorOutcomeLabelRes(outcome))) }
                )
            }
        }
    }
}

@Composable
private fun StepChecklistCard(
    step: XfeederGuidedStep,
    onStepStatusSelected: (XfeederStepCode, XfeederStepStatus) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(xfeederStepCodeLabelRes(step.code)),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (step.required) {
                    stringResource(R.string.xfeeder_label_required_step)
                } else {
                    stringResource(R.string.xfeeder_label_optional_step)
                },
                style = MaterialTheme.typography.bodySmall
            )
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        stringResource(
                            R.string.xfeeder_label_step_status,
                            stringResource(xfeederStepStatusLabelRes(step.status))
                        )
                    )
                }
            )
            XfeederStepStatus.entries.forEach { status ->
                AssistChip(
                    onClick = { onStepStatusSelected(step.code, status) },
                    label = { Text(stringResource(xfeederStepStatusLabelRes(status))) }
                )
            }
        }
    }
}

private fun formatEpoch(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return formatter.format(
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    )
}
