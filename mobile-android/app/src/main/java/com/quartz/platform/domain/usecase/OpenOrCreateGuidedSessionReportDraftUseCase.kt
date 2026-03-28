package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import com.quartz.platform.domain.repository.ReportDraftRepository
import javax.inject.Inject

data class GuidedSessionReportDraftResult(
    val draft: ReportDraft,
    val created: Boolean
)

class OpenOrCreateGuidedSessionReportDraftUseCase @Inject constructor(
    private val reportDraftRepository: ReportDraftRepository
) {
    suspend operator fun invoke(
        siteId: String,
        originSessionId: String,
        originSectorId: String,
        originWorkflowType: ReportDraftOriginWorkflowType
    ): GuidedSessionReportDraftResult {
        val existing = reportDraftRepository.findLatestLinkedDraft(
            siteId = siteId,
            originSessionId = originSessionId,
            originWorkflowType = originWorkflowType
        )

        return if (existing != null) {
            GuidedSessionReportDraftResult(
                draft = existing,
                created = false
            )
        } else {
            GuidedSessionReportDraftResult(
                draft = reportDraftRepository.createDraft(
                    siteId = siteId,
                    originSessionId = originSessionId,
                    originSectorId = originSectorId,
                    originWorkflowType = originWorkflowType
                ),
                created = true
            )
        }
    }
}
