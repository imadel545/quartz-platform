package com.quartz.platform.core.di

import com.quartz.platform.core.dispatchers.DefaultDispatcherProvider
import com.quartz.platform.core.dispatchers.DispatcherProvider
import com.quartz.platform.core.logging.AndroidAppLogger
import com.quartz.platform.core.logging.AppLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    @Singleton
    fun provideAppLogger(): AppLogger = AndroidAppLogger()
}
