package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.repository.ReportDraftRepository
import javax.inject.Inject

class UpdateReportDraftUseCase @Inject constructor(
    private val reportDraftRepository: ReportDraftRepository
) {
    suspend operator fun invoke(
        draftId: String,
        title: String,
        observation: String
    ): ReportDraft? = reportDraftRepository.updateDraft(
        draftId = draftId,
        title = title,
        observation = observation
    )
}
