package com.quartz.platform.data.remote

import com.quartz.platform.domain.model.SyncJob
import kotlinx.coroutines.delay
import javax.inject.Inject

class FakeSyncGateway @Inject constructor() : SyncGateway {
    override suspend fun push(job: SyncJob): SyncPushResult {
        delay(250)
        return when {
            job.aggregateId.contains("terminal_error", ignoreCase = true) -> {
                SyncPushResult.TerminalFailure("Remote rejected aggregate")
            }

            job.aggregateId.contains("retry", ignoreCase = true) -> {
                SyncPushResult.RetryableFailure("Transient remote issue")
            }

            else -> SyncPushResult.Success
        }
    }
}
