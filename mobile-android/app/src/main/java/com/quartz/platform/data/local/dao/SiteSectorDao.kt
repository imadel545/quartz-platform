package com.quartz.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quartz.platform.data.local.entity.SiteSectorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteSectorDao {
    @Query("SELECT * FROM site_sectors WHERE siteId = :siteId ORDER BY displayOrder ASC, code ASC")
    fun observeBySiteId(siteId: String): Flow<List<SiteSectorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sectors: List<SiteSectorEntity>)

    @Query("DELETE FROM site_sectors")
    suspend fun deleteAll()
}
