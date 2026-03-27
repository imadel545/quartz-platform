package com.quartz.platform.data.remote.simulation

import com.quartz.platform.data.remote.SyncPushResult
import com.quartz.platform.domain.model.SyncJob

interface SyncPushSimulationOverride {
    suspend fun overridePushResult(job: SyncJob): SyncPushResult?
}
