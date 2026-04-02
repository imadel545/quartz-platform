package com.quartz.platform.presentation.home.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quartz.platform.R
import com.quartz.platform.domain.model.SiteSummary
import com.quartz.platform.presentation.components.AdvancedDisclosureButton
import com.quartz.platform.presentation.components.OperationalSectionCard
import com.quartz.platform.presentation.components.OperationalSeverity
import com.quartz.platform.presentation.components.OperationalSignal
import com.quartz.platform.presentation.components.OperationalSignalRow
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun HomeMapRoute(
    onOpenControlTower: () -> Unit,
    onSiteSelected: (String) -> Unit,
    viewModel: HomeMapViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current

    val requestLocationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onRecenterClicked()
    }

    HomeMapScreen(
        state = state,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onLoadDemoSnapshot = viewModel::onLoadDemoSnapshotClicked,
        onMapSiteSelected = viewModel::onSiteSelected,
        onOpenControlTower = onOpenControlTower,
        onOpenSelectedSite = { siteId -> onSiteSelected(siteId) },
        onRecenter = {
            val hasFine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasFine || hasCoarse) {
                viewModel.onRecenterClicked()
            } else {
                requestLocationPermission.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMapScreen(
    state: HomeMapUiState,
    onSearchQueryChanged: (String) -> Unit,
    onLoadDemoSnapshot: () -> Unit,
    onMapSiteSelected: (String) -> Unit,
    onOpenControlTower: () -> Unit,
    onOpenSelectedSite: (String) -> Unit,
    onRecenter: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.title_home_map)) })
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        var showSupportActions by rememberSaveable { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeMissionEntryCard(
                state = state,
                onOpenControlTower = onOpenControlTower,
                onOpenSelectedSite = onOpenSelectedSite,
                onRecenter = onRecenter
            )

            OperationalSectionCard(
                title = stringResource(R.string.home_map_section_targeting_title),
                subtitle = stringResource(R.string.home_map_section_targeting_hint)
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.hint_site_search)) },
                    singleLine = true
                )
                Text(
                    text = stringResource(R.string.label_sites_found, state.filteredSites.size),
                    style = MaterialTheme.typography.bodySmall
                )
                if (state.hasNoSearchResults) {
                    Text(
                        text = stringResource(R.string.empty_map_no_search_result),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            state.errorMessage?.let { error ->
                RuntimeMessageCard(
                    message = error,
                    isError = true
                )
            }

            state.infoMessage?.let { info ->
                RuntimeMessageCard(
                    message = info,
                    isError = false
                )
            }

            if (state.isEmpty) {
                EmptyMapState(
                    isBootstrappingDemo = state.isBootstrappingDemo,
                    onLoadDemoSnapshot = onLoadDemoSnapshot
                )
            } else {
                OperationalSectionCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.home_map_section_title),
                    subtitle = stringResource(R.string.home_map_section_hint)
                ) {
                    QuartzHomeMapView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        sites = state.filteredSites,
                        selectedSiteId = state.selectedSiteId,
                        userLocation = state.userLocation?.let {
                            MapCoordinate(it.latitude, it.longitude)
                        },
                        cameraCenter = state.cameraCenter,
                        cameraRequestVersion = state.cameraRequestVersion,
                        onSiteSelected = onMapSiteSelected
                    )
                }
            }

            AdvancedDisclosureButton(
                expanded = showSupportActions,
                onToggle = { showSupportActions = !showSupportActions },
                showLabel = stringResource(R.string.home_action_show_support_actions),
                hideLabel = stringResource(R.string.home_action_hide_support_actions)
            )

            if (showSupportActions) {
                HomeSecondaryActionsCard(
                    isBootstrappingDemo = state.isBootstrappingDemo,
                    onLoadDemoSnapshot = onLoadDemoSnapshot
                )
            }
        }
    }
}

