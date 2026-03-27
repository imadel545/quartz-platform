package com.quartz.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quartz.platform.data.local.entity.SiteAntennaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteAntennaDao {
    @Query("SELECT * FROM site_antennas WHERE siteId = :siteId ORDER BY displayOrder ASC, reference ASC")
    fun observeBySiteId(siteId: String): Flow<List<SiteAntennaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(antennas: List<SiteAntennaEntity>)

    @Query("DELETE FROM site_antennas")
    suspend fun deleteAll()
}
