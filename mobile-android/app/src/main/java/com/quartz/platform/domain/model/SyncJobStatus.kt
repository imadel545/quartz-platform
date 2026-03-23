package com.quartz.platform.domain.model

enum class SyncJobStatus {
    PENDING,
    IN_FLIGHT,
    SUCCEEDED,
    FAILED_RETRYABLE,
    FAILED_TERMINAL
}
