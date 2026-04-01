package com.quartz.platform.domain.model

enum class SupervisorQueueStatus {
    UNTRIAGED,
    IN_REVIEW,
    WAITING_FIELD_FEEDBACK,
    RESOLVED
}

enum class SupervisorQueueActionType {
    MARK_IN_REVIEW,
    BULK_MARK_IN_REVIEW,
    MARK_WAITING_FIELD_FEEDBACK,
    MARK_RESOLVED,
    REOPEN_TO_UNTRIAGED,
    RETRY_SYNC
}

data class SupervisorQueueState(
    val draftId: String,
    val status: SupervisorQueueStatus,
    val lastActionType: SupervisorQueueActionType?,
    val lastActionAtEpochMillis: Long?,
    val lastActionNote: String?,
    val updatedAtEpochMillis: Long
)

data class SupervisorQueueAction(
    val id: String,
    val draftId: String,
    val actionType: SupervisorQueueActionType,
    val fromStatus: SupervisorQueueStatus?,
    val toStatus: SupervisorQueueStatus?,
    val note: String?,
    val triggeredFromFilter: String?,
    val triggeredFromPreset: String?,
    val actedAtEpochMillis: Long
)
