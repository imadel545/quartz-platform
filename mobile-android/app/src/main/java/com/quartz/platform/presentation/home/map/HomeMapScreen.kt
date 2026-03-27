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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quartz.platform.R
import com.quartz.platform.domain.model.SiteSummary
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun HomeMapRoute(
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.hint_site_search)) },
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !state.isBootstrappingDemo,
                    onClick = onLoadDemoSnapshot
                ) {
                    Text(
                        if (state.isBootstrappingDemo) {
                            stringResource(R.string.action_load_demo_snapshot_loading)
                        } else {
                            stringResource(R.string.action_load_demo_snapshot)
                        }
                    )
                }
            }

            state.errorMessage?.let { error ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            state.infoMessage?.let { info ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = info,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (state.isEmpty) {
                EmptyMapState(
                    isBootstrappingDemo = state.isBootstrappingDemo,
                    onLoadDemoSnapshot = onLoadDemoSnapshot
                )
                return@Column
            }

            Text(
                text = stringResource(R.string.label_sites_found, state.filteredSites.size),
                style = MaterialTheme.typography.bodySmall
            )

            if (state.hasNoSearchResults) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.empty_map_no_search_result),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
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

            state.selectedSite?.let { selected ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_selected_site),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = selected.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.label_site_code, selected.externalCode),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onOpenSelectedSite(selected.id) }
                        ) {
                            Text(stringResource(R.string.action_open_selected_site))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMapState(
    isBootstrappingDemo: Boolean,
    onLoadDemoSnapshot: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.empty_site_snapshot_cache),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.empty_site_snapshot_cache_hint),
                style = MaterialTheme.typography.bodySmall
            )
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
