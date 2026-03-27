package com.quartz.platform.data.remote

import com.quartz.platform.core.logging.AppLogger
import com.quartz.platform.data.remote.simulation.SyncPushSimulationOverride
import com.quartz.platform.domain.model.SyncJob
import kotlin.jvm.JvmSuppressWildcards
import kotlinx.coroutines.delay
import javax.inject.Inject

class StubSyncGateway @Inject constructor(
    private val appLogger: AppLogger,
    private val pushSimulationOverrides: Set<@JvmSuppressWildcards SyncPushSimulationOverride>
) : SyncGateway {
    override suspend fun push(job: SyncJob): SyncPushResult {
        delay(150)
        pushSimulationOverrides.forEach { override ->
            override.overridePushResult(job)?.let { simulated ->
                appLogger.warn(TAG, "Stub sync simulated result=$simulated for ${job.aggregateType}:${job.aggregateId}")
                return simulated
            }
        }
        appLogger.info(TAG, "Stub sync accepted ${job.aggregateType}:${job.aggregateId}")
        return SyncPushResult.Success
    }

    private companion object {
        const val TAG = "StubSyncGateway"
    }
}
