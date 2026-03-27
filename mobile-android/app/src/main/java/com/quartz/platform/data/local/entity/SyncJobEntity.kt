package com.quartz.platform.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.quartz.platform.domain.model.SyncAggregateType
import com.quartz.platform.domain.model.SyncOperationType
import com.quartz.platform.domain.model.SyncJobStatus

@Entity(
    tableName = "sync_jobs",
    indices = [Index(value = ["status", "nextAttemptAtEpochMillis"])]
)
data class SyncJobEntity(
    @PrimaryKey val id: String,
    val aggregateType: SyncAggregateType,
    val aggregateId: String,
    val operationType: SyncOperationType,
    @ColumnInfo(name = "payload")
    val payloadReference: String?,
    val status: SyncJobStatus,
    val retryCount: Int,
    val nextAttemptAtEpochMillis: Long?,
    val lastAttemptAtEpochMillis: Long?,
    val lastError: String?,
    val createdAtEpochMillis: Long
)
