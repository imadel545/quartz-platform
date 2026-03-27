package com.quartz.platform.data.remote.simulation

import com.quartz.platform.data.remote.SyncPushResult
import com.quartz.platform.domain.model.SyncJob
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class DebugSyncSimulationController @Inject constructor() : SyncPushSimulationOverride, SyncSimulationControl {

    private val mode = MutableStateFlow(SyncSimulationMode.NORMAL_SUCCESS)
    private val failOnceConsumedKeys = ConcurrentHashMap.newKeySet<String>()

    override fun observeMode(): Flow<SyncSimulationMode> = mode.asStateFlow()

    override suspend fun setMode(mode: SyncSimulationMode) {
        this.mode.value = mode
        when (mode) {
            SyncSimulationMode.NORMAL_SUCCESS -> failOnceConsumedKeys.clear()
            SyncSimulationMode.FAIL_NEXT_RETRYABLE -> Unit
            SyncSimulationMode.FAIL_ONCE_THEN_SUCCESS -> failOnceConsumedKeys.clear()
            SyncSimulationMode.FAIL_NEXT_TERMINAL -> Unit
        }
    }

    override suspend fun overridePushResult(job: SyncJob): SyncPushResult? {
        return when (mode.value) {
            SyncSimulationMode.NORMAL_SUCCESS -> null

            SyncSimulationMode.FAIL_NEXT_RETRYABLE -> {
                mode.value = SyncSimulationMode.NORMAL_SUCCESS
                SyncPushResult.RetryableFailure("DEBUG_SIMULATION_RETRYABLE_FAILURE")
            }

            SyncSimulationMode.FAIL_ONCE_THEN_SUCCESS -> {
                val key = "${job.aggregateType}:${job.aggregateId}:${job.payloadReference}"
                if (failOnceConsumedKeys.add(key)) {
                    SyncPushResult.RetryableFailure("DEBUG_SIMULATION_FAIL_ONCE_THEN_SUCCESS")
                } else {
                    null
                }
            }

            SyncSimulationMode.FAIL_NEXT_TERMINAL -> {
                mode.value = SyncSimulationMode.NORMAL_SUCCESS
                SyncPushResult.TerminalFailure("DEBUG_SIMULATION_TERMINAL_FAILURE")
            }
        }
    }
}
