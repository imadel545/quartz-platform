package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.RetGuidedSession
import com.quartz.platform.domain.repository.RetGuidedSessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSectorRetSessionHistoryUseCase @Inject constructor(
    private val repository: RetGuidedSessionRepository
) {
    operator fun invoke(siteId: String, sectorId: String): Flow<List<RetGuidedSession>> {
        return repository.observeSectorSessionHistory(siteId = siteId, sectorId = sectorId)
    }
}
