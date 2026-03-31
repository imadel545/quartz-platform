package com.quartz.platform.device.di

import com.quartz.platform.device.battery.AndroidBatteryStatusRepository
import com.quartz.platform.device.network.AndroidNetworkMonitor
import com.quartz.platform.device.location.AndroidLocationRepository
import com.quartz.platform.domain.repository.BatteryStatusRepository
import com.quartz.platform.domain.repository.LocationRepository
import com.quartz.platform.domain.repository.NetworkStatusRepository
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
    abstract fun bindNetworkStatusRepository(impl: AndroidNetworkMonitor): NetworkStatusRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: AndroidLocationRepository): LocationRepository

    @Binds
    @Singleton
    abstract fun bindBatteryStatusRepository(impl: AndroidBatteryStatusRepository): BatteryStatusRepository
}
