package com.quartz.platform.presentation.reviewer.controltower

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import com.quartz.platform.domain.model.ReportListClosureSummary
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.ReportSyncTrace
import com.quartz.platform.domain.model.ReviewerAttentionSignal
import com.quartz.platform.domain.model.ReviewerControlTowerItem
import com.quartz.platform.domain.model.ReviewerControlTowerSummary
import com.quartz.platform.domain.model.ReviewerDraftAgeBucket
import com.quartz.platform.domain.model.ReviewerUrgencyClass
import com.quartz.platform.domain.model.ReviewerUrgencyReason
import com.quartz.platform.domain.model.RetResultOutcome
import org.junit.Test

class ReviewerControlTowerUiStateTest {

    @Test
    fun sync_failures_preset_prioritizes_failed_rows_and_keeps_fallback() {
        val failed = item(
            draftId = "d-failed",
            syncState = ReportSyncState.FAILED,
            attentionSignals = setOf(ReviewerAttentionSignal.SYNC_FAILED),
            attentionRank = 50
        )
        val clean = item(
            draftId = "d-clean",
            syncState = ReportSyncState.SYNCED,
            attentionSignals = emptySet(),
            attentionRank = 0
        )

        val state = baseState(items = listOf(clean, failed), selectedPreset = ReviewerQueuePreset.SYNC_FAILURES_FIRST)
        assertThat(state.queuedItems.map { it.draftId }).containsExactly("d-failed")

        val noFailedState = baseState(items = listOf(clean), selectedPreset = ReviewerQueuePreset.SYNC_FAILURES_FIRST)
        assertThat(noFailedState.queuedItems.map { it.draftId }).containsExactly("d-clean")
    }

    @Test
    fun queue_progress_excludes_progressed_rows_and_reset_restores_all() {
        val first = item(draftId = "d-1", attentionRank = 40, attentionSignals = setOf(ReviewerAttentionSignal.SYNC_PENDING))
        val second = item(draftId = "d-2", attentionRank = 20, attentionSignals = setOf(ReviewerAttentionSignal.STALE_DRAFT))

        val state = baseState(
            items = listOf(first, second),
            selectedPreset = ReviewerQueuePreset.NEEDS_ATTENTION_NOW,
            progressedDraftIds = setOf("d-1")
        )

        assertThat(state.activeQueueItems.map { it.draftId }).containsExactly("d-2")
        assertThat(state.queueTopDraftId).isEqualTo("d-2")
    }

    @Test
    fun motifs_are_aggregated_from_active_queue() {
        val a1 = item(
            draftId = "a1",
            siteId = "site-a",
            siteCode = "A",
            siteName = "Site A",
            originWorkflowType = ReportDraftOriginWorkflowType.RET,
            attentionSignals = setOf(ReviewerAttentionSignal.SYNC_FAILED),
            attentionRank = 50
        )
        val a2 = item(
            draftId = "a2",
            siteId = "site-a",
            siteCode = "A",
            siteName = "Site A",
            originWorkflowType = ReportDraftOriginWorkflowType.RET,
            attentionSignals = setOf(ReviewerAttentionSignal.STALE_DRAFT),
            attentionRank = 10
        )
        val b1 = item(
            draftId = "b1",
            siteId = "site-b",
            siteCode = "B",
            siteName = "Site B",
            originWorkflowType = ReportDraftOriginWorkflowType.PERFORMANCE,
            attentionSignals = setOf(ReviewerAttentionSignal.QOS_FAILED_OR_BLOCKED),
            attentionRank = 35
        )

        val state = baseState(items = listOf(a1, a2, b1), selectedPreset = ReviewerQueuePreset.NEEDS_ATTENTION_NOW)
        assertThat(state.siteMotifs.first().siteId).isEqualTo("site-a")
        assertThat(state.siteMotifs.first().draftCount).isEqualTo(2)
        assertThat(state.workflowMotifs.first().workflowType).isEqualTo(ReportDraftOriginWorkflowType.RET)
        assertThat(state.workflowMotifs.first().draftCount).isEqualTo(2)
    }

    @Test
    fun guided_unresolved_preset_keeps_guided_rows_needing_followup() {
        val doneGuided = item(
            draftId = "done-guided",
            originWorkflowType = ReportDraftOriginWorkflowType.RET,
            syncState = ReportSyncState.SYNCED,
            attentionSignals = emptySet(),
            attentionRank = 0,
            closureSummary = ReportListClosureSummary.Ret(
                resultOutcome = RetResultOutcome.PASS,
                requiredStepCount = 2,
                completedRequiredStepCount = 2
            )
        )
        val unresolvedGuided = item(
            draftId = "unresolved-guided",
            originWorkflowType = ReportDraftOriginWorkflowType.RET,
            syncState = ReportSyncState.PENDING,
            attentionSignals = setOf(ReviewerAttentionSignal.SYNC_PENDING),
            attentionRank = 12,
            closureSummary = ReportListClosureSummary.Ret(
                resultOutcome = RetResultOutcome.PASS,
                requiredStepCount = 3,
                completedRequiredStepCount = 2
            )
        )
        val nonGuided = item(
            draftId = "non-guided",
            originWorkflowType = null,
            syncState = ReportSyncState.PENDING,
            attentionSignals = setOf(ReviewerAttentionSignal.SYNC_PENDING),
            attentionRank = 12
        )

        val state = baseState(
            items = listOf(doneGuided, unresolvedGuided, nonGuided),
            selectedPreset = ReviewerQueuePreset.GUIDED_UNRESOLVED
        )
        assertThat(state.queuedItems.map { it.draftId }).containsExactly("unresolved-guided")
    }

