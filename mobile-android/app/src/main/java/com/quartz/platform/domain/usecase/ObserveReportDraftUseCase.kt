package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.repository.ReportDraftRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveReportDraftUseCase @Inject constructor(
    private val reportDraftRepository: ReportDraftRepository
) {
    operator fun invoke(draftId: String): Flow<ReportDraft?> = reportDraftRepository.observeDraft(draftId)
}
