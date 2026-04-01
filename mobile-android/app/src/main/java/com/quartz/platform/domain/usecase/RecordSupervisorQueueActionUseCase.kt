package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.SupervisorQueueActionType
import com.quartz.platform.domain.repository.SupervisorQueueRepository
import javax.inject.Inject

class RecordSupervisorQueueActionUseCase @Inject constructor(
    private val supervisorQueueRepository: SupervisorQueueRepository
) {
    suspend operator fun invoke(
        draftId: String,
        actionType: SupervisorQueueActionType,
        note: String? = null,
        triggeredFromFilter: String? = null,
        triggeredFromPreset: String? = null
    ) {
        supervisorQueueRepository.recordDraftAction(
            draftId = draftId,
            actionType = actionType,
            note = note,
            triggeredFromFilter = triggeredFromFilter,
            triggeredFromPreset = triggeredFromPreset
        )
    }
}
