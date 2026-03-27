package com.quartz.platform.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "report_drafts",
    indices = [
        Index(value = ["siteId"]),
        Index(
            value = ["siteId", "originSessionId"],
            unique = true
        )
    ]
)
data class ReportDraftEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val originSessionId: String?,
    val originSectorId: String?,
    val title: String,
    val observation: String,
    val revision: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)
