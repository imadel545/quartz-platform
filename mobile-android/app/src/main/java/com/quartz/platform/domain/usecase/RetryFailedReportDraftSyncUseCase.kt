package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.repository.SyncRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class RetryFailedReportDraftSyncUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(reportDraftId: String): Boolean {
        val currentTrace = syncRepository.observeSyncTrace(reportDraftId).first()
        if (currentTrace.state != ReportSyncState.FAILED) return false

        syncRepository.enqueueReportUpload(reportDraftId)
        return true
    }
}
