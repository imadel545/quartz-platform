package com.quartz.platform.domain.repository

import com.quartz.platform.domain.model.SupervisorQueueAction
import com.quartz.platform.domain.model.SupervisorQueueActionType
import com.quartz.platform.domain.model.SupervisorQueueState
import com.quartz.platform.domain.model.SupervisorQueueStatus
import kotlinx.coroutines.flow.Flow

interface SupervisorQueueRepository {
    fun observeQueueStates(): Flow<List<SupervisorQueueState>>

    fun observeQueueActions(): Flow<List<SupervisorQueueAction>>

    suspend fun transitionDraftStatus(
        draftId: String,
        toStatus: SupervisorQueueStatus,
        actionType: SupervisorQueueActionType,
        note: String?,
        triggeredFromFilter: String?,
        triggeredFromPreset: String?
    )

    suspend fun transitionDraftStatuses(
        draftIds: List<String>,
        toStatus: SupervisorQueueStatus,
        actionType: SupervisorQueueActionType,
        note: String?,
        triggeredFromFilter: String?,
        triggeredFromPreset: String?
    )

    suspend fun recordDraftAction(
        draftId: String,
        actionType: SupervisorQueueActionType,
        note: String?,
        triggeredFromFilter: String?,
        triggeredFromPreset: String?
    )
}
