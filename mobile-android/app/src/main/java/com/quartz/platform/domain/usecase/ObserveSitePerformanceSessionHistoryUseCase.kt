package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.PerformanceSession
import com.quartz.platform.domain.repository.PerformanceSessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSitePerformanceSessionHistoryUseCase @Inject constructor(
    private val repository: PerformanceSessionRepository
) {
    operator fun invoke(siteId: String): Flow<List<PerformanceSession>> {
        return repository.observeSiteSessionHistory(siteId)
    }
}
