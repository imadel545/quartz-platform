package com.quartz.platform.domain.model

data class ReviewerControlTowerSnapshot(
    val generatedAtEpochMillis: Long,
    val summary: ReviewerControlTowerSummary,
    val items: List<ReviewerControlTowerItem>
)

data class ReviewerControlTowerSummary(
    val totalDraftCount: Int,
    val guidedDraftCount: Int,
    val nonGuidedDraftCount: Int,
    val syncFailedCount: Int,
    val syncPendingCount: Int,
    val qosRiskCount: Int,
    val staleDraftCount: Int,
    val attentionRequiredCount: Int
)

data class ReviewerControlTowerItem(
    val draftId: String,
    val siteId: String,
    val siteCode: String,
    val siteName: String,
    val title: String,
    val revision: Int,
    val updatedAtEpochMillis: Long,
    val syncTrace: ReportSyncTrace,
    val originWorkflowType: ReportDraftOriginWorkflowType?,
    val closureSummary: ReportListClosureSummary?,
    val attentionSignals: Set<ReviewerAttentionSignal>,
    val attentionRank: Int
)

enum class ReviewerAttentionSignal {
    SYNC_FAILED,
    SYNC_PENDING,
    QOS_FAILED_OR_BLOCKED,
    QOS_PREREQUISITES_NOT_READY,
    STALE_DRAFT
}
