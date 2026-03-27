package com.quartz.platform.domain.model

data class ReportSyncTrace(
    val state: ReportSyncState,
    val lastAttemptAtEpochMillis: Long?,
    val retryCount: Int,
    val failureReason: String?
) {
    companion object {
        fun localOnly(): ReportSyncTrace {
            return ReportSyncTrace(
                state = ReportSyncState.LOCAL_ONLY,
                lastAttemptAtEpochMillis = null,
                retryCount = 0,
                failureReason = null
            )
        }
    }
}
