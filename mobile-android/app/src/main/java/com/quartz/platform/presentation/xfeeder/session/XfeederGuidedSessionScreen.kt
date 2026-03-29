package com.quartz.platform.presentation.xfeeder.session

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quartz.platform.R
import com.quartz.platform.domain.model.XfeederGuidedSession
import com.quartz.platform.domain.model.XfeederGuidedStep
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederSessionStatus
import com.quartz.platform.domain.model.XfeederStepCode
import com.quartz.platform.domain.model.XfeederStepStatus
import com.quartz.platform.domain.model.XfeederUnreliableReason
import com.quartz.platform.domain.model.XfeederProximityEligibilityState
import com.quartz.platform.domain.model.XfeederReferenceAltitudeSourceState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
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
                            SectorCellsContextCard(state = state)
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
private fun GeospatialSessionSurfaceCard(
    state: XfeederGuidedSessionUiState,
    onMeasurementZoneExtensionReasonChanged: (String) -> Unit,
    onProximityReferenceAltitudeChanged: (String) -> Unit,
    onExtendMeasurementZoneClicked: () -> Unit,
    onResetMeasurementZoneClicked: () -> Unit,
    onToggleProximityModeClicked: (Boolean) -> Unit,
    onRefreshUserLocationClicked: () -> Unit,
    onOpenNavigationToMeasurementZone: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.xfeeder_header_geospatial_surface),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(
                    R.string.xfeeder_label_measurement_zone_radius,
                    state.measurementZoneRadiusMeters
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.xfeeder_label_sector_azimuth,
                    state.sectorAzimuthDegrees?.toString() ?: stringResource(R.string.value_not_available)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.xfeeder_label_distance_to_zone,
                    state.distanceToMeasurementZoneMeters?.toString() ?: stringResource(R.string.value_not_available)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.xfeeder_label_zone_membership,
                    when (state.isInsideMeasurementZone) {
                        true -> stringResource(R.string.xfeeder_value_inside_zone)
                        false -> stringResource(R.string.xfeeder_value_outside_zone)
                        null -> stringResource(R.string.value_not_available)
                    }
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.xfeeder_label_proximity_eligibility,
                    when (state.proximityEligibilityState) {
                        XfeederProximityEligibilityState.ELIGIBLE -> {
                            stringResource(R.string.xfeeder_value_proximity_eligible)
                        }
                        XfeederProximityEligibilityState.INELIGIBLE -> {
                            stringResource(R.string.xfeeder_value_proximity_ineligible)
                        }
                        XfeederProximityEligibilityState.SUPPORTED -> {
                            stringResource(R.string.xfeeder_value_proximity_supported)
                        }
                        XfeederProximityEligibilityState.UNAVAILABLE -> {
                            stringResource(R.string.xfeeder_value_proximity_unavailable)
                        }
                    }
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.xfeeder_label_user_altitude,
                    state.userAltitudeMeters?.let { String.format("%.1f", it) }
                        ?: stringResource(R.string.value_not_available)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.xfeeder_label_effective_reference_altitude,
                    state.effectiveReferenceAltitudeMeters?.let { String.format("%.1f", it) }
                        ?: stringResource(R.string.value_not_available)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.xfeeder_label_technical_reference_altitude,
                    state.technicalReferenceAltitudeMeters?.let { String.format("%.1f", it) }
                        ?: stringResource(R.string.value_not_available)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.xfeeder_label_reference_altitude_source,
                    when (state.proximityReferenceAltitudeSource) {
                        XfeederReferenceAltitudeSourceState.TECHNICAL_DEFAULT ->
                            stringResource(R.string.xfeeder_value_reference_altitude_source_technical_default)
                        XfeederReferenceAltitudeSourceState.OPERATOR_OVERRIDE ->
                            stringResource(R.string.xfeeder_value_reference_altitude_source_operator_override)
                        XfeederReferenceAltitudeSourceState.UNAVAILABLE ->
                            stringResource(R.string.xfeeder_value_reference_altitude_source_unavailable)
                    }
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.xfeeder_label_operator_override_altitude,
                    state.proximityReferenceAltitudeInput.ifBlank {
                        stringResource(R.string.value_not_available)
                    }
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = when (state.proximityEligibilityState) {
                    XfeederProximityEligibilityState.ELIGIBLE -> {
                        stringResource(R.string.xfeeder_helper_proximity_eligible)
                    }
                    XfeederProximityEligibilityState.INELIGIBLE -> {
                        stringResource(R.string.xfeeder_helper_proximity_ineligible)
                    }
                    XfeederProximityEligibilityState.SUPPORTED -> {
                        stringResource(R.string.xfeeder_helper_proximity_supported)
                    }
                    XfeederProximityEligibilityState.UNAVAILABLE -> {
                        stringResource(R.string.xfeeder_helper_proximity_unavailable)
                    }
                },
                style = MaterialTheme.typography.bodySmall
            )

            XfeederSessionMapView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(top = 4.dp),
                siteLatitude = state.siteLatitude,
                siteLongitude = state.siteLongitude,
                measurementLatitude = state.measurementZoneLatitude,
                measurementLongitude = state.measurementZoneLongitude,
                measurementZoneRadiusMeters = state.measurementZoneRadiusMeters,
                userLatitude = state.userLocation?.latitude,
                userLongitude = state.userLocation?.longitude
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.measurementZoneExtensionReasonInput,
                onValueChange = onMeasurementZoneExtensionReasonChanged,
                label = { Text(stringResource(R.string.xfeeder_input_zone_extension_reason)) },
                supportingText = { Text(stringResource(R.string.xfeeder_hint_zone_extension_reason)) }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.proximityReferenceAltitudeInput,
                onValueChange = onProximityReferenceAltitudeChanged,
                label = { Text(stringResource(R.string.xfeeder_input_reference_altitude_override)) },
                supportingText = { Text(stringResource(R.string.xfeeder_hint_reference_altitude_override)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onExtendMeasurementZoneClicked
                ) {
                    Text(stringResource(R.string.xfeeder_action_extend_zone))
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onResetMeasurementZoneClicked
                ) {
                    Text(stringResource(R.string.xfeeder_action_reset_zone))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !state.isRefreshingLocation,
                    onClick = onRefreshUserLocationClicked
                ) {
                    Text(
                        text = if (state.isRefreshingLocation) {
                            stringResource(R.string.action_recenter_loading)
                        } else {
                            stringResource(R.string.xfeeder_action_refresh_position)
                        }
                    )
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onOpenNavigationToMeasurementZone
                ) {
                    Text(stringResource(R.string.xfeeder_action_open_navigation_to_zone))
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onToggleProximityModeClicked(!state.proximityModeEnabled) }
            ) {
                Text(
                    text = if (state.proximityModeEnabled) {
                        stringResource(R.string.xfeeder_action_disable_proximity)
                    } else {
                        stringResource(R.string.xfeeder_action_enable_proximity)
                    }
                )
            }
        }
    }
}

