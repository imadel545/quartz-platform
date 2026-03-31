package com.quartz.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quartz.platform.data.local.entity.PerformanceSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PerformanceSessionDao {
    @Query(
        """
        SELECT * FROM performance_sessions
        WHERE siteId = :siteId
        ORDER BY createdAtEpochMillis DESC
        """
    )
    fun observeHistoryBySite(siteId: String): Flow<List<PerformanceSessionEntity>>

    @Query(
        """
        SELECT * FROM performance_sessions
        WHERE siteId = :siteId AND workflowType = :workflowType
        ORDER BY createdAtEpochMillis DESC
        LIMIT 1
        """
    )
    fun observeLatestBySiteAndType(siteId: String, workflowType: String): Flow<PerformanceSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: PerformanceSessionEntity)

    @Query("SELECT * FROM performance_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getById(sessionId: String): PerformanceSessionEntity?

    @Query(
        """
        UPDATE performance_sessions
        SET status = :status,
            prerequisiteNetworkReady = :prerequisiteNetworkReady,
            prerequisiteBatterySufficient = :prerequisiteBatterySufficient,
            prerequisiteLocationReady = :prerequisiteLocationReady,
            observedNetworkStatus = :observedNetworkStatus,
            observedBatteryLevelPercent = :observedBatteryLevelPercent,
            observedLocationAvailable = :observedLocationAvailable,
            observedSignalsCapturedAtEpochMillis = :observedSignalsCapturedAtEpochMillis,
            throughputDownloadMbps = :throughputDownloadMbps,
            throughputUploadMbps = :throughputUploadMbps,
            throughputLatencyMs = :throughputLatencyMs,
            throughputMinDownloadMbps = :throughputMinDownloadMbps,
            throughputMinUploadMbps = :throughputMinUploadMbps,
            throughputMaxLatencyMs = :throughputMaxLatencyMs,
            qosScriptId = :qosScriptId,
            qosScriptName = :qosScriptName,
            qosConfiguredRepeatCount = :qosConfiguredRepeatCount,
            qosConfiguredTechnologiesCsv = :qosConfiguredTechnologiesCsv,
            qosScriptSnapshotUpdatedAtEpochMillis = :qosScriptSnapshotUpdatedAtEpochMillis,
            qosTestFamiliesCsv = :qosTestFamiliesCsv,
            qosTargetTechnology = :qosTargetTechnology,
            qosTargetPhoneNumber = :qosTargetPhoneNumber,
            qosIterationCount = :qosIterationCount,
            qosSuccessCount = :qosSuccessCount,
            qosFailureCount = :qosFailureCount,
            notes = :notes,
            resultSummary = :resultSummary,
            updatedAtEpochMillis = :updatedAtEpochMillis,
            completedAtEpochMillis = :completedAtEpochMillis
        WHERE id = :sessionId
        """
    )
    suspend fun updateExecution(
        sessionId: String,
        status: String,
        prerequisiteNetworkReady: Boolean,
        prerequisiteBatterySufficient: Boolean,
        prerequisiteLocationReady: Boolean,
        observedNetworkStatus: String?,
        observedBatteryLevelPercent: Int?,
        observedLocationAvailable: Boolean?,
        observedSignalsCapturedAtEpochMillis: Long?,
        throughputDownloadMbps: Double?,
        throughputUploadMbps: Double?,
        throughputLatencyMs: Int?,
        throughputMinDownloadMbps: Double?,
        throughputMinUploadMbps: Double?,
        throughputMaxLatencyMs: Int?,
        qosScriptId: String?,
        qosScriptName: String?,
        qosConfiguredRepeatCount: Int?,
        qosConfiguredTechnologiesCsv: String,
        qosScriptSnapshotUpdatedAtEpochMillis: Long?,
        qosTestFamiliesCsv: String,
        qosTargetTechnology: String?,
        qosTargetPhoneNumber: String?,
        qosIterationCount: Int,
        qosSuccessCount: Int,
        qosFailureCount: Int,
        notes: String,
        resultSummary: String,
        updatedAtEpochMillis: Long,
        completedAtEpochMillis: Long?
    )
}
