package com.quartz.platform.presentation.report.list

import com.quartz.platform.domain.model.SiteReportListItem
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType

data class ReportListUiState(
    val siteId: String,
    val isLoading: Boolean = true,
    val reports: List<SiteReportListItem> = emptyList(),
    val selectedFilter: ReportListFilter = ReportListFilter.ALL,
    val retryingDraftIds: Set<String> = emptySet(),
    val infoMessage: String? = null,
    val errorMessage: String? = null
) {
    val filteredReports: List<SiteReportListItem>
        get() = reports.filter(selectedFilter::matches)

    val isEmpty: Boolean = !isLoading && errorMessage == null && reports.isEmpty()

    val isFilterEmpty: Boolean
        get() = !isLoading && errorMessage == null && reports.isNotEmpty() && filteredReports.isEmpty()
}

enum class ReportListFilter {
    ALL,
    XFEEDER,
    RET,
    PERFORMANCE,
    NON_GUIDED;

    fun matches(item: SiteReportListItem): Boolean {
        return when (this) {
            ALL -> true
            XFEEDER -> item.originWorkflowType == ReportDraftOriginWorkflowType.XFEEDER
            RET -> item.originWorkflowType == ReportDraftOriginWorkflowType.RET
            PERFORMANCE -> item.originWorkflowType == ReportDraftOriginWorkflowType.PERFORMANCE
            NON_GUIDED -> item.originWorkflowType == null
        }
    }

    companion object {
        fun fromPersistedNameOrDefault(raw: String?): ReportListFilter {
            return entries.firstOrNull { it.name == raw } ?: ALL
        }

        fun forWorkflowTypeOrDefault(
            workflowType: ReportDraftOriginWorkflowType?
        ): ReportListFilter {
            return when (workflowType) {
                null -> NON_GUIDED
                ReportDraftOriginWorkflowType.XFEEDER -> XFEEDER
                ReportDraftOriginWorkflowType.RET -> RET
                ReportDraftOriginWorkflowType.PERFORMANCE -> PERFORMANCE
            }
        }
    }
}
