package com.quartz.platform.data.local.mapper

import com.quartz.platform.data.local.entity.SyncJobEntity
import com.quartz.platform.domain.model.SyncJob

fun SyncJobEntity.toDomain(): SyncJob {
    return SyncJob(
        id = id,
        aggregateType = aggregateType,
        aggregateId = aggregateId,
        operationType = operationType,
        payloadReference = payloadReference,
        status = status,
        retryCount = retryCount,
        nextAttemptAtEpochMillis = nextAttemptAtEpochMillis,
        lastAttemptAtEpochMillis = lastAttemptAtEpochMillis,
        lastError = lastError
    )
}
