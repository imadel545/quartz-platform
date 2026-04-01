package com.quartz.platform.domain.usecase

import javax.inject.Inject

class RetryControlTowerFailedSyncUseCase @Inject constructor(
    private val retryFailedReportDraftSyncUseCase: RetryFailedReportDraftSyncUseCase
) {
    suspend operator fun invoke(draftIds: List<String>): Int {
        var queued = 0
        draftIds.distinct().forEach { draftId ->
            if (retryFailedReportDraftSyncUseCase(draftId)) {
                queued += 1
            }
        }
        return queued
    }
}
