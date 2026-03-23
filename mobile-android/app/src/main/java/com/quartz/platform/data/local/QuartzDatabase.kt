package com.quartz.platform.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.quartz.platform.data.local.dao.SiteDao
import com.quartz.platform.data.local.dao.SyncJobDao
import com.quartz.platform.data.local.entity.SiteEntity
import com.quartz.platform.data.local.entity.SyncJobEntity

@Database(
    entities = [SiteEntity::class, SyncJobEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(QuartzTypeConverters::class)
abstract class QuartzDatabase : RoomDatabase() {
    abstract fun siteDao(): SiteDao
    abstract fun syncJobDao(): SyncJobDao
}
