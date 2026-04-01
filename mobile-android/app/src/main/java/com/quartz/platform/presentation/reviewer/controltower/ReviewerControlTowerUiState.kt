package com.quartz.platform.presentation.reviewer.controltower

import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import com.quartz.platform.domain.model.ReviewerAttentionSignal
import com.quartz.platform.domain.model.ReviewerControlTowerGroupKey
import com.quartz.platform.domain.model.ReviewerControlTowerItem
import com.quartz.platform.domain.model.ReviewerControlTowerSummary

data class ReviewerControlTowerUiState(
    val isLoading: Boolean = true,
    val items: List<ReviewerControlTowerItem> = emptyList(),
    val summary: ReviewerControlTowerSummary = ReviewerControlTowerSummary(
        totalDraftCount = 0,
        guidedDraftCount = 0,
        nonGuidedDraftCount = 0,
        syncFailedCount = 0,
        syncPendingCount = 0,
        qosRiskCount = 0,
        staleDraftCount = 0,
        attentionRequiredCount = 0
    ),
    val selectedFilter: ReviewerControlTowerFilter = ReviewerControlTowerFilter.ALL,
    val selectedGrouping: ReviewerControlTowerGrouping = ReviewerControlTowerGrouping.ATTENTION,
    val selectedPreset: ReviewerQueuePreset = ReviewerQueuePreset.NEEDS_ATTENTION_NOW,
    val progressedDraftIds: Set<String> = emptySet(),
    val retryingDraftIds: Set<String> = emptySet(),
    val isBulkRetryInProgress: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null
) {
    val filteredItems: List<ReviewerControlTowerItem>
        get() = items.filter(selectedFilter::matches)

    val queuedItems: List<ReviewerControlTowerItem>
        get() = selectedPreset.apply(filteredItems)

    val activeQueueItems: List<ReviewerControlTowerItem>
        get() = queuedItems.filterNot { it.draftId in progressedDraftIds }

    val visibleSyncFailedCount: Int
        get() = queuedItems.count { item -> ReviewerAttentionSignal.SYNC_FAILED in item.attentionSignals }

    val groupedItems: List<ReviewerControlTowerGroup>
        get() = activeQueueItems
            .groupBy { item -> selectedGrouping.groupKeyFor(item) }
            .toList()
            .sortedBy { (key, _) -> selectedGrouping.orderFor(key) }
            .map { (key, items) ->
                ReviewerControlTowerGroup(
                    key = key,
                    items = items.sortedWith(
                        compareByDescending<ReviewerControlTowerItem> { it.attentionRank }
                            .thenByDescending { it.updatedAtEpochMillis }
                            .thenBy { it.siteCode }
                    )
                )
            }

    val queueTopDraftId: String?
        get() = activeQueueItems.firstOrNull()?.draftId

    val siteMotifs: List<ReviewerQueueSiteMotif>
        get() = activeQueueItems
            .groupBy { item -> item.siteId }
            .values
            .map { siteItems ->
                val first = siteItems.first()
                ReviewerQueueSiteMotif(
                    siteId = first.siteId,
                    siteCode = first.siteCode,
                    siteName = first.siteName,
                    draftCount = siteItems.size,
                    attentionCount = siteItems.count { it.attentionRank > 0 },
                    dominantAttentionSignal = siteItems
                        .mapNotNull { it.dominantAttentionSignal }
                        .groupingBy { it }
                        .eachCount()
                        .maxByOrNull { it.value }
                        ?.key,
                    topDraftId = siteItems.maxWithOrNull(
                        compareBy<ReviewerControlTowerItem>({ it.attentionRank }, { it.updatedAtEpochMillis })
                    )?.draftId
                )
            }
            .sortedWith(
                compareByDescending<ReviewerQueueSiteMotif> { it.attentionCount }
                    .thenByDescending { it.draftCount }
                    .thenBy { it.siteCode }
            )
            .take(3)

    val workflowMotifs: List<ReviewerQueueWorkflowMotif>
        get() = activeQueueItems
            .groupBy { it.originWorkflowType }
            .entries
            .map { (workflowType, workflowItems) ->
                ReviewerQueueWorkflowMotif(
                    workflowType = workflowType,
                    draftCount = workflowItems.size,
                    attentionCount = workflowItems.count { it.attentionRank > 0 },
                    dominantAttentionSignal = workflowItems
                        .mapNotNull { it.dominantAttentionSignal }
                        .groupingBy { it }
                        .eachCount()
                        .maxByOrNull { it.value }
                        ?.key,
                    topDraftId = workflowItems.maxWithOrNull(
                        compareBy<ReviewerControlTowerItem>({ it.attentionRank }, { it.updatedAtEpochMillis })
                    )?.draftId
                )
            }
            .sortedWith(
                compareByDescending<ReviewerQueueWorkflowMotif> { it.attentionCount }
                    .thenByDescending { it.draftCount }
                    .thenBy { it.workflowType?.name ?: "NON_GUIDED" }
            )
}

