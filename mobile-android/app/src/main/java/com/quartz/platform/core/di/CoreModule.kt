package com.quartz.platform.core.di

import android.content.Context
import com.quartz.platform.core.dispatchers.DefaultDispatcherProvider
import com.quartz.platform.core.dispatchers.DispatcherProvider
import com.quartz.platform.core.logging.AndroidAppLogger
import com.quartz.platform.core.logging.AppLogger
import com.quartz.platform.core.text.AndroidUiStrings
import com.quartz.platform.core.text.UiStrings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Provides
    @Singleton
    fun provideUiStrings(@ApplicationContext context: Context): UiStrings = AndroidUiStrings(context)
}
