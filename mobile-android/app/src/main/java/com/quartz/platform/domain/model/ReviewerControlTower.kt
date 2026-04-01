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
    val attentionRequiredCount: Int,
    val actNowCount: Int,
    val overdueCount: Int,
    val untriagedCount: Int,
    val inReviewCount: Int,
    val waitingFieldFeedbackCount: Int,
    val resolvedCount: Int
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
    val dominantAttentionSignal: ReviewerAttentionSignal?,
    val staleAgeHours: Int,
    val attentionRank: Int,
    val ageBucket: ReviewerDraftAgeBucket,
    val urgencyClass: ReviewerUrgencyClass,
    val urgencyReason: ReviewerUrgencyReason,
    val urgencyRank: Int,
    val queueStatus: SupervisorQueueStatus,
    val queueLastActionType: SupervisorQueueActionType?,
    val queueLastActionAtEpochMillis: Long?
)

enum class ReviewerAttentionSignal {
    SYNC_FAILED,
    SYNC_PENDING,
    QOS_FAILED_OR_BLOCKED,
    QOS_PREREQUISITES_NOT_READY,
    STALE_DRAFT
}

enum class ReviewerDraftAgeBucket {
    FRESH,
    AGING,
    STALE,
    OVERDUE
}

enum class ReviewerUrgencyClass {
    ACT_NOW,
    HIGH,
    WATCH,
    NORMAL
}

enum class ReviewerUrgencyReason {
    SYNC_FAILED,
    QOS_FAILED_OR_BLOCKED,
    QOS_PREREQUISITES_NOT_READY,
    STALE_DRAFT,
    STALE_GUIDED_WORK,
    STALE_PENDING_SYNC,
    NONE
}

enum class ReviewerControlTowerGroupKey {
    SYNC_FAILED,
    QOS_RISK,
    SYNC_PENDING,
    STALE,
    NO_ATTENTION,
    WORKFLOW_XFEEDER,
    WORKFLOW_RET,
    WORKFLOW_PERFORMANCE,
    WORKFLOW_NON_GUIDED
}
