package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.ReportClosureProjection
import com.quartz.platform.domain.model.toReportClosureProjection
import com.quartz.platform.domain.repository.PerformanceSessionRepository
import com.quartz.platform.domain.repository.RetGuidedSessionRepository
import com.quartz.platform.domain.repository.XfeederGuidedSessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveSiteReportClosureProjectionsUseCase @Inject constructor(
    private val xfeederRepository: XfeederGuidedSessionRepository,
    private val retRepository: RetGuidedSessionRepository,
    private val performanceSessionRepository: PerformanceSessionRepository
) {
    operator fun invoke(siteId: String): Flow<List<ReportClosureProjection>> {
        return combine(
            xfeederRepository.observeSiteClosureProjections(siteId),
            retRepository.observeSiteClosureProjections(siteId),
            performanceSessionRepository.observeSiteSessionHistory(siteId)
        ) { xfeederClosures, retClosures, performanceSessions ->
            buildList {
                addAll(xfeederClosures.map { closure -> closure.toReportClosureProjection() })
                addAll(retClosures.map { closure -> closure.toReportClosureProjection() })
                addAll(performanceSessions.map { session -> session.toReportClosureProjection() })
            }.sortedWith(
                compareByDescending<ReportClosureProjection> { projection ->
                    projection.updatedAtEpochMillis
                }.thenBy { projection ->
                    projection.sectorCode.orEmpty()
                }
            )
        }
    }
}
