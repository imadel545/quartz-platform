package com.quartz.platform.domain.model

data class ReportDraft(
    val id: String,
    val siteId: String,
    val originSessionId: String? = null,
    val originSectorId: String? = null,
    val title: String,
    val observation: String,
    val revision: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)
