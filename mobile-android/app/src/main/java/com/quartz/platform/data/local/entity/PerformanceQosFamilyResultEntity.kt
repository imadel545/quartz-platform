package com.quartz.platform.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "performance_qos_family_results",
    primaryKeys = ["sessionId", "family"],
    foreignKeys = [
        ForeignKey(
            entity = PerformanceSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["sessionId", "updatedAtEpochMillis"])
    ]
)
data class PerformanceQosFamilyResultEntity(
    val sessionId: String,
    val family: String,
    val status: String,
    val failureReason: String?,
    val observedLatencyMs: Int?,
    val observedDownloadMbps: Double?,
    val observedUploadMbps: Double?,
    val updatedAtEpochMillis: Long
)
