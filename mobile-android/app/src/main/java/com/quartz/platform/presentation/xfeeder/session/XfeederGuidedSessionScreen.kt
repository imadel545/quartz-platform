package com.quartz.platform.presentation.xfeeder.session

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quartz.platform.R
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederSessionStatus
import com.quartz.platform.domain.model.XfeederStepCode
import com.quartz.platform.domain.model.XfeederStepStatus
import com.quartz.platform.domain.model.XfeederUnreliableReason
import com.quartz.platform.presentation.components.AdvancedDisclosureButton
import com.quartz.platform.presentation.components.OperationalEmptyStateCard
import com.quartz.platform.presentation.components.OperationalMessageCard
import com.quartz.platform.presentation.components.OperationalSeverity
import kotlinx.coroutines.flow.collectLatest

@Composable
fun XfeederGuidedSessionRoute(
    onBack: () -> Unit,
    onOpenDraft: (String) -> Unit,
    viewModel: XfeederGuidedSessionViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val requestLocationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onRefreshUserLocationClicked()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is XfeederGuidedSessionEvent.OpenDraft -> onOpenDraft(event.draftId)
                is XfeederGuidedSessionEvent.OpenNavigationToMeasurementZone -> {
                    val uri = Uri.parse(
                        "geo:${event.latitude},${event.longitude}?q=" +
                            "${event.latitude},${event.longitude}(${Uri.encode(event.label)})"
                    )
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        viewModel.onNavigationUnavailable()
                    }
                }
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
        onMeasurementZoneExtensionReasonChanged = viewModel::onMeasurementZoneExtensionReasonChanged,
        onProximityReferenceAltitudeChanged = viewModel::onProximityReferenceAltitudeChanged,
        onExtendMeasurementZoneClicked = viewModel::onExtendMeasurementZoneClicked,
        onResetMeasurementZoneClicked = viewModel::onResetMeasurementZoneClicked,
        onToggleProximityModeClicked = viewModel::onToggleProximityModeClicked,
        onRefreshUserLocationClicked = {
            val hasFine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasFine || hasCoarse) {
                viewModel.onRefreshUserLocationClicked()
            } else {
                requestLocationPermission.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        },
        onOpenNavigationToMeasurementZone = viewModel::onOpenNavigationToMeasurementZoneClicked,
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
    onMeasurementZoneExtensionReasonChanged: (String) -> Unit,
    onProximityReferenceAltitudeChanged: (String) -> Unit,
    onExtendMeasurementZoneClicked: () -> Unit,
    onResetMeasurementZoneClicked: () -> Unit,
    onToggleProximityModeClicked: (Boolean) -> Unit,
    onRefreshUserLocationClicked: () -> Unit,
    onOpenNavigationToMeasurementZone: () -> Unit,
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
                var showAdvancedMissionContext by rememberSaveable { mutableStateOf(false) }
                var showChecklist by rememberSaveable(state.session?.id) {
                    mutableStateOf(
                        state.session?.toProgressSnapshot()?.remainingRequiredSteps?.let { it > 0 } ?: true
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        XfeederMissionHeader(
                            state = state,
                            onCreateSession = onCreateSession,
                            onResumeLatest = onResumeLatest,
                            onSaveSummary = onSaveSummary,
                            onCreateReportDraft = onCreateReportDraft
                        )
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

                    state.infoMessage?.let { info ->
                        item {
                            OperationalMessageCard(
                                title = stringResource(R.string.home_runtime_info_title),
                                message = info,
                                severity = OperationalSeverity.NORMAL
                            )
                        }
                    }

                    state.completionGuardMessage?.let { guardMessage ->
                        item {
                            OperationalMessageCard(
                                title = stringResource(R.string.home_runtime_alert_title),
                                message = guardMessage,
                                severity = OperationalSeverity.WARNING
                            )
                        }
                    }

                    if (state.session == null) {
                        item {
                            XfeederRuntimeStateBanner(state = state)
                        }
                        item {
                            OperationalEmptyStateCard(
                                title = stringResource(R.string.xfeeder_runtime_empty_title),
                                message = stringResource(R.string.xfeeder_empty_session)
                            )
                        }
                        item {
                            SessionEntryChoiceCard(
                                hasLatest = state.sessionHistory.isNotEmpty(),
                                isCreating = state.isCreatingSession,
                                onResumeLatest = onResumeLatest,
                                onCreateSession = onCreateSession
                            )
                        }
                    } else {
                        item {
                            XfeederRuntimeStateBanner(state = state)
                        }

                        item {
                            XfeederMissionProgressCard(state = state)
                        }

                        item {
                            OutcomeCaptureCard(
                                state = state,
                                onSessionStatusSelected = onSessionStatusSelected,
                                onSectorOutcomeSelected = onSectorOutcomeSelected,
                                onRelatedSectorCodeChanged = onRelatedSectorCodeChanged,
                                onUnreliableReasonSelected = onUnreliableReasonSelected,
                                onObservedSectorCountChanged = onObservedSectorCountChanged,
                                onNotesChanged = onNotesChanged,
                                onResultSummaryChanged = onResultSummaryChanged,
                                onSaveSummary = onSaveSummary,
                                onCreateReportDraft = onCreateReportDraft
                            )
                        }

                        item {
                            GeospatialSessionSurfaceCard(
                                state = state,
                                onMeasurementZoneExtensionReasonChanged = onMeasurementZoneExtensionReasonChanged,
                                onProximityReferenceAltitudeChanged = onProximityReferenceAltitudeChanged,
                                onExtendMeasurementZoneClicked = onExtendMeasurementZoneClicked,
                                onResetMeasurementZoneClicked = onResetMeasurementZoneClicked,
                                onToggleProximityModeClicked = onToggleProximityModeClicked,
                                onRefreshUserLocationClicked = onRefreshUserLocationClicked,
                                onOpenNavigationToMeasurementZone = onOpenNavigationToMeasurementZone
                            )
                        }

                        item {
                            AdvancedDisclosureButton(
                                expanded = showChecklist,
                                onToggle = { showChecklist = !showChecklist },
                                showLabel = stringResource(R.string.xfeeder_action_show_checklist),
                                hideLabel = stringResource(R.string.xfeeder_action_hide_checklist)
                            )
                        }

                        if (showChecklist) {
                            items(state.session.steps, key = { step -> step.code.name }) { step ->
                                StepChecklistCard(
                                    step = step,
                                    onStepStatusSelected = onStepStatusSelected
                                )
                            }
                        }

                        item {
                            AdvancedDisclosureButton(
                                expanded = showAdvancedMissionContext,
                                onToggle = { showAdvancedMissionContext = !showAdvancedMissionContext },
                                showLabel = stringResource(
                                    R.string.xfeeder_action_show_advanced_context,
                                    state.sessionHistory.size
                                ),
                                hideLabel = stringResource(
                                    R.string.xfeeder_action_hide_advanced_context,
                                    state.sessionHistory.size
                                )
                            )
                        }

                        if (showAdvancedMissionContext) {
                            if (state.sessionHistory.isNotEmpty()) {
                                items(state.sessionHistory, key = { historyItem -> historyItem.id }) { historyItem ->
                                    SessionHistoryItemCard(
                                        session = historyItem,
                                        isSelected = historyItem.id == state.session.id,
                                        onOpen = { onOpenHistorySession(historyItem.id) }
                                    )
                                }
                            }
                            item {
                                SectorCellsContextCard(state = state)
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
