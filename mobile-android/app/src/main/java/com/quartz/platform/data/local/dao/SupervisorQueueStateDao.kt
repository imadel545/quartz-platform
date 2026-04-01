package com.quartz.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quartz.platform.data.local.entity.SupervisorQueueStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupervisorQueueStateDao {
    @Query("SELECT * FROM supervisor_queue_states")
    fun observeAll(): Flow<List<SupervisorQueueStateEntity>>

    @Query("SELECT * FROM supervisor_queue_states WHERE draftId = :draftId LIMIT 1")
    suspend fun getByDraftId(draftId: String): SupervisorQueueStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SupervisorQueueStateEntity)

    @Query("DELETE FROM supervisor_queue_states WHERE draftId NOT IN (:activeDraftIds)")
    suspend fun pruneMissingDrafts(activeDraftIds: List<String>)
}
