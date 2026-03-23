package com.quartz.platform.data.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.quartz.platform.data.local.QuartzDatabase
import com.quartz.platform.data.local.dao.SiteDao
import com.quartz.platform.data.local.dao.SyncJobDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideQuartzDatabase(@ApplicationContext context: Context): QuartzDatabase {
        return Room.databaseBuilder(
            context,
            QuartzDatabase::class.java,
            "quartz.db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSiteDao(database: QuartzDatabase): SiteDao = database.siteDao()

    @Provides
    fun provideSyncJobDao(database: QuartzDatabase): SyncJobDao = database.syncJobDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)
}
