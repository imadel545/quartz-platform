package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.GuidedSessionClosureProjection
import com.quartz.platform.domain.repository.XfeederGuidedSessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSiteClosureProjectionsUseCase @Inject constructor(
    private val repository: XfeederGuidedSessionRepository
) {
    operator fun invoke(siteId: String): Flow<List<GuidedSessionClosureProjection>> {
        return repository.observeSiteClosureProjections(siteId)
    }
}
