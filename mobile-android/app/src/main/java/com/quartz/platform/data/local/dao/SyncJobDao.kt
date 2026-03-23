package com.quartz.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quartz.platform.data.local.entity.SyncJobEntity
import com.quartz.platform.domain.model.SyncAggregateType
import com.quartz.platform.domain.model.SyncOperationType
import com.quartz.platform.domain.model.SyncJobStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncJobDao {
    @Query("SELECT COUNT(*) FROM sync_jobs WHERE status IN ('PENDING', 'IN_FLIGHT', 'FAILED_RETRYABLE')")
    fun observePendingCount(): Flow<Int>

    @Query(
        """
        SELECT * FROM sync_jobs
        WHERE (status = 'PENDING') OR (status = 'FAILED_RETRYABLE' AND (nextAttemptAtEpochMillis IS NULL OR nextAttemptAtEpochMillis <= :currentTimeMillis))
        ORDER BY createdAtEpochMillis ASC
        LIMIT :limit
        """
    )
    suspend fun getPendingJobs(currentTimeMillis: Long, limit: Int): List<SyncJobEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: SyncJobEntity)

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM sync_jobs
            WHERE aggregateType = :aggregateType
              AND aggregateId = :aggregateId
              AND operationType = :operationType
              AND status IN ('PENDING', 'IN_FLIGHT', 'FAILED_RETRYABLE')
        )
        """
    )
    suspend fun hasActionableJob(
        aggregateType: SyncAggregateType,
        aggregateId: String,
        operationType: SyncOperationType
    ): Boolean

    @Query(
        """
        UPDATE sync_jobs
        SET status = :status,
            retryCount = :retryCount,
            nextAttemptAtEpochMillis = :nextAttemptAtEpochMillis,
            lastAttemptAtEpochMillis = :lastAttemptAtEpochMillis,
            lastError = :lastError
        WHERE id = :jobId
        """
    )
    suspend fun updateStatus(
        jobId: String,
        status: SyncJobStatus,
        retryCount: Int,
        nextAttemptAtEpochMillis: Long?,
        lastAttemptAtEpochMillis: Long?,
        lastError: String?
    )
}