enum class ReviewerControlTowerFilter {
    ALL,
    NEEDS_ATTENTION,
    SYNC_FAILED,
    QOS_RISK,
    GUIDED,
    NON_GUIDED;

    fun matches(item: ReviewerControlTowerItem): Boolean {
        return when (this) {
            ALL -> true
            NEEDS_ATTENTION -> item.attentionRank > 0
            SYNC_FAILED -> ReviewerAttentionSignal.SYNC_FAILED in item.attentionSignals
            QOS_RISK -> {
                ReviewerAttentionSignal.QOS_FAILED_OR_BLOCKED in item.attentionSignals ||
                    ReviewerAttentionSignal.QOS_PREREQUISITES_NOT_READY in item.attentionSignals
            }
            GUIDED -> item.originWorkflowType != null
            NON_GUIDED -> item.originWorkflowType == null
        }
    }

    companion object {
        fun fromPersistedNameOrDefault(raw: String?): ReviewerControlTowerFilter {
            return entries.firstOrNull { it.name == raw } ?: ALL
        }

        fun forWorkflowType(workflowType: ReportDraftOriginWorkflowType?): ReviewerControlTowerFilter {
            return if (workflowType == null) NON_GUIDED else GUIDED
        }
    }
}

enum class ReviewerControlTowerGrouping {
    ATTENTION,
    WORKFLOW;

    fun groupKeyFor(item: ReviewerControlTowerItem): ReviewerControlTowerGroupKey {
        return when (this) {
            ATTENTION -> when {
                ReviewerAttentionSignal.SYNC_FAILED in item.attentionSignals -> ReviewerControlTowerGroupKey.SYNC_FAILED
                ReviewerAttentionSignal.QOS_FAILED_OR_BLOCKED in item.attentionSignals ||
                    ReviewerAttentionSignal.QOS_PREREQUISITES_NOT_READY in item.attentionSignals -> ReviewerControlTowerGroupKey.QOS_RISK
                ReviewerAttentionSignal.SYNC_PENDING in item.attentionSignals -> ReviewerControlTowerGroupKey.SYNC_PENDING
                ReviewerAttentionSignal.STALE_DRAFT in item.attentionSignals -> ReviewerControlTowerGroupKey.STALE
                else -> ReviewerControlTowerGroupKey.NO_ATTENTION
            }
            WORKFLOW -> when (item.originWorkflowType) {
                ReportDraftOriginWorkflowType.XFEEDER -> ReviewerControlTowerGroupKey.WORKFLOW_XFEEDER
                ReportDraftOriginWorkflowType.RET -> ReviewerControlTowerGroupKey.WORKFLOW_RET
                ReportDraftOriginWorkflowType.PERFORMANCE -> ReviewerControlTowerGroupKey.WORKFLOW_PERFORMANCE
                null -> ReviewerControlTowerGroupKey.WORKFLOW_NON_GUIDED
            }
        }
    }

    fun orderFor(key: ReviewerControlTowerGroupKey): Int {
        return when (this) {
            ATTENTION -> when (key) {
                ReviewerControlTowerGroupKey.SYNC_FAILED -> 0
                ReviewerControlTowerGroupKey.QOS_RISK -> 1
                ReviewerControlTowerGroupKey.SYNC_PENDING -> 2
                ReviewerControlTowerGroupKey.STALE -> 3
                ReviewerControlTowerGroupKey.NO_ATTENTION -> 4
                else -> 99
            }
            WORKFLOW -> when (key) {
                ReviewerControlTowerGroupKey.WORKFLOW_PERFORMANCE -> 0
                ReviewerControlTowerGroupKey.WORKFLOW_RET -> 1
                ReviewerControlTowerGroupKey.WORKFLOW_XFEEDER -> 2
                ReviewerControlTowerGroupKey.WORKFLOW_NON_GUIDED -> 3
                else -> 99
            }
        }
    }

