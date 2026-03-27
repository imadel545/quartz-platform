package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.XfeederGuidedSession
import com.quartz.platform.domain.repository.XfeederGuidedSessionRepository
import javax.inject.Inject

class CreateSectorXfeederSessionUseCase @Inject constructor(
    private val repository: XfeederGuidedSessionRepository
) {
    suspend operator fun invoke(
        siteId: String,
        sectorId: String,
        sectorCode: String
    ): XfeederGuidedSession {
        return repository.createSession(
            siteId = siteId,
            sectorId = sectorId,
            sectorCode = sectorCode
        )
    }
}
