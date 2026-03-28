package com.quartz.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quartz.platform.data.local.entity.RetStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RetStepDao {
    @Query(
        """
        SELECT * FROM ret_steps
        WHERE sessionId = :sessionId
        ORDER BY displayOrder ASC
        """
    )
    fun observeBySession(sessionId: String): Flow<List<RetStepEntity>>

    @Query(
        """
        SELECT * FROM ret_steps
        WHERE sessionId IN (:sessionIds)
        ORDER BY sessionId ASC, displayOrder ASC
        """
    )
    fun observeBySessionIds(sessionIds: List<String>): Flow<List<RetStepEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(steps: List<RetStepEntity>)

    @Query(
        """
        SELECT * FROM ret_steps
        WHERE sessionId = :sessionId
        ORDER BY displayOrder ASC
        """
    )
    suspend fun listBySession(sessionId: String): List<RetStepEntity>

    @Query(
        """
        UPDATE ret_steps
        SET status = :status
        WHERE sessionId = :sessionId AND code = :code
        """
    )
    suspend fun updateStatus(sessionId: String, code: String, status: String)
}
