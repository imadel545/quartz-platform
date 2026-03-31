package com.quartz.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quartz.platform.data.local.entity.QosScriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QosScriptDao {
    @Query(
        """
        SELECT * FROM qos_scripts
        WHERE isArchived = 0
        ORDER BY updatedAtEpochMillis DESC
        """
    )
    fun observeActiveScripts(): Flow<List<QosScriptEntity>>

    @Query("SELECT * FROM qos_scripts WHERE id = :scriptId LIMIT 1")
    suspend fun getById(scriptId: String): QosScriptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: QosScriptEntity)

    @Query(
        """
        UPDATE qos_scripts
        SET isArchived = 1,
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE id = :scriptId
        """
    )
    suspend fun archive(scriptId: String, updatedAtEpochMillis: Long)
}

