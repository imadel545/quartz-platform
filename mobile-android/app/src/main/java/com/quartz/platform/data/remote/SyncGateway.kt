package com.quartz.platform.data.remote

import com.quartz.platform.domain.model.SyncJob

interface SyncGateway {
    suspend fun push(job: SyncJob): SyncPushResult
}

sealed class SyncPushResult {
    data object Success : SyncPushResult()
    data class RetryableFailure(val reason: String) : SyncPushResult()
    data class TerminalFailure(val reason: String) : SyncPushResult()
}
