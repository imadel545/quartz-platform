package com.quartz.platform.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "performance_sessions",
    foreignKeys = [
        ForeignKey(
            entity = SiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["siteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["siteId"]),
        Index(value = ["siteId", "workflowType", "createdAtEpochMillis"])
    ]
)
data class PerformanceSessionEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val siteCode: String,
    val workflowType: String,
    val operatorName: String?,
    val technology: String?,
    val status: String,
    val prerequisiteNetworkReady: Boolean,
    val prerequisiteBatterySufficient: Boolean,
    val prerequisiteLocationReady: Boolean,
    val observedNetworkStatus: String?,
    val observedBatteryLevelPercent: Int?,
    val observedLocationAvailable: Boolean?,
    val observedSignalsCapturedAtEpochMillis: Long?,
    val throughputDownloadMbps: Double?,
    val throughputUploadMbps: Double?,
    val throughputLatencyMs: Int?,
    val throughputMinDownloadMbps: Double?,
    val throughputMinUploadMbps: Double?,
    val throughputMaxLatencyMs: Int?,
    val qosScriptId: String?,
    val qosScriptName: String?,
    val qosConfiguredRepeatCount: Int?,
    val qosConfiguredTechnologiesCsv: String,
    val qosScriptSnapshotUpdatedAtEpochMillis: Long?,
    val qosTestFamiliesCsv: String,
    val qosTargetTechnology: String?,
    val qosTargetPhoneNumber: String?,
    val qosIterationCount: Int,
    val qosSuccessCount: Int,
    val qosFailureCount: Int,
    val notes: String,
    val resultSummary: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val completedAtEpochMillis: Long?
)
