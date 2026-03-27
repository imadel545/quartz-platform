package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.ReportSyncTrace
import com.quartz.platform.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveReportDraftSyncTraceUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    operator fun invoke(reportDraftId: String): Flow<ReportSyncTrace> =
        syncRepository.observeSyncTrace(reportDraftId)
}
