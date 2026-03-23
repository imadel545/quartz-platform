package com.quartz.platform.data.di

import com.quartz.platform.data.repository.OfflineFirstSiteRepository
import com.quartz.platform.data.repository.OfflineFirstSyncRepository
import com.quartz.platform.domain.repository.SiteRepository
import com.quartz.platform.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSiteRepository(impl: OfflineFirstSiteRepository): SiteRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: OfflineFirstSyncRepository): SyncRepository
}