@Composable
private fun HomeMissionEntryCard(
    state: HomeMapUiState,
    onOpenControlTower: () -> Unit,
    onOpenSelectedSite: (String) -> Unit,
    onRecenter: () -> Unit
) {
    val selectionLabel = state.selectedSite?.let { selected ->
        stringResource(R.string.home_mission_selected_site, selected.name)
    } ?: stringResource(R.string.home_mission_no_selected_site)
    val visibleSiteCode = state.selectedSite?.externalCode
        ?: state.filteredSites.firstOrNull()?.externalCode
        ?: state.sites.firstOrNull()?.externalCode

    val locationSignal = if (state.userLocation != null) {
        OperationalSignal(
            text = stringResource(R.string.home_signal_location_ready),
            severity = OperationalSeverity.SUCCESS
        )
    } else {
        OperationalSignal(
            text = stringResource(R.string.home_signal_location_missing),
            severity = OperationalSeverity.WARNING
        )
    }

    val cacheSignal = OperationalSignal(
        text = stringResource(
            R.string.home_signal_cache_sites,
            state.sites.size,
            state.filteredSites.size
        )
    )

    OperationalSectionCard(
        title = stringResource(R.string.home_mission_title),
        subtitle = selectionLabel
    ) {
        OperationalSignalRow(
            signals = listOf(locationSignal, cacheSignal)
        )
        visibleSiteCode?.let { code ->
            Text(
                text = stringResource(R.string.label_site_code, code),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onOpenControlTower
            ) {
                Text(stringResource(R.string.action_open_control_tower))
            }
            Button(
                modifier = Modifier.weight(1f),
                enabled = !state.isRecenterInProgress,
                onClick = onRecenter
            ) {
                Text(
                    if (state.isRecenterInProgress) {
                        stringResource(R.string.action_recenter_loading)
                    } else {
                        stringResource(R.string.action_recenter)
                    }
                )
            }
        }

        state.selectedSite?.let { selected ->
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenSelectedSite(selected.id) }
            ) {
                Text(stringResource(R.string.home_action_open_site_intelligence))
            }
        }
    }
}

@Composable
private fun RuntimeMessageCard(
    message: String,
    isError: Boolean
) {
    OperationalSectionCard(
        title = if (isError) {
            stringResource(R.string.home_runtime_alert_title)
        } else {
            stringResource(R.string.home_runtime_info_title)
        }
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HomeSecondaryActionsCard(
    isBootstrappingDemo: Boolean,
    onLoadDemoSnapshot: () -> Unit
) {
    OperationalSectionCard(
        title = stringResource(R.string.home_section_secondary_actions),
        subtitle = stringResource(R.string.home_section_secondary_actions_hint)
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isBootstrappingDemo,
            onClick = onLoadDemoSnapshot
        ) {
            Text(
                if (isBootstrappingDemo) {
                    stringResource(R.string.action_load_demo_snapshot_loading)
                } else {
                    stringResource(R.string.action_load_demo_snapshot)
                }
            )
        }
    }
}

@Composable
private fun EmptyMapState(
    isBootstrappingDemo: Boolean,
    onLoadDemoSnapshot: () -> Unit
) {
    OperationalSectionCard(
        title = stringResource(R.string.empty_site_snapshot_cache),
        subtitle = stringResource(R.string.empty_site_snapshot_cache_hint)
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isBootstrappingDemo,
            onClick = onLoadDemoSnapshot
        ) {
            Text(
                if (isBootstrappingDemo) {
                    stringResource(R.string.action_load_demo_snapshot_loading)
                } else {
                    stringResource(R.string.action_load_demo_snapshot)
                }
            )
        }
    }
}

@Composable
private fun QuartzHomeMapView(
    modifier: Modifier,
    sites: List<SiteSummary>,
    selectedSiteId: String?,
    userLocation: MapCoordinate?,
    cameraCenter: MapCoordinate?,
    cameraRequestVersion: Int,
    onSiteSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.0)
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

    var appliedCameraRequest by remember { mutableIntStateOf(-1) }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { map ->
            map.overlays.removeAll { it is Marker }

            sites.forEach { site ->
                val marker = Marker(map).apply {
                    position = GeoPoint(site.latitude, site.longitude)
                    title = site.name
                    subDescription = site.externalCode
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    alpha = if (site.id == selectedSiteId) 1f else 0.75f
                    setOnMarkerClickListener { _, _ ->
                        onSiteSelected(site.id)
                        true
                    }
                }
                map.overlays.add(marker)
            }

            userLocation?.let { location ->
                map.overlays.add(
                    Marker(map).apply {
                        position = GeoPoint(location.latitude, location.longitude)
                        title = context.getString(R.string.label_user_position_marker)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        alpha = 0.9f
                    }
                )
            }

            if (cameraCenter != null && cameraRequestVersion != appliedCameraRequest) {
                map.controller.animateTo(GeoPoint(cameraCenter.latitude, cameraCenter.longitude))
                appliedCameraRequest = cameraRequestVersion
            }

            map.invalidate()
        }
    )
}
