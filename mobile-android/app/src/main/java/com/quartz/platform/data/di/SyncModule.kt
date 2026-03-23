package com.quartz.platform.data.di

import com.quartz.platform.data.remote.FakeSyncGateway
import com.quartz.platform.data.remote.SyncGateway
import com.quartz.platform.data.sync.SyncOrchestrator
import com.quartz.platform.data.sync.WorkManagerSyncOrchestrator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindSyncGateway(impl: FakeSyncGateway): SyncGateway

    @Binds
    @Singleton
    abstract fun bindSyncOrchestrator(impl: WorkManagerSyncOrchestrator): SyncOrchestrator
}
