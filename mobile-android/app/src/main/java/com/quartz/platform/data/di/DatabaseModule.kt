package com.quartz.platform.data.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.quartz.platform.data.local.DatabaseMigrations
import com.quartz.platform.data.local.QuartzDatabase
import com.quartz.platform.data.local.dao.ReportDraftDao
import com.quartz.platform.data.local.dao.PerformanceQosFamilyResultDao
import com.quartz.platform.data.local.dao.PerformanceQosTimelineEventDao
import com.quartz.platform.data.local.dao.PerformanceSessionDao
import com.quartz.platform.data.local.dao.PerformanceStepDao
import com.quartz.platform.data.local.dao.QosScriptDao
import com.quartz.platform.data.local.dao.RetSessionDao
import com.quartz.platform.data.local.dao.RetStepDao
import com.quartz.platform.data.local.dao.SiteAntennaDao
import com.quartz.platform.data.local.dao.SiteCellDao
import com.quartz.platform.data.local.dao.SiteDao
import com.quartz.platform.data.local.dao.SiteSectorDao
import com.quartz.platform.data.local.dao.SyncJobDao
import com.quartz.platform.data.local.dao.XfeederSessionDao
import com.quartz.platform.data.local.dao.XfeederStepDao
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
        ).addMigrations(
            DatabaseMigrations.MIGRATION_1_2,
            DatabaseMigrations.MIGRATION_2_3,
            DatabaseMigrations.MIGRATION_3_4,
            DatabaseMigrations.MIGRATION_4_5,
            DatabaseMigrations.MIGRATION_5_6,
            DatabaseMigrations.MIGRATION_6_7,
            DatabaseMigrations.MIGRATION_7_8,
            DatabaseMigrations.MIGRATION_8_9,
            DatabaseMigrations.MIGRATION_9_10,
            DatabaseMigrations.MIGRATION_10_11,
            DatabaseMigrations.MIGRATION_11_12,
            DatabaseMigrations.MIGRATION_12_13,
            DatabaseMigrations.MIGRATION_13_14,
            DatabaseMigrations.MIGRATION_14_15,
            DatabaseMigrations.MIGRATION_15_16,
            DatabaseMigrations.MIGRATION_16_17,
            DatabaseMigrations.MIGRATION_17_18,
            DatabaseMigrations.MIGRATION_18_19,
            DatabaseMigrations.MIGRATION_19_20
        )
            .build()
    }

    @Provides
    fun provideSiteDao(database: QuartzDatabase): SiteDao = database.siteDao()

    @Provides
    fun provideSiteSectorDao(database: QuartzDatabase): SiteSectorDao = database.siteSectorDao()

    @Provides
    fun provideSiteAntennaDao(database: QuartzDatabase): SiteAntennaDao = database.siteAntennaDao()

    @Provides
    fun provideSiteCellDao(database: QuartzDatabase): SiteCellDao = database.siteCellDao()

    @Provides
    fun provideXfeederSessionDao(database: QuartzDatabase): XfeederSessionDao = database.xfeederSessionDao()

    @Provides
    fun provideXfeederStepDao(database: QuartzDatabase): XfeederStepDao = database.xfeederStepDao()

    @Provides
    fun providePerformanceSessionDao(database: QuartzDatabase): PerformanceSessionDao =
        database.performanceSessionDao()

    @Provides
    fun providePerformanceStepDao(database: QuartzDatabase): PerformanceStepDao =
        database.performanceStepDao()

    @Provides
    fun providePerformanceQosFamilyResultDao(database: QuartzDatabase): PerformanceQosFamilyResultDao =
        database.performanceQosFamilyResultDao()

    @Provides
    fun providePerformanceQosTimelineEventDao(database: QuartzDatabase): PerformanceQosTimelineEventDao =
        database.performanceQosTimelineEventDao()

    @Provides
    fun provideQosScriptDao(database: QuartzDatabase): QosScriptDao = database.qosScriptDao()

    @Provides
    fun provideRetSessionDao(database: QuartzDatabase): RetSessionDao = database.retSessionDao()

    @Provides
    fun provideRetStepDao(database: QuartzDatabase): RetStepDao = database.retStepDao()

    @Provides
    fun provideSyncJobDao(database: QuartzDatabase): SyncJobDao = database.syncJobDao()

    @Provides
    fun provideReportDraftDao(database: QuartzDatabase): ReportDraftDao = database.reportDraftDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)
}
