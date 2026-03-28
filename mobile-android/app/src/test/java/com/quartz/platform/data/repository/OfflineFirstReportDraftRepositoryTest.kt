package com.quartz.platform.data.repository

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.data.local.dao.ReportDraftDao
import com.quartz.platform.data.local.entity.ReportDraftEntity
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OfflineFirstReportDraftRepositoryTest {

    @Test
    fun `create and update draft persists and increments revision`() = runTest {
        val dao = InMemoryReportDraftDao()
        val repository = OfflineFirstReportDraftRepository(dao)

        val created = repository.createDraft(
            siteId = "site-1",
            originSessionId = null,
            originSectorId = null
        )
        assertThat(created.revision).isEqualTo(1)

        val updated = repository.updateDraft(
            draftId = created.id,
            title = "Updated title",
            observation = "Updated observation"
        )

        assertThat(updated).isNotNull()
        assertThat(updated?.revision).isEqualTo(2)
        assertThat(repository.observeDraft(created.id).first()?.title).isEqualTo("Updated title")
        assertThat(repository.listDraftsBySite("site-1").first()).hasSize(1)
        assertThat(repository.observeDraft(created.id).first()?.originSessionId).isNull()
        assertThat(repository.observeDraft(created.id).first()?.originSectorId).isNull()
    }

    @Test
    fun `create draft stores origin link and update preserves it`() = runTest {
        val dao = InMemoryReportDraftDao()
        val repository = OfflineFirstReportDraftRepository(dao)

        val created = repository.createDraft(
            siteId = "site-1",
            originSessionId = "session-10",
            originSectorId = "sector-s0",
            originWorkflowType = ReportDraftOriginWorkflowType.XFEEDER
        )
        val updated = repository.updateDraft(
            draftId = created.id,
            title = "Title",
            observation = "Obs"
        )

        assertThat(created.originSessionId).isEqualTo("session-10")
        assertThat(created.originSectorId).isEqualTo("sector-s0")
        assertThat(created.originWorkflowType).isEqualTo(ReportDraftOriginWorkflowType.XFEEDER)
        assertThat(updated?.originSessionId).isEqualTo("session-10")
        assertThat(updated?.originSectorId).isEqualTo("sector-s0")
        assertThat(updated?.originWorkflowType).isEqualTo(ReportDraftOriginWorkflowType.XFEEDER)
    }

    @Test
    fun `find latest linked draft returns the most recent draft for session origin`() = runTest {
        val dao = InMemoryReportDraftDao()
        val repository = OfflineFirstReportDraftRepository(dao)

        dao.insert(
            ReportDraftEntity(
                id = "d1",
                siteId = "site-1",
                originSessionId = "session-1",
                originSectorId = "sector-s0",
                originWorkflowType = ReportDraftOriginWorkflowType.XFEEDER.name,
                title = "A",
                observation = "",
                revision = 1,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 10L
            )
        )
        dao.insert(
            ReportDraftEntity(
                id = "d2",
                siteId = "site-1",
                originSessionId = "session-1",
                originSectorId = "sector-s0",
                originWorkflowType = ReportDraftOriginWorkflowType.XFEEDER.name,
                title = "B",
                observation = "",
                revision = 2,
                createdAtEpochMillis = 2L,
                updatedAtEpochMillis = 20L
            )
        )

        val linked = repository.findLatestLinkedDraft(
            siteId = "site-1",
            originSessionId = "session-1",
            originWorkflowType = ReportDraftOriginWorkflowType.XFEEDER
        )

        assertThat(linked?.id).isEqualTo("d2")
    }

    private class InMemoryReportDraftDao : ReportDraftDao {
        private val drafts = MutableStateFlow<Map<String, ReportDraftEntity>>(emptyMap())

        override suspend fun insert(entity: ReportDraftEntity) {
            drafts.value = drafts.value + (entity.id to entity)
        }

        override fun observeById(draftId: String): Flow<ReportDraftEntity?> {
            return drafts.map { map -> map[draftId] }
        }

        override fun listBySite(siteId: String): Flow<List<ReportDraftEntity>> {
            return drafts.map { map ->
                map.values.filter { it.siteId == siteId }.sortedByDescending { it.updatedAtEpochMillis }
            }
        }

        override suspend fun getById(draftId: String): ReportDraftEntity? = drafts.value[draftId]

        override suspend fun findLatestLinkedBySession(
            siteId: String,
            originSessionId: String,
            originWorkflowType: String?
        ): ReportDraftEntity? {
            return drafts.value.values
                .asSequence()
                .filter { entity ->
                    entity.siteId == siteId &&
                        entity.originSessionId == originSessionId &&
                        entity.originWorkflowType == originWorkflowType
                }
                .maxByOrNull { entity -> entity.updatedAtEpochMillis }
        }

        override suspend fun getRevision(draftId: String): Int? = drafts.value[draftId]?.revision

        override suspend fun updateDraft(
            draftId: String,
            title: String,
            observation: String,
            revision: Int,
            updatedAtEpochMillis: Long
        ) {
            val current = drafts.value[draftId] ?: return
            drafts.value = drafts.value + (
                draftId to current.copy(
                    title = title,
                    observation = observation,
                    revision = revision,
                    updatedAtEpochMillis = updatedAtEpochMillis
                )
            )
        }
    }
}
