package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.ReportClosureProjection
import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.model.QosReportClosureProjection
import com.quartz.platform.domain.model.ReportListClosureSummary
import com.quartz.platform.domain.model.SiteReportListItem
import com.quartz.platform.domain.model.ThroughputReportClosureProjection
import com.quartz.platform.domain.model.RetReportClosureProjection
import com.quartz.platform.domain.model.XfeederReportClosureProjection
import com.quartz.platform.domain.model.XfeederSignal
import com.quartz.platform.domain.repository.ReportDraftRepository
import com.quartz.platform.domain.repository.SyncRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveSiteReportListUseCase @Inject constructor(
    private val reportDraftRepository: ReportDraftRepository,
    private val syncRepository: SyncRepository,
    private val observeSiteReportClosureProjectionsUseCase: ObserveSiteReportClosureProjectionsUseCase
) {
    operator fun invoke(siteId: String): Flow<List<SiteReportListItem>> {
        return reportDraftRepository.listDraftsBySite(siteId)
            .flatMapLatest { drafts ->
                if (drafts.isEmpty()) return@flatMapLatest flowOf(emptyList())

                val draftSyncFlow = combine(
                    drafts.map { draft ->
                        syncRepository.observeSyncTrace(draft.id)
                            .map { syncTrace ->
                                draft to syncTrace
                            }
                    }
                ) { draftAndStates -> draftAndStates.toList() }

                combine(
                    draftSyncFlow,
                    observeSiteReportClosureProjectionsUseCase(siteId)
                ) { draftAndStates, closures ->
                    draftAndStates.map { (draft, syncTrace) ->
                        SiteReportListItem(
                            draftId = draft.id,
                            siteId = draft.siteId,
                            originWorkflowType = draft.originWorkflowType,
                            title = draft.title,
                            revision = draft.revision,
                            updatedAtEpochMillis = draft.updatedAtEpochMillis,
                            syncTrace = syncTrace,
                            closureSummary = resolveClosureSummary(
                                draft = draft,
                                closures = closures
                            )
                        )
                    }
                }
            }
    }

    private fun resolveClosureSummary(
        draft: ReportDraft,
        closures: List<ReportClosureProjection>
    ): ReportListClosureSummary? {
        val guidedSummary = draft.originWorkflowType?.let { workflow ->
            val workflowClosures = closures.filter { closure ->
                closure.workflowType == workflow
            }
            if (workflowClosures.isEmpty()) {
                null
            } else {
                val matched = draft.originSessionId?.let { originSessionId ->
                    workflowClosures.firstOrNull { closure ->
                        closure.sessionId == originSessionId
                    }
                } ?: draft.originSectorId?.let { originSectorId ->
                    workflowClosures.firstOrNull { closure ->
                        closure.sectorId == originSectorId
                    }
                }

                when (matched) {
                    is XfeederReportClosureProjection -> {
                        val signal = when {
                            !matched.relatedSectorCode.isNullOrBlank() -> XfeederSignal.RELATED_SECTOR
                            matched.signalIsUnreliable() -> XfeederSignal.UNRELIABLE
                            (matched.observedSectorCount ?: 0) >= 2 -> XfeederSignal.OBSERVED_MULTIPLE
                            else -> null
                        }
                        ReportListClosureSummary.Xfeeder(
                            sectorOutcome = matched.sectorOutcome,
                            signal = signal
                        )
                    }

                    is RetReportClosureProjection -> {
                        ReportListClosureSummary.Ret(
                            resultOutcome = matched.resultOutcome,
                            requiredStepCount = matched.requiredStepCount,
                            completedRequiredStepCount = matched.completedRequiredStepCount
                        )
                    }

                    is ThroughputReportClosureProjection -> {
                        ReportListClosureSummary.Throughput(
                            sessionStatus = matched.sessionStatus,
                            preconditionsReady = matched.preconditionsReady,
                            requiredStepCount = matched.requiredStepCount,
                            completedRequiredStepCount = matched.completedRequiredStepCount,
                            downloadMbps = matched.downloadMbps,
                            uploadMbps = matched.uploadMbps,
                            latencyMs = matched.latencyMs
                        )
                    }

                    is QosReportClosureProjection -> {
                        ReportListClosureSummary.Qos(
                            sessionStatus = matched.sessionStatus,
                            preconditionsReady = matched.preconditionsReady,
                            requiredStepCount = matched.requiredStepCount,
                            completedRequiredStepCount = matched.completedRequiredStepCount,
                            scriptName = matched.scriptName,
                            configuredRepeatCount = matched.configuredRepeatCount,
                            testFamilyCount = matched.testFamilies.size,
                            iterationCount = matched.iterationCount,
                            successCount = matched.successCount,
                            failureCount = matched.failureCount
                        )
                    }

                    else -> null
                }
            }
        }

        if (guidedSummary != null || draft.originWorkflowType != null) {
            return guidedSummary
        }

        val latestPerformanceProjection = closures
            .filter { projection ->
                projection is ThroughputReportClosureProjection || projection is QosReportClosureProjection
            }
            .maxByOrNull { projection -> projection.updatedAtEpochMillis }
            ?: return null

        return when (latestPerformanceProjection) {
            is ThroughputReportClosureProjection -> {
                ReportListClosureSummary.Throughput(
                    sessionStatus = latestPerformanceProjection.sessionStatus,
                    preconditionsReady = latestPerformanceProjection.preconditionsReady,
                    requiredStepCount = latestPerformanceProjection.requiredStepCount,
                    completedRequiredStepCount = latestPerformanceProjection.completedRequiredStepCount,
                    downloadMbps = latestPerformanceProjection.downloadMbps,
                    uploadMbps = latestPerformanceProjection.uploadMbps,
                    latencyMs = latestPerformanceProjection.latencyMs
                )
            }

            is QosReportClosureProjection -> {
                ReportListClosureSummary.Qos(
                    sessionStatus = latestPerformanceProjection.sessionStatus,
                    preconditionsReady = latestPerformanceProjection.preconditionsReady,
                    requiredStepCount = latestPerformanceProjection.requiredStepCount,
                    completedRequiredStepCount = latestPerformanceProjection.completedRequiredStepCount,
                    scriptName = latestPerformanceProjection.scriptName,
                    configuredRepeatCount = latestPerformanceProjection.configuredRepeatCount,
                    testFamilyCount = latestPerformanceProjection.testFamilies.size,
                    iterationCount = latestPerformanceProjection.iterationCount,
                    successCount = latestPerformanceProjection.successCount,
                    failureCount = latestPerformanceProjection.failureCount
                )
            }

            else -> null
        }
    }

    private fun XfeederReportClosureProjection.signalIsUnreliable(): Boolean {
        return unreliableReason != null
    }
}
