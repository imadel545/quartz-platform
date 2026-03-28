package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.RetGuidedSession
import com.quartz.platform.domain.repository.RetGuidedSessionRepository
import javax.inject.Inject

class CreateSectorRetSessionUseCase @Inject constructor(
    private val repository: RetGuidedSessionRepository
) {
    suspend operator fun invoke(
        siteId: String,
        sectorId: String,
        sectorCode: String
    ): RetGuidedSession {
        return repository.createSession(
            siteId = siteId,
            sectorId = sectorId,
            sectorCode = sectorCode
        )
    }
}
