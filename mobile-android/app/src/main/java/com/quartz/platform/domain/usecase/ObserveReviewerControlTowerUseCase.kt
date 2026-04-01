package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.QosExecutionEngineState
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import com.quartz.platform.domain.model.ReportListClosureSummary
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.ReviewerAttentionSignal
import com.quartz.platform.domain.model.ReviewerControlTowerItem
import com.quartz.platform.domain.model.ReviewerControlTowerSnapshot
import com.quartz.platform.domain.model.ReviewerControlTowerSummary
import com.quartz.platform.domain.model.SiteSummary
import com.quartz.platform.domain.repository.ReportDraftRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

private const val STALE_DRAFT_THRESHOLD_MILLIS: Long = 24L * 60L * 60L * 1000L
private const val FRESH_CRITICAL_DRAFT_THRESHOLD_MILLIS: Long = 2L * 60L * 60L * 1000L

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveReviewerControlTowerUseCase @Inject constructor(
    private val reportDraftRepository: ReportDraftRepository,
    private val observeSiteReportListUseCase: ObserveSiteReportListUseCase,
    private val observeSiteListUseCase: ObserveSiteListUseCase
) {
    operator fun invoke(): Flow<ReviewerControlTowerSnapshot> {
        val allReportsFlow = reportDraftRepository.listAllDrafts()
            .flatMapLatest { drafts ->
                val siteIds = drafts.map { draft -> draft.siteId }.distinct()
                if (siteIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(siteIds.map { siteId -> observeSiteReportListUseCase(siteId) }) { rows ->
                        rows.toList().flatMap { siteRows -> siteRows }
                    }
                }
            }

        return combine(
            allReportsFlow,
            observeSiteListUseCase()
        ) { reportRows, sites ->
            val now = System.currentTimeMillis()
            val siteIndex = sites.associateBy { site -> site.id }
            val items = reportRows
                .map { row -> row.toControlTowerItem(siteIndex = siteIndex, nowEpochMillis = now) }
                .sortedWith(
                    compareByDescending<ReviewerControlTowerItem> { item -> item.attentionRank }
                        .thenByDescending { item -> item.updatedAtEpochMillis }
                        .thenBy { item -> item.siteCode }
                        .thenBy { item -> item.title.lowercase() }
                )

            ReviewerControlTowerSnapshot(
                generatedAtEpochMillis = now,
                summary = buildSummary(items),
                items = items
            )
        }
    }

    private fun com.quartz.platform.domain.model.SiteReportListItem.toControlTowerItem(
        siteIndex: Map<String, SiteSummary>,
        nowEpochMillis: Long
    ): ReviewerControlTowerItem {
        val site = siteIndex[siteId]
        val ageMillis = (nowEpochMillis - updatedAtEpochMillis).coerceAtLeast(0L)
        val isStale = ageMillis >= STALE_DRAFT_THRESHOLD_MILLIS
        val attentionSignals = buildAttentionSignals(
            syncState = syncState,
            closureSummary = closureSummary,
            isStale = isStale
        )
        val staleAgeHours = (ageMillis / (60L * 60L * 1000L)).toInt()
        val dominantAttentionSignal = attentionSignals.sortedByDescending { signalWeight(it) }.firstOrNull()
        return ReviewerControlTowerItem(
            draftId = draftId,
            siteId = siteId,
            siteCode = site?.externalCode ?: siteId,
            siteName = site?.name ?: "Site $siteId",
            title = title,
            revision = revision,
            updatedAtEpochMillis = updatedAtEpochMillis,
            syncTrace = syncTrace,
            originWorkflowType = originWorkflowType,
            closureSummary = closureSummary,
            attentionSignals = attentionSignals,
            dominantAttentionSignal = dominantAttentionSignal,
            staleAgeHours = staleAgeHours,
            attentionRank = computeAttentionRank(
                signals = attentionSignals,
                ageMillis = ageMillis
            )
        )
    }

    private fun buildAttentionSignals(
        syncState: ReportSyncState,
        closureSummary: ReportListClosureSummary?,
        isStale: Boolean
    ): Set<ReviewerAttentionSignal> {
        return buildSet {
            if (syncState == ReportSyncState.FAILED) add(ReviewerAttentionSignal.SYNC_FAILED)
            if (syncState == ReportSyncState.PENDING) add(ReviewerAttentionSignal.SYNC_PENDING)
            if (isStale) add(ReviewerAttentionSignal.STALE_DRAFT)

            if (closureSummary is ReportListClosureSummary.Qos) {
                if (closureSummary.failedFamilyCount > 0 || closureSummary.blockedFamilyCount > 0) {
                    add(ReviewerAttentionSignal.QOS_FAILED_OR_BLOCKED)
                }
                if (!closureSummary.preconditionsReady ||
                    closureSummary.executionEngineState == QosExecutionEngineState.PREFLIGHT_BLOCKED
                ) {
                    add(ReviewerAttentionSignal.QOS_PREREQUISITES_NOT_READY)
                }
            }
        }
    }

    private fun computeAttentionRank(
        signals: Set<ReviewerAttentionSignal>,
        ageMillis: Long
    ): Int {
        var rank = signals.sumOf(::signalWeight)
        if (ReviewerAttentionSignal.SYNC_FAILED in signals && ageMillis <= FRESH_CRITICAL_DRAFT_THRESHOLD_MILLIS) {
            rank += 15
        }
        if (ReviewerAttentionSignal.SYNC_PENDING in signals && ReviewerAttentionSignal.STALE_DRAFT in signals) {
            rank += 5
        }
        return rank
    }

    private fun signalWeight(signal: ReviewerAttentionSignal): Int {
        return when (signal) {
            ReviewerAttentionSignal.SYNC_FAILED -> 50
            ReviewerAttentionSignal.QOS_FAILED_OR_BLOCKED -> 35
            ReviewerAttentionSignal.QOS_PREREQUISITES_NOT_READY -> 20
            ReviewerAttentionSignal.SYNC_PENDING -> 12
            ReviewerAttentionSignal.STALE_DRAFT -> 8
        }
    }

    private fun buildSummary(items: List<ReviewerControlTowerItem>): ReviewerControlTowerSummary {
        val guidedCount = items.count { item -> item.originWorkflowType != null }
        val nonGuidedCount = items.size - guidedCount
        return ReviewerControlTowerSummary(
            totalDraftCount = items.size,
            guidedDraftCount = guidedCount,
            nonGuidedDraftCount = nonGuidedCount,
            syncFailedCount = items.count { item -> item.syncTrace.state == ReportSyncState.FAILED },
            syncPendingCount = items.count { item -> item.syncTrace.state == ReportSyncState.PENDING },
            qosRiskCount = items.count { item ->
                ReviewerAttentionSignal.QOS_FAILED_OR_BLOCKED in item.attentionSignals ||
                    ReviewerAttentionSignal.QOS_PREREQUISITES_NOT_READY in item.attentionSignals
            },
            staleDraftCount = items.count { item -> ReviewerAttentionSignal.STALE_DRAFT in item.attentionSignals },
            attentionRequiredCount = items.count { item -> item.attentionRank > 0 }
        )
    }
}
