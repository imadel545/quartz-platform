package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.XfeederGuidedSession
import com.quartz.platform.domain.repository.XfeederGuidedSessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSectorXfeederSessionHistoryUseCase @Inject constructor(
    private val repository: XfeederGuidedSessionRepository
) {
    operator fun invoke(siteId: String, sectorId: String): Flow<List<XfeederGuidedSession>> {
        return repository.observeSectorSessionHistory(siteId = siteId, sectorId = sectorId)
    }
}
