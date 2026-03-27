package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.repository.SyncRepository
import javax.inject.Inject

class EnqueueReportDraftSyncUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(reportDraftId: String) {
        syncRepository.enqueueReportUpload(reportDraftId)
    }
}
