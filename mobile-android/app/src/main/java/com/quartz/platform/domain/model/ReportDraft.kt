package com.quartz.platform.domain.model

enum class ReportDraftOriginWorkflowType {
    XFEEDER,
    RET,
    PERFORMANCE
}

data class ReportDraft(
    val id: String,
    val siteId: String,
    val originSessionId: String? = null,
    val originSectorId: String? = null,
    val originWorkflowType: ReportDraftOriginWorkflowType? = null,
    val title: String,
    val observation: String,
    val revision: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)
