package com.quartz.platform.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.quartz.platform.data.local.dao.ReportDraftDao
import com.quartz.platform.data.local.dao.SiteAntennaDao
import com.quartz.platform.data.local.dao.SiteCellDao
import com.quartz.platform.data.local.dao.SiteDao
import com.quartz.platform.data.local.dao.SiteSectorDao
import com.quartz.platform.data.local.dao.SyncJobDao
import com.quartz.platform.data.local.dao.XfeederSessionDao
import com.quartz.platform.data.local.dao.XfeederStepDao
import com.quartz.platform.data.local.entity.ReportDraftEntity
import com.quartz.platform.data.local.entity.SiteAntennaEntity
import com.quartz.platform.data.local.entity.SiteCellEntity
import com.quartz.platform.data.local.entity.SiteEntity
import com.quartz.platform.data.local.entity.SiteSectorEntity
import com.quartz.platform.data.local.entity.SyncJobEntity
import com.quartz.platform.data.local.entity.XfeederSessionEntity
import com.quartz.platform.data.local.entity.XfeederStepEntity

@Database(
    entities = [
        SiteEntity::class,
        SiteSectorEntity::class,
        SiteAntennaEntity::class,
        SiteCellEntity::class,
        XfeederSessionEntity::class,
        XfeederStepEntity::class,
        SyncJobEntity::class,
        ReportDraftEntity::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(QuartzTypeConverters::class)
abstract class QuartzDatabase : RoomDatabase() {
    abstract fun siteDao(): SiteDao
    abstract fun siteSectorDao(): SiteSectorDao
    abstract fun siteAntennaDao(): SiteAntennaDao
    abstract fun siteCellDao(): SiteCellDao
    abstract fun xfeederSessionDao(): XfeederSessionDao
    abstract fun xfeederStepDao(): XfeederStepDao
    abstract fun syncJobDao(): SyncJobDao
    abstract fun reportDraftDao(): ReportDraftDao
}
