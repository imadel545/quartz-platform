package com.quartz.platform.data.remote.simulation

import kotlinx.coroutines.flow.Flow

interface SyncSimulationControl {
    fun observeMode(): Flow<SyncSimulationMode>
    suspend fun setMode(mode: SyncSimulationMode)
}