@Composable
@Suppress("DEPRECATION")
private fun XfeederSessionMapView(
    modifier: Modifier,
    siteLatitude: Double?,
    siteLongitude: Double?,
    measurementLatitude: Double?,
    measurementLongitude: Double?,
    measurementZoneRadiusMeters: Int,
    userLatitude: Double?,
    userLongitude: Double?
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(16.0)
        }
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDetach()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    var overlaysVersion by remember {
        mutableIntStateOf(-1)
    }

    val currentVersion = listOf(
        siteLatitude,
        siteLongitude,
        measurementLatitude,
        measurementLongitude,
        measurementZoneRadiusMeters,
        userLatitude,
        userLongitude
    ).hashCode()

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { map ->
            if (overlaysVersion == currentVersion) return@AndroidView
            map.overlays.removeAll { it is Marker || it is Polygon }

            val measurementPoint = if (measurementLatitude != null && measurementLongitude != null) {
                GeoPoint(measurementLatitude, measurementLongitude)
            } else {
                null
            }

            if (siteLatitude != null && siteLongitude != null) {
                map.overlays.add(
                    Marker(map).apply {
                        position = GeoPoint(siteLatitude, siteLongitude)
                        title = context.getString(R.string.xfeeder_map_marker_site)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                )
            }

            measurementPoint?.let { zonePoint ->
                map.overlays.add(
                    Marker(map).apply {
                        position = zonePoint
                        title = context.getString(R.string.xfeeder_map_marker_zone_target)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        alpha = 0.9f
                    }
                )
                map.overlays.add(
                    Polygon(map).apply {
                        points = Polygon.pointsAsCircle(zonePoint, measurementZoneRadiusMeters.toDouble())
                        setFillColor(0x220099FF)
                        setStrokeColor(0x990066CC.toInt())
                        setStrokeWidth(2f)
                    }
                )
            }

            if (userLatitude != null && userLongitude != null) {
                map.overlays.add(
                    Marker(map).apply {
                        position = GeoPoint(userLatitude, userLongitude)
                        title = context.getString(R.string.label_user_position_marker)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                )
            }

            val center = measurementPoint
                ?: if (siteLatitude != null && siteLongitude != null) GeoPoint(siteLatitude, siteLongitude) else null
            center?.let { map.controller.setCenter(it) }
            map.invalidate()
            overlaysVersion = currentVersion
        }
    )
}

@Composable
private fun SectorCellsContextCard(state: XfeederGuidedSessionUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.xfeeder_header_sector_cells_context),
                style = MaterialTheme.typography.titleSmall
            )
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
