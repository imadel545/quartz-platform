package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.SupervisorQueueActionType
import com.quartz.platform.domain.model.SupervisorQueueStatus
import com.quartz.platform.domain.repository.SupervisorQueueRepository
import javax.inject.Inject

class TransitionSupervisorQueueStatusUseCase @Inject constructor(
    private val supervisorQueueRepository: SupervisorQueueRepository
) {
    suspend operator fun invoke(
        draftId: String,
        toStatus: SupervisorQueueStatus,
        actionType: SupervisorQueueActionType,
        note: String? = null,
        triggeredFromFilter: String? = null,
        triggeredFromPreset: String? = null
    ) {
        supervisorQueueRepository.transitionDraftStatus(
            draftId = draftId,
            toStatus = toStatus,
            actionType = actionType,
            note = note,
            triggeredFromFilter = triggeredFromFilter,
            triggeredFromPreset = triggeredFromPreset
        )
    }

    suspend fun bulk(
        draftIds: List<String>,
        toStatus: SupervisorQueueStatus,
        actionType: SupervisorQueueActionType,
        note: String? = null,
        triggeredFromFilter: String? = null,
        triggeredFromPreset: String? = null
    ) {
        if (draftIds.isEmpty()) return
        supervisorQueueRepository.transitionDraftStatuses(
            draftIds = draftIds,
            toStatus = toStatus,
            actionType = actionType,
            note = note,
            triggeredFromFilter = triggeredFromFilter,
            triggeredFromPreset = triggeredFromPreset
        )
    }
}
