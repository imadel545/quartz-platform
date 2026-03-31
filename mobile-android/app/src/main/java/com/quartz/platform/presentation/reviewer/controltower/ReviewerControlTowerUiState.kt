package com.quartz.platform.presentation.reviewer.controltower

import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import com.quartz.platform.domain.model.ReviewerAttentionSignal
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
    val errorMessage: String? = null
) {
    val filteredItems: List<ReviewerControlTowerItem>
        get() = items.filter(selectedFilter::matches)
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

internal const val STATE_CONTROL_TOWER_SELECTED_FILTER = "state_control_tower_selected_filter"

sealed interface ReviewerControlTowerEvent {
    data class OpenDraft(val draftId: String) : ReviewerControlTowerEvent
    data class OpenSite(val siteId: String) : ReviewerControlTowerEvent
}
