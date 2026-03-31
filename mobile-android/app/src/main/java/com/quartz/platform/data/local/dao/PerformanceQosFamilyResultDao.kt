package com.quartz.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quartz.platform.data.local.entity.PerformanceQosFamilyResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PerformanceQosFamilyResultDao {
    @Query(
        """
        SELECT * FROM performance_qos_family_results
        WHERE sessionId = :sessionId
        ORDER BY family ASC
        """
    )
    fun observeBySession(sessionId: String): Flow<List<PerformanceQosFamilyResultEntity>>

    @Query(
        """
        SELECT * FROM performance_qos_family_results
        WHERE sessionId IN (:sessionIds)
        ORDER BY sessionId ASC, family ASC
        """
    )
    fun observeBySessionIds(sessionIds: List<String>): Flow<List<PerformanceQosFamilyResultEntity>>

    @Query(
        """
        SELECT * FROM performance_qos_family_results
        WHERE sessionId = :sessionId
        ORDER BY family ASC
        """
    )
    suspend fun listBySession(sessionId: String): List<PerformanceQosFamilyResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(results: List<PerformanceQosFamilyResultEntity>)

    @Query("DELETE FROM performance_qos_family_results WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}