    companion object {
        fun fromPersistedNameOrDefault(raw: String?): ReviewerControlTowerGrouping {
            return entries.firstOrNull { it.name == raw } ?: ATTENTION
        }
    }
}

enum class ReviewerQueuePreset {
    NEEDS_ATTENTION_NOW,
    SYNC_FAILURES_FIRST,
    QOS_RISK_FIRST,
    STALE_GUIDED_WORK,
    GUIDED_UNRESOLVED;

    fun apply(items: List<ReviewerControlTowerItem>): List<ReviewerControlTowerItem> {
        val filtered = when (this) {
            NEEDS_ATTENTION_NOW -> items.filter { item -> item.attentionRank > 0 }
            SYNC_FAILURES_FIRST -> items.filter { item -> ReviewerAttentionSignal.SYNC_FAILED in item.attentionSignals }
            QOS_RISK_FIRST -> items.filter { item ->
                ReviewerAttentionSignal.QOS_FAILED_OR_BLOCKED in item.attentionSignals ||
                    ReviewerAttentionSignal.QOS_PREREQUISITES_NOT_READY in item.attentionSignals
            }
            STALE_GUIDED_WORK -> items.filter { item ->
                item.originWorkflowType != null && ReviewerAttentionSignal.STALE_DRAFT in item.attentionSignals
            }
            GUIDED_UNRESOLVED -> items.filter { item ->
                item.originWorkflowType != null && (
                    item.syncTrace.state != com.quartz.platform.domain.model.ReportSyncState.SYNCED ||
                        item.attentionRank > 0 ||
                        item.hasIncompleteGuidedClosure()
                    )
            }
        }

        val sorted = filtered.sortedWith(
            compareByDescending<ReviewerControlTowerItem> { it.attentionRank }
                .thenByDescending { it.updatedAtEpochMillis }
                .thenBy { it.siteCode }
        )
        return if (sorted.isNotEmpty()) sorted else items
    }

    private fun ReviewerControlTowerItem.hasIncompleteGuidedClosure(): Boolean {
        val closure = closureSummary ?: return false
        return when (closure) {
            is com.quartz.platform.domain.model.ReportListClosureSummary.Ret ->
                closure.completedRequiredStepCount < closure.requiredStepCount
            is com.quartz.platform.domain.model.ReportListClosureSummary.Throughput ->
                !closure.preconditionsReady || closure.completedRequiredStepCount < closure.requiredStepCount
            is com.quartz.platform.domain.model.ReportListClosureSummary.Qos ->
                !closure.preconditionsReady || closure.completedRequiredStepCount < closure.requiredStepCount
            is com.quartz.platform.domain.model.ReportListClosureSummary.Xfeeder -> false
        }
    }

    companion object {
        fun fromPersistedNameOrDefault(raw: String?): ReviewerQueuePreset {
            return entries.firstOrNull { it.name == raw } ?: NEEDS_ATTENTION_NOW
        }
    }
}

data class ReviewerControlTowerGroup(
    val key: ReviewerControlTowerGroupKey,
    val items: List<ReviewerControlTowerItem>
)

data class ReviewerQueueSiteMotif(
    val siteId: String,
    val siteCode: String,
    val siteName: String,
    val draftCount: Int,
    val attentionCount: Int,
    val dominantAttentionSignal: ReviewerAttentionSignal?,
    val topDraftId: String?
)

data class ReviewerQueueWorkflowMotif(
    val workflowType: ReportDraftOriginWorkflowType?,
    val draftCount: Int,
    val attentionCount: Int,
    val dominantAttentionSignal: ReviewerAttentionSignal?,
    val topDraftId: String?
)

internal const val STATE_CONTROL_TOWER_SELECTED_FILTER = "state_control_tower_selected_filter"
internal const val STATE_CONTROL_TOWER_SELECTED_GROUPING = "state_control_tower_selected_grouping"
internal const val STATE_CONTROL_TOWER_SELECTED_PRESET = "state_control_tower_selected_preset"
internal const val STATE_CONTROL_TOWER_PROGRESS_DRAFT_IDS = "state_control_tower_progress_draft_ids"

sealed interface ReviewerControlTowerEvent {
    data class OpenDraft(val draftId: String) : ReviewerControlTowerEvent
    data class OpenSite(val siteId: String) : ReviewerControlTowerEvent
}
