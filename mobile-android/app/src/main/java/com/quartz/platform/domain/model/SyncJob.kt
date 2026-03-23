package com.quartz.platform.domain.model

data class SyncJob(
    val id: String,
    val aggregateType: SyncAggregateType,
    val aggregateId: String,
    val operationType: SyncOperationType,
    val payloadReference: String?,
    val status: SyncJobStatus,
    val retryCount: Int,
    val nextAttemptAtEpochMillis: Long?,
    val lastAttemptAtEpochMillis: Long?,
    val lastError: String?
)
