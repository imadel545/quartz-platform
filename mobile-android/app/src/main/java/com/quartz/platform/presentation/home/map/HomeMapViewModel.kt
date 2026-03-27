package com.quartz.platform.presentation.home.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quartz.platform.R
import com.quartz.platform.core.text.UiStrings
import com.quartz.platform.domain.model.SiteSummary
import com.quartz.platform.domain.usecase.BootstrapDemoSiteSnapshotUseCase
import com.quartz.platform.domain.usecase.GetLastKnownUserLocationUseCase
import com.quartz.platform.domain.usecase.ObserveSiteListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeMapViewModel @Inject constructor(
    private val observeSiteListUseCase: ObserveSiteListUseCase,
    private val bootstrapDemoSiteSnapshotUseCase: BootstrapDemoSiteSnapshotUseCase,
    private val getLastKnownUserLocationUseCase: GetLastKnownUserLocationUseCase,
    private val uiStrings: UiStrings
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeMapUiState())
    val uiState: StateFlow<HomeMapUiState> = _uiState.asStateFlow()

    init {
        observeSites()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            reduceStateAfterSiteChange(
                state = state.copy(searchQuery = query, errorMessage = null, infoMessage = null),
                sites = state.sites,
                forceCameraToSelected = true
            )
        }
    }

    fun onSiteSelected(siteId: String) {
        _uiState.update { state ->
            val selected = state.filteredSites.firstOrNull { it.id == siteId } ?: return@update state
            state.copy(
                selectedSiteId = selected.id,
                cameraCenter = selected.toCoordinate(),
                cameraRequestVersion = state.cameraRequestVersion + 1,
                errorMessage = null
            )
        }
    }

    fun onRecenterClicked() {
        val current = _uiState.value
        if (current.isRecenterInProgress) return

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isRecenterInProgress = true, errorMessage = null)
            }

            val location = runCatching { getLastKnownUserLocationUseCase() }.getOrNull()
            if (location != null) {
                _uiState.update { state ->
                    state.copy(
                        userLocation = location,
                        cameraCenter = MapCoordinate(location.latitude, location.longitude),
                        cameraRequestVersion = state.cameraRequestVersion + 1,
                        isRecenterInProgress = false,
                        infoMessage = null
                    )
                }
                return@launch
            }

            _uiState.update { state ->
                val fallback = state.selectedSite?.toCoordinate()
                state.copy(
                    cameraCenter = fallback ?: state.cameraCenter,
                    cameraRequestVersion = if (fallback != null) {
                        state.cameraRequestVersion + 1
                    } else {
                        state.cameraRequestVersion
                    },
                    isRecenterInProgress = false,
                    infoMessage = if (fallback != null) {
                        uiStrings.get(R.string.info_user_location_unavailable)
                    } else {
                        uiStrings.get(R.string.info_user_location_unavailable_no_site)
                    }
                )
            }
        }
    }

    fun onLoadDemoSnapshotClicked() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isBootstrappingDemo = true, errorMessage = null, infoMessage = null)
            }

            runCatching { bootstrapDemoSiteSnapshotUseCase() }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isBootstrappingDemo = false,
                            infoMessage = uiStrings.get(R.string.info_demo_snapshot_loaded)
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isBootstrappingDemo = false,
                            errorMessage = throwable.message ?: uiStrings.get(R.string.error_load_demo_snapshot)
                        )
                    }
                }
        }
    }

    private fun observeSites() {
        viewModelScope.launch {
            observeSiteListUseCase()
                .onStart {
                    _uiState.update { state ->
                        state.copy(isLoading = true, errorMessage = null)
                    }
                }
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: uiStrings.get(R.string.error_load_cached_sites)
                        )
                    }
                }
                .collect { sites ->
                    _uiState.update { state ->
                        reduceStateAfterSiteChange(state = state.copy(isLoading = false), sites = sites)
                    }
                }
        }
    }

    private fun reduceStateAfterSiteChange(
        state: HomeMapUiState,
        sites: List<SiteSummary>,
        forceCameraToSelected: Boolean = false
    ): HomeMapUiState {
        val filtered = filterSites(sites, state.searchQuery)
        val selectedId = when {
            filtered.any { it.id == state.selectedSiteId } -> state.selectedSiteId
            else -> filtered.firstOrNull()?.id
        }

        val selectedSite = filtered.firstOrNull { it.id == selectedId }
        val shouldMoveCamera = forceCameraToSelected || (state.cameraCenter == null && selectedSite != null)
        return state.copy(
            sites = sites,
            filteredSites = filtered,
            selectedSiteId = selectedId,
            cameraCenter = if (shouldMoveCamera && selectedSite != null) {
                selectedSite.toCoordinate()
            } else {
                state.cameraCenter
            },
            cameraRequestVersion = if (shouldMoveCamera && selectedSite != null) {
                state.cameraRequestVersion + 1
            } else {
                state.cameraRequestVersion
            }
        )
    }

    private fun filterSites(sites: List<SiteSummary>, query: String): List<SiteSummary> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return sites

        return sites.filter { site ->
            site.name.lowercase().contains(normalized) ||
                site.externalCode.lowercase().contains(normalized)
        }
    }

    private fun SiteSummary.toCoordinate(): MapCoordinate {
        return MapCoordinate(latitude = latitude, longitude = longitude)
    }
}
