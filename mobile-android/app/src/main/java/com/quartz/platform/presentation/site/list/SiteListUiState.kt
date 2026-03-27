package com.quartz.platform.presentation.site.list

import com.quartz.platform.domain.model.SiteSummary

data class SiteListUiState(
    val isLoading: Boolean = true,
    val sites: List<SiteSummary> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isBootstrappingDemo: Boolean = false
) {
    val isEmpty: Boolean = !isLoading && errorMessage == null && sites.isEmpty()
}
