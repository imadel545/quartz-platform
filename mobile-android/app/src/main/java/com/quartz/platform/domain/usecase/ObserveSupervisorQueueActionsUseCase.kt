package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.SupervisorQueueAction
import com.quartz.platform.domain.repository.SupervisorQueueRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSupervisorQueueActionsUseCase @Inject constructor(
    private val supervisorQueueRepository: SupervisorQueueRepository
) {
    operator fun invoke(): Flow<List<SupervisorQueueAction>> = supervisorQueueRepository.observeQueueActions()
}
