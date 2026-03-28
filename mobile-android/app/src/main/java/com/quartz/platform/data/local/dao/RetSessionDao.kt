package com.quartz.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quartz.platform.data.local.entity.RetSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RetSessionDao {
    @Query(
        """
        SELECT * FROM ret_sessions
        WHERE siteId = :siteId AND sectorId = :sectorId
        ORDER BY createdAtEpochMillis DESC
        """
    )
    fun observeHistoryBySector(siteId: String, sectorId: String): Flow<List<RetSessionEntity>>

    @Query(
        """
        SELECT * FROM ret_sessions
        WHERE siteId = :siteId AND sectorId = :sectorId
        ORDER BY createdAtEpochMillis DESC
        LIMIT 1
        """
    )
    fun observeLatestBySector(siteId: String, sectorId: String): Flow<RetSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: RetSessionEntity)

    @Query("SELECT * FROM ret_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getById(sessionId: String): RetSessionEntity?

    @Query(
        """
        UPDATE ret_sessions
        SET status = :status,
            resultOutcome = :resultOutcome,
            notes = :notes,
            resultSummary = :resultSummary,
            updatedAtEpochMillis = :updatedAtEpochMillis,
            completedAtEpochMillis = :completedAtEpochMillis
        WHERE id = :sessionId
        """
    )
    suspend fun updateSummary(
        sessionId: String,
        status: String,
        resultOutcome: String,
        notes: String,
        resultSummary: String,
        updatedAtEpochMillis: Long,
        completedAtEpochMillis: Long?
    )
}
