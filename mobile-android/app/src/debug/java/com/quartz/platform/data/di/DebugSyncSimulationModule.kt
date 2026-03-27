package com.quartz.platform.data.di

import com.quartz.platform.data.remote.simulation.DebugSyncSimulationController
import com.quartz.platform.data.remote.simulation.SyncPushSimulationOverride
import com.quartz.platform.data.remote.simulation.SyncSimulationControl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class DebugSyncSimulationModule {

    @Binds
    @IntoSet
    abstract fun bindSyncPushOverride(
        controller: DebugSyncSimulationController
    ): SyncPushSimulationOverride

    @Binds
    @IntoSet
    abstract fun bindSyncSimulationControl(
        controller: DebugSyncSimulationController
    ): SyncSimulationControl
}
