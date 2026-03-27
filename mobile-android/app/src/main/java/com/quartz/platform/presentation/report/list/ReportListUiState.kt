package com.quartz.platform.presentation.report.list

import com.quartz.platform.domain.model.SiteReportListItem

data class ReportListUiState(
    val siteId: String,
    val isLoading: Boolean = true,
    val reports: List<SiteReportListItem> = emptyList(),
    val retryingDraftIds: Set<String> = emptySet(),
    val infoMessage: String? = null,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean = !isLoading && errorMessage == null && reports.isEmpty()
}
