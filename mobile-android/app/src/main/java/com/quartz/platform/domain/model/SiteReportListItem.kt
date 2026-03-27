package com.quartz.platform.domain.model

data class SiteReportListItem(
    val draftId: String,
    val siteId: String,
    val title: String,
    val revision: Int,
    val updatedAtEpochMillis: Long,
    val syncTrace: ReportSyncTrace
) {
    val syncState: ReportSyncState
        get() = syncTrace.state
}
