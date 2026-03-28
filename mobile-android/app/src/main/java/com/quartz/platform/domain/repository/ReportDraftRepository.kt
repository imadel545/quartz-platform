package com.quartz.platform.domain.repository

import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import kotlinx.coroutines.flow.Flow

interface ReportDraftRepository {
    suspend fun createDraft(
        siteId: String,
        originSessionId: String? = null,
        originSectorId: String? = null,
        originWorkflowType: ReportDraftOriginWorkflowType? = null
    ): ReportDraft
    suspend fun updateDraft(draftId: String, title: String, observation: String): ReportDraft?
    suspend fun findLatestLinkedDraft(
        siteId: String,
        originSessionId: String,
        originWorkflowType: ReportDraftOriginWorkflowType? = null
    ): ReportDraft?
    fun observeDraft(draftId: String): Flow<ReportDraft?>
    fun listDraftsBySite(siteId: String): Flow<List<ReportDraft>>
}
