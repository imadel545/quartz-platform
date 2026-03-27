package com.quartz.platform.data.repository

import com.quartz.platform.data.local.dao.ReportDraftDao
import com.quartz.platform.data.local.entity.ReportDraftEntity
import com.quartz.platform.data.local.mapper.toDomain
import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.repository.ReportDraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class OfflineFirstReportDraftRepository @Inject constructor(
    private val reportDraftDao: ReportDraftDao
) : ReportDraftRepository {

    override suspend fun createDraft(
        siteId: String,
        originSessionId: String?,
        originSectorId: String?
    ): ReportDraft {
        val now = System.currentTimeMillis()
        val draft = ReportDraftEntity(
            id = UUID.randomUUID().toString(),
            siteId = siteId,
            originSessionId = originSessionId,
            originSectorId = originSectorId,
            title = "Brouillon rapport",
            observation = "",
            revision = 1,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now
        )
        reportDraftDao.insert(draft)
        return draft.toDomain()
    }

    override suspend fun updateDraft(draftId: String, title: String, observation: String): ReportDraft? {
        val existing = reportDraftDao.getById(draftId) ?: return null
        val nextRevision = existing.revision + 1
        val now = System.currentTimeMillis()

        reportDraftDao.updateDraft(
            draftId = draftId,
            title = title,
            observation = observation,
            revision = nextRevision,
            updatedAtEpochMillis = now
        )

        return reportDraftDao.getById(draftId)?.toDomain()
    }

    override suspend fun findLatestLinkedDraft(siteId: String, originSessionId: String): ReportDraft? {
        return reportDraftDao.findLatestLinkedBySession(
            siteId = siteId,
            originSessionId = originSessionId
        )?.toDomain()
    }

    override fun observeDraft(draftId: String): Flow<ReportDraft?> {
        return reportDraftDao.observeById(draftId).map { entity -> entity?.toDomain() }
    }

    override fun listDraftsBySite(siteId: String): Flow<List<ReportDraft>> {
        return reportDraftDao.listBySite(siteId).map { entities -> entities.map { it.toDomain() } }
    }
}
