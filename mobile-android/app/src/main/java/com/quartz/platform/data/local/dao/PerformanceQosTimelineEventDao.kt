package com.quartz.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quartz.platform.data.local.entity.PerformanceQosTimelineEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PerformanceQosTimelineEventDao {
    @Query(
        """
        SELECT * FROM performance_qos_timeline_events
        WHERE sessionId = :sessionId
        ORDER BY occurredAtEpochMillis DESC, family ASC, repetitionIndex ASC, eventType ASC
        """
    )
    fun observeBySession(sessionId: String): Flow<List<PerformanceQosTimelineEventEntity>>

    @Query(
        """
        SELECT * FROM performance_qos_timeline_events
        WHERE sessionId IN (:sessionIds)
        ORDER BY sessionId ASC, occurredAtEpochMillis DESC, family ASC, repetitionIndex ASC, eventType ASC
        """
    )
    fun observeBySessionIds(sessionIds: List<String>): Flow<List<PerformanceQosTimelineEventEntity>>

    @Query(
        """
        SELECT * FROM performance_qos_timeline_events
        WHERE sessionId = :sessionId
        ORDER BY occurredAtEpochMillis DESC, family ASC, repetitionIndex ASC, eventType ASC
        """
    )
    suspend fun listBySession(sessionId: String): List<PerformanceQosTimelineEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<PerformanceQosTimelineEventEntity>)

    @Query("DELETE FROM performance_qos_timeline_events WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}
