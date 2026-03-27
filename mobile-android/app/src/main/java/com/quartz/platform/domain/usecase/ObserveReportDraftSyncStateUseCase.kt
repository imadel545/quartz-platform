package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveReportDraftSyncStateUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    operator fun invoke(reportDraftId: String): Flow<ReportSyncState> =
        syncRepository.observeSyncTrace(reportDraftId).map { trace -> trace.state }
}
