package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.XfeederGuidedSession
import com.quartz.platform.domain.repository.XfeederGuidedSessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveLatestSectorXfeederSessionUseCase @Inject constructor(
    private val repository: XfeederGuidedSessionRepository
) {
    operator fun invoke(siteId: String, sectorId: String): Flow<XfeederGuidedSession?> {
        return repository.observeLatestSectorSession(siteId, sectorId)
    }
}
