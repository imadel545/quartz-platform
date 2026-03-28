package com.quartz.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quartz.platform.data.local.entity.XfeederSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface XfeederSessionDao {
    @Query(
        """
        SELECT * FROM xfeeder_sessions
        WHERE siteId = :siteId
        ORDER BY updatedAtEpochMillis DESC
        """
    )
    fun observeBySite(siteId: String): Flow<List<XfeederSessionEntity>>

    @Query(
        """
        SELECT * FROM xfeeder_sessions
        WHERE siteId = :siteId AND sectorId = :sectorId
        ORDER BY createdAtEpochMillis DESC
        """
    )
    fun observeHistoryBySector(siteId: String, sectorId: String): Flow<List<XfeederSessionEntity>>

    @Query(
        """
        SELECT * FROM xfeeder_sessions
        WHERE siteId = :siteId AND sectorId = :sectorId
        ORDER BY createdAtEpochMillis DESC
        LIMIT 1
        """
    )
    fun observeLatestBySector(siteId: String, sectorId: String): Flow<XfeederSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: XfeederSessionEntity)

    @Query("SELECT * FROM xfeeder_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getById(sessionId: String): XfeederSessionEntity?

    @Query(
        """
        UPDATE xfeeder_sessions
        SET status = :status,
            sectorOutcome = :sectorOutcome,
            closureRelatedSectorCode = :closureRelatedSectorCode,
            closureUnreliableReason = :closureUnreliableReason,
            closureObservedSectorCount = :closureObservedSectorCount,
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
        sectorOutcome: String,
        closureRelatedSectorCode: String,
        closureUnreliableReason: String?,
        closureObservedSectorCount: Int?,
        notes: String,
        resultSummary: String,
        updatedAtEpochMillis: Long,
        completedAtEpochMillis: Long?
    )

    @Query(
        """
        UPDATE xfeeder_sessions
        SET measurementZoneRadiusMeters = :measurementZoneRadiusMeters,
            measurementZoneExtensionReason = :measurementZoneExtensionReason,
            proximityModeEnabled = :proximityModeEnabled,
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE id = :sessionId
        """
    )
    suspend fun updateGeospatialContext(
        sessionId: String,
        measurementZoneRadiusMeters: Int,
        measurementZoneExtensionReason: String,
        proximityModeEnabled: Boolean,
        updatedAtEpochMillis: Long
    )
}
