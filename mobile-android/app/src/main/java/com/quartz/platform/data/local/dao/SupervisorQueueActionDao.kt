package com.quartz.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quartz.platform.data.local.entity.SupervisorQueueActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupervisorQueueActionDao {
    @Query("SELECT * FROM supervisor_queue_actions ORDER BY actedAtEpochMillis DESC")
    fun observeAll(): Flow<List<SupervisorQueueActionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SupervisorQueueActionEntity)

    @Query("DELETE FROM supervisor_queue_actions WHERE actedAtEpochMillis < :thresholdEpochMillis")
    suspend fun pruneOlderThan(thresholdEpochMillis: Long)
}
