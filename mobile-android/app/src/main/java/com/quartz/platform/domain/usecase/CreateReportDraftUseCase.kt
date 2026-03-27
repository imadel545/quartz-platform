package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.repository.ReportDraftRepository
import javax.inject.Inject

class CreateReportDraftUseCase @Inject constructor(
    private val reportDraftRepository: ReportDraftRepository
) {
    suspend operator fun invoke(
        siteId: String,
        originSessionId: String? = null,
        originSectorId: String? = null
    ): ReportDraft = reportDraftRepository.createDraft(
        siteId = siteId,
        originSessionId = originSessionId,
        originSectorId = originSectorId
    )
}
