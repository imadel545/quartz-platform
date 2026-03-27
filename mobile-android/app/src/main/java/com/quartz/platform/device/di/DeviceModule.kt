package com.quartz.platform.device.di

import com.quartz.platform.device.network.AndroidNetworkMonitor
import com.quartz.platform.device.network.NetworkMonitor
import com.quartz.platform.device.location.AndroidLocationRepository
import com.quartz.platform.domain.repository.LocationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DeviceModule {

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: AndroidNetworkMonitor): NetworkMonitor

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: AndroidLocationRepository): LocationRepository
}
