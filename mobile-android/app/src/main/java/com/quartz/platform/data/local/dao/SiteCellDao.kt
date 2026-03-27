package com.quartz.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quartz.platform.data.local.entity.SiteCellEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteCellDao {
    @Query("SELECT * FROM site_cells WHERE siteId = :siteId ORDER BY displayOrder ASC, label ASC")
    fun observeBySiteId(siteId: String): Flow<List<SiteCellEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(cells: List<SiteCellEntity>)

    @Query("DELETE FROM site_cells")
    suspend fun deleteAll()
}
