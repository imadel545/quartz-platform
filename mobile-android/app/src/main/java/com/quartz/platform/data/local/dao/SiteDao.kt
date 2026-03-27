package com.quartz.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quartz.platform.data.local.entity.SiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {
    @Query("SELECT * FROM sites ORDER BY name ASC")
    fun observeAll(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE id = :siteId LIMIT 1")
    fun observeById(siteId: String): Flow<SiteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sites: List<SiteEntity>)

    @Query("DELETE FROM sites")
    suspend fun deleteAll()
}
