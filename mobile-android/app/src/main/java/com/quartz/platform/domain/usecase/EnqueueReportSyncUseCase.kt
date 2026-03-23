package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.repository.SyncRepository
import javax.inject.Inject

class EnqueueReportSyncUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(reportId: String) {
        syncRepository.enqueueReportUpload(reportId = reportId)
    }
}
