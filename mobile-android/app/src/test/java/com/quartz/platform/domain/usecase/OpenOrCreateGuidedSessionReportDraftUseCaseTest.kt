package com.quartz.platform.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import com.quartz.platform.domain.repository.ReportDraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OpenOrCreateGuidedSessionReportDraftUseCaseTest {

    @Test
    fun `returns existing linked draft when session origin matches`() = runTest {
        val repository = FakeReportDraftRepository(
            initialDrafts = listOf(
                ReportDraft(
                    id = "draft-1",
                    siteId = "site-1",
                    originSessionId = "session-1",
                    originSectorId = "sector-a",
                    originWorkflowType = ReportDraftOriginWorkflowType.XFEEDER,
                    title = "A",
                    observation = "",
                    revision = 1,
                    createdAtEpochMillis = 1L,
                    updatedAtEpochMillis = 10L
                )
            )
        )
        val useCase = OpenOrCreateGuidedSessionReportDraftUseCase(repository)

        val result = useCase(
            siteId = "site-1",
            originSessionId = "session-1",
            originSectorId = "sector-a",
            originWorkflowType = ReportDraftOriginWorkflowType.XFEEDER
        )

        assertThat(result.created).isFalse()
        assertThat(result.draft.id).isEqualTo("draft-1")
        assertThat(repository.createDraftCalls).isEqualTo(0)
    }

    @Test
    fun `creates draft when no linked draft exists`() = runTest {
        val repository = FakeReportDraftRepository()
        val useCase = OpenOrCreateGuidedSessionReportDraftUseCase(repository)

        val result = useCase(
            siteId = "site-1",
            originSessionId = "session-new",
            originSectorId = "sector-s0",
            originWorkflowType = ReportDraftOriginWorkflowType.RET
        )

        assertThat(result.created).isTrue()
        assertThat(result.draft.originSessionId).isEqualTo("session-new")
        assertThat(result.draft.originSectorId).isEqualTo("sector-s0")
        assertThat(repository.createDraftCalls).isEqualTo(1)
    }

    @Test
    fun `supports performance workflow linkage without sector origin`() = runTest {
        val repository = FakeReportDraftRepository()
        val useCase = OpenOrCreateGuidedSessionReportDraftUseCase(repository)

        val result = useCase(
            siteId = "site-1",
            originSessionId = "perf-session-1",
            originSectorId = null,
            originWorkflowType = ReportDraftOriginWorkflowType.PERFORMANCE
        )

        assertThat(result.created).isTrue()
        assertThat(result.draft.originWorkflowType).isEqualTo(ReportDraftOriginWorkflowType.PERFORMANCE)
        assertThat(result.draft.originSessionId).isEqualTo("perf-session-1")
        assertThat(result.draft.originSectorId).isNull()
    }

    private class FakeReportDraftRepository(
        initialDrafts: List<ReportDraft> = emptyList()
    ) : ReportDraftRepository {
        private val drafts = MutableStateFlow(initialDrafts)
        var createDraftCalls: Int = 0

        override suspend fun createDraft(
            siteId: String,
            originSessionId: String?,
            originSectorId: String?,
            originWorkflowType: ReportDraftOriginWorkflowType?
        ): ReportDraft {
            createDraftCalls += 1
            val created = ReportDraft(
                id = "draft-created",
                siteId = siteId,
                originSessionId = originSessionId,
                originSectorId = originSectorId,
                originWorkflowType = originWorkflowType,
                title = "Nouveau",
                observation = "",
                revision = 1,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L
            )
            drafts.value = listOf(created) + drafts.value
            return created
        }

        override suspend fun updateDraft(draftId: String, title: String, observation: String): ReportDraft? = null

        override suspend fun findLatestLinkedDraft(
            siteId: String,
            originSessionId: String,
            originWorkflowType: ReportDraftOriginWorkflowType?
        ): ReportDraft? {
            return drafts.value
                .asSequence()
                .filter { draft ->
                    draft.siteId == siteId &&
                        draft.originSessionId == originSessionId &&
                        draft.originWorkflowType == originWorkflowType
                }
                .maxByOrNull { draft -> draft.updatedAtEpochMillis }
        }

        override fun observeDraft(draftId: String): Flow<ReportDraft?> =
            drafts.map { list -> list.firstOrNull { draft -> draft.id == draftId } }

        override fun listDraftsBySite(siteId: String): Flow<List<ReportDraft>> =
            drafts.map { list -> list.filter { draft -> draft.siteId == siteId } }
    }
}
