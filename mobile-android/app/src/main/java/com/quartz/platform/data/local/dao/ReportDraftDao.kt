package com.quartz.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quartz.platform.data.local.entity.ReportDraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReportDraftEntity)

    @Query("SELECT * FROM report_drafts WHERE id = :draftId LIMIT 1")
    fun observeById(draftId: String): Flow<ReportDraftEntity?>

    @Query("SELECT * FROM report_drafts WHERE siteId = :siteId ORDER BY updatedAtEpochMillis DESC")
    fun listBySite(siteId: String): Flow<List<ReportDraftEntity>>

    @Query("SELECT * FROM report_drafts WHERE id = :draftId LIMIT 1")
    suspend fun getById(draftId: String): ReportDraftEntity?

    @Query(
        """
        SELECT * FROM report_drafts
        WHERE siteId = :siteId AND originSessionId = :originSessionId
        ORDER BY updatedAtEpochMillis DESC
        LIMIT 1
        """
    )
    suspend fun findLatestLinkedBySession(siteId: String, originSessionId: String): ReportDraftEntity?

    @Query("SELECT revision FROM report_drafts WHERE id = :draftId LIMIT 1")
    suspend fun getRevision(draftId: String): Int?

    @Query(
        """
        UPDATE report_drafts
        SET title = :title,
            observation = :observation,
            revision = :revision,
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE id = :draftId
        """
    )
    suspend fun updateDraft(
        draftId: String,
        title: String,
        observation: String,
        revision: Int,
        updatedAtEpochMillis: Long
    )
}
