package com.quartz.platform.presentation.home.map

import com.quartz.platform.domain.model.SiteSummary
import com.quartz.platform.domain.model.UserLocation

data class HomeMapUiState(
    val isLoading: Boolean = true,
    val sites: List<SiteSummary> = emptyList(),
    val filteredSites: List<SiteSummary> = emptyList(),
    val searchQuery: String = "",
    val selectedSiteId: String? = null,
    val cameraCenter: MapCoordinate? = null,
    val cameraRequestVersion: Int = 0,
    val userLocation: UserLocation? = null,
    val isRecenterInProgress: Boolean = false,
    val isBootstrappingDemo: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null
) {
    val selectedSite: SiteSummary?
        get() {
            val selected = selectedSiteId ?: return null
            return filteredSites.firstOrNull { it.id == selected } ?: sites.firstOrNull { it.id == selected }
        }

    val isEmpty: Boolean
        get() = sites.isEmpty()

    val hasNoSearchResults: Boolean
        get() = sites.isNotEmpty() && filteredSites.isEmpty()
}

data class MapCoordinate(
    val latitude: Double,
    val longitude: Double
)