    @Test
    fun act_now_overdue_preset_keeps_act_now_or_overdue_rows() {
        val actNow = item(
            draftId = "act-now",
            attentionSignals = setOf(ReviewerAttentionSignal.SYNC_FAILED),
            urgencyClass = ReviewerUrgencyClass.ACT_NOW,
            ageBucket = ReviewerDraftAgeBucket.AGING,
            attentionRank = 60
        )
        val overdue = item(
            draftId = "overdue",
            attentionSignals = setOf(ReviewerAttentionSignal.STALE_DRAFT),
            urgencyClass = ReviewerUrgencyClass.WATCH,
            ageBucket = ReviewerDraftAgeBucket.OVERDUE,
            attentionRank = 12
        )
        val normal = item(
            draftId = "normal",
            attentionSignals = emptySet(),
            urgencyClass = ReviewerUrgencyClass.NORMAL,
            ageBucket = ReviewerDraftAgeBucket.FRESH,
            attentionRank = 0
        )

        val state = baseState(
            items = listOf(normal, overdue, actNow),
            selectedPreset = ReviewerQueuePreset.ACT_NOW_OVERDUE
        )
        assertThat(state.queuedItems.map { it.draftId }).containsExactly("act-now", "overdue").inOrder()
    }

    private fun baseState(
        items: List<ReviewerControlTowerItem>,
        selectedPreset: ReviewerQueuePreset,
        progressedDraftIds: Set<String> = emptySet()
    ): ReviewerControlTowerUiState {
        return ReviewerControlTowerUiState(
            isLoading = false,
            items = items,
            summary = ReviewerControlTowerSummary(
                totalDraftCount = items.size,
                guidedDraftCount = items.count { it.originWorkflowType != null },
                nonGuidedDraftCount = items.count { it.originWorkflowType == null },
                syncFailedCount = items.count { it.syncTrace.state == ReportSyncState.FAILED },
                syncPendingCount = items.count { it.syncTrace.state == ReportSyncState.PENDING },
                qosRiskCount = items.count { ReviewerAttentionSignal.QOS_FAILED_OR_BLOCKED in it.attentionSignals },
                staleDraftCount = items.count { ReviewerAttentionSignal.STALE_DRAFT in it.attentionSignals },
                attentionRequiredCount = items.count { it.attentionRank > 0 },
                actNowCount = items.count { it.urgencyClass == ReviewerUrgencyClass.ACT_NOW },
                overdueCount = items.count { it.ageBucket == ReviewerDraftAgeBucket.OVERDUE }
            ),
            selectedPreset = selectedPreset,
            progressedDraftIds = progressedDraftIds
        )
    }

    private fun item(
        draftId: String,
        siteId: String = "site-1",
        siteCode: String = "QRTZ-001",
        siteName: String = "Site",
        originWorkflowType: ReportDraftOriginWorkflowType? = ReportDraftOriginWorkflowType.PERFORMANCE,
        syncState: ReportSyncState = ReportSyncState.LOCAL_ONLY,
        attentionSignals: Set<ReviewerAttentionSignal> = emptySet(),
        ageBucket: ReviewerDraftAgeBucket = ReviewerDraftAgeBucket.FRESH,
        urgencyClass: ReviewerUrgencyClass = ReviewerUrgencyClass.NORMAL,
        urgencyReason: ReviewerUrgencyReason = ReviewerUrgencyReason.NONE,
        attentionRank: Int = 0,
        closureSummary: ReportListClosureSummary? = null
    ): ReviewerControlTowerItem {
        return ReviewerControlTowerItem(
            draftId = draftId,
            siteId = siteId,
            siteCode = siteCode,
            siteName = siteName,
            title = draftId,
            revision = 1,
            updatedAtEpochMillis = 10L,
            syncTrace = ReportSyncTrace(
                state = syncState,
                lastAttemptAtEpochMillis = null,
                retryCount = 0,
                failureReason = null
            ),
            originWorkflowType = originWorkflowType,
            closureSummary = closureSummary,
            attentionSignals = attentionSignals,
            dominantAttentionSignal = attentionSignals.firstOrNull(),
            staleAgeHours = if (ReviewerAttentionSignal.STALE_DRAFT in attentionSignals) 30 else 1,
            attentionRank = attentionRank,
            ageBucket = ageBucket,
            urgencyClass = urgencyClass,
            urgencyReason = urgencyReason,
            urgencyRank = when (urgencyClass) {
                ReviewerUrgencyClass.ACT_NOW -> 100
                ReviewerUrgencyClass.HIGH -> 60
                ReviewerUrgencyClass.WATCH -> 30
                ReviewerUrgencyClass.NORMAL -> 0
            }
        )
    }
}
