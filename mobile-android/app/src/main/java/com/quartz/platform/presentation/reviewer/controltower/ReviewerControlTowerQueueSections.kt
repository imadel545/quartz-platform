package com.quartz.platform.presentation.reviewer.controltower

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quartz.platform.R
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.ReviewerAttentionSignal
import com.quartz.platform.domain.model.ReviewerControlTowerGroupKey
import com.quartz.platform.domain.model.ReviewerControlTowerItem
import com.quartz.platform.domain.model.ReviewerDraftAgeBucket
import com.quartz.platform.domain.model.ReviewerUrgencyClass
import com.quartz.platform.domain.model.ReviewerUrgencyReason
import com.quartz.platform.domain.model.SupervisorQueueActionType
import com.quartz.platform.domain.model.SupervisorQueueStatus
import com.quartz.platform.presentation.components.AdvancedDisclosureButton
import com.quartz.platform.presentation.components.OperationalSectionCard
import com.quartz.platform.presentation.components.OperationalSeverity
import com.quartz.platform.presentation.components.OperationalSignal
import com.quartz.platform.presentation.components.OperationalSignalRow
import com.quartz.platform.presentation.sync.syncStateLabelRes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun ControlTowerGroupSection(
    group: ReviewerControlTowerGroup,
    onOpenDraft: (String) -> Unit,
    onOpenSite: (String) -> Unit,
    onRetryDraftSync: (String) -> Unit,
    retryingDraftIds: Set<String>,
    transitioningDraftIds: Set<String>,
    onMarkDraftInReview: (String) -> Unit,
    onMarkDraftWaitingFeedback: (String) -> Unit,
    onMarkDraftResolved: (String) -> Unit,
    onReopenDraft: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(groupTitleRes(group.key)),
                style = MaterialTheme.typography.titleSmall
            )
            group.items.forEach { item ->
                ReviewerControlTowerItemCard(
                    item = item,
                    isRetrying = item.draftId in retryingDraftIds,
                    isQueueTransitioning = item.draftId in transitioningDraftIds,
                    onOpenDraft = onOpenDraft,
                    onOpenSite = onOpenSite,
                    onRetryDraftSync = onRetryDraftSync,
                    onMarkDraftInReview = onMarkDraftInReview,
                    onMarkDraftWaitingFeedback = onMarkDraftWaitingFeedback,
                    onMarkDraftResolved = onMarkDraftResolved,
                    onReopenDraft = onReopenDraft
                )
            }
        }
    }
}

@Composable
private fun ReviewerControlTowerItemCard(
    item: ReviewerControlTowerItem,
    isRetrying: Boolean,
    isQueueTransitioning: Boolean,
    onOpenDraft: (String) -> Unit,
    onOpenSite: (String) -> Unit,
    onRetryDraftSync: (String) -> Unit,
    onMarkDraftInReview: (String) -> Unit,
    onMarkDraftWaitingFeedback: (String) -> Unit,
    onMarkDraftResolved: (String) -> Unit,
    onReopenDraft: (String) -> Unit
) {
    var showQueueActions by rememberSaveable(item.draftId) { mutableStateOf(false) }
    OperationalSectionCard(
        title = "${item.siteCode} • ${item.siteName}",
        subtitle = item.title
    ) {
        val urgencySeverity = when (item.urgencyClass) {
            ReviewerUrgencyClass.ACT_NOW -> OperationalSeverity.CRITICAL
            ReviewerUrgencyClass.HIGH -> OperationalSeverity.WARNING
            ReviewerUrgencyClass.WATCH -> OperationalSeverity.NORMAL
            ReviewerUrgencyClass.NORMAL -> OperationalSeverity.SUCCESS
        }
        OperationalSignalRow(
            signals = buildList {
                add(
                    OperationalSignal(
                        text = stringResource(
                            R.string.reviewer_control_tower_urgency_short,
                            stringResource(urgencyClassLabelRes(item.urgencyClass)),
                            stringResource(ageBucketLabelRes(item.ageBucket))
                        ),
                        severity = urgencySeverity
                    )
                )
                add(
                    OperationalSignal(
                        text = stringResource(
                            R.string.reviewer_control_tower_queue_status_short,
                            stringResource(queueStatusLabelRes(item.queueStatus))
                        )
                    )
                )
                item.originWorkflowType?.let { workflow ->
                    add(
                        OperationalSignal(
                            text = stringResource(
                                R.string.reviewer_control_tower_workflow_short,
                                stringResource(workflowLabelRes(workflow))
                            )
                        )
                    )
                }
            }
        )
        Text(
            text = stringResource(R.string.label_updated_at, formatControlTowerEpoch(item.updatedAtEpochMillis)),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(
                R.string.reviewer_control_tower_urgency_reason_short,
                stringResource(urgencyReasonLabelRes(item.urgencyReason))
            ),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(
                R.string.label_sync_state,
                stringResource(syncStateLabelRes(item.syncTrace.state))
            ),
            style = MaterialTheme.typography.bodySmall
        )
        item.dominantAttentionSignal?.let { dominant ->
            Text(
                text = stringResource(
                    R.string.reviewer_control_tower_dominant_signal,
                    stringResource(attentionSignalLabelRes(dominant))
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (item.attentionSignals.isNotEmpty()) {
            OperationalSignalRow(
                signals = item.attentionSignals
                    .toList()
                    .sortedByDescending(::attentionSignalSeverity)
                    .take(3)
                    .map { signal ->
                        OperationalSignal(
                            text = stringResource(attentionSignalLabelRes(signal)),
                            severity = when (signal) {
                                ReviewerAttentionSignal.SYNC_FAILED -> OperationalSeverity.CRITICAL
                                ReviewerAttentionSignal.QOS_FAILED_OR_BLOCKED -> OperationalSeverity.CRITICAL
                                ReviewerAttentionSignal.QOS_PREREQUISITES_NOT_READY -> OperationalSeverity.WARNING
                                ReviewerAttentionSignal.STALE_DRAFT -> OperationalSeverity.WARNING
                                ReviewerAttentionSignal.SYNC_PENDING -> OperationalSeverity.NORMAL
                            }
                        )
                    }
            )
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onOpenDraft(item.draftId) }
        ) {
            Text(stringResource(R.string.action_open_draft))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { onOpenSite(item.siteId) }
            ) {
                Text(stringResource(R.string.reviewer_control_tower_action_open_site))
            }
            if (item.syncTrace.state == ReportSyncState.FAILED) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !isRetrying,
                    onClick = { onRetryDraftSync(item.draftId) }
                ) {
                    Text(
                        if (isRetrying) {
                            stringResource(R.string.action_retry_sync_loading)
                        } else {
                            stringResource(R.string.action_retry_sync)
                        }
                    )
                }
            }
        }

        AdvancedDisclosureButton(
            expanded = showQueueActions,
            onToggle = { showQueueActions = !showQueueActions },
            showLabel = stringResource(R.string.reviewer_control_tower_action_show_queue_actions),
            hideLabel = stringResource(R.string.reviewer_control_tower_action_hide_queue_actions)
        )

        if (showQueueActions) {
            when (item.queueStatus) {
                SupervisorQueueStatus.UNTRIAGED -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = !isQueueTransitioning,
                            onClick = { onMarkDraftInReview(item.draftId) }
                        ) {
                            Text(stringResource(R.string.reviewer_control_tower_action_mark_in_review))
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = !isQueueTransitioning,
                            onClick = { onMarkDraftResolved(item.draftId) }
                        ) {
                            Text(stringResource(R.string.reviewer_control_tower_action_mark_resolved))
                        }
                    }
                }

                SupervisorQueueStatus.IN_REVIEW -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = !isQueueTransitioning,
                            onClick = { onMarkDraftWaitingFeedback(item.draftId) }
                        ) {
                            Text(stringResource(R.string.reviewer_control_tower_action_mark_waiting_feedback))
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = !isQueueTransitioning,
                            onClick = { onMarkDraftResolved(item.draftId) }
                        ) {
                            Text(stringResource(R.string.reviewer_control_tower_action_mark_resolved))
                        }
                    }
                }

                SupervisorQueueStatus.WAITING_FIELD_FEEDBACK -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = !isQueueTransitioning,
                            onClick = { onMarkDraftInReview(item.draftId) }
                        ) {
                            Text(stringResource(R.string.reviewer_control_tower_action_mark_in_review))
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = !isQueueTransitioning,
                            onClick = { onMarkDraftResolved(item.draftId) }
                        ) {
                            Text(stringResource(R.string.reviewer_control_tower_action_mark_resolved))
                        }
                    }
                }

                SupervisorQueueStatus.RESOLVED -> {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isQueueTransitioning,
                        onClick = { onReopenDraft(item.draftId) }
                    ) {
                        Text(stringResource(R.string.reviewer_control_tower_action_reopen))
                    }
                }
            }
        }
    }
}

@StringRes
internal fun groupTitleRes(key: ReviewerControlTowerGroupKey): Int {
    return when (key) {
        ReviewerControlTowerGroupKey.SYNC_FAILED -> R.string.reviewer_control_tower_group_sync_failed
        ReviewerControlTowerGroupKey.QOS_RISK -> R.string.reviewer_control_tower_group_qos_risk
        ReviewerControlTowerGroupKey.SYNC_PENDING -> R.string.reviewer_control_tower_group_sync_pending
        ReviewerControlTowerGroupKey.STALE -> R.string.reviewer_control_tower_group_stale
        ReviewerControlTowerGroupKey.NO_ATTENTION -> R.string.reviewer_control_tower_group_no_attention
        ReviewerControlTowerGroupKey.WORKFLOW_XFEEDER -> R.string.reviewer_control_tower_group_workflow_xfeeder
        ReviewerControlTowerGroupKey.WORKFLOW_RET -> R.string.reviewer_control_tower_group_workflow_ret
        ReviewerControlTowerGroupKey.WORKFLOW_PERFORMANCE -> R.string.reviewer_control_tower_group_workflow_performance
        ReviewerControlTowerGroupKey.WORKFLOW_NON_GUIDED -> R.string.reviewer_control_tower_group_workflow_non_guided
    }
}

@StringRes
internal fun attentionSignalLabelRes(signal: ReviewerAttentionSignal): Int {
    return when (signal) {
        ReviewerAttentionSignal.SYNC_FAILED -> R.string.reviewer_control_tower_signal_sync_failed
        ReviewerAttentionSignal.SYNC_PENDING -> R.string.reviewer_control_tower_signal_sync_pending
        ReviewerAttentionSignal.QOS_FAILED_OR_BLOCKED -> R.string.reviewer_control_tower_signal_qos_failed_or_blocked
        ReviewerAttentionSignal.QOS_PREREQUISITES_NOT_READY -> R.string.reviewer_control_tower_signal_qos_preflight_blocked
        ReviewerAttentionSignal.STALE_DRAFT -> R.string.reviewer_control_tower_signal_stale_draft
    }
}

internal fun attentionSignalSeverity(signal: ReviewerAttentionSignal): Int {
    return when (signal) {
        ReviewerAttentionSignal.SYNC_FAILED -> 5
        ReviewerAttentionSignal.QOS_FAILED_OR_BLOCKED -> 4
        ReviewerAttentionSignal.QOS_PREREQUISITES_NOT_READY -> 3
        ReviewerAttentionSignal.SYNC_PENDING -> 2
        ReviewerAttentionSignal.STALE_DRAFT -> 1
    }
}

@StringRes
internal fun urgencyClassLabelRes(urgencyClass: ReviewerUrgencyClass): Int {
    return when (urgencyClass) {
        ReviewerUrgencyClass.ACT_NOW -> R.string.reviewer_control_tower_urgency_act_now
        ReviewerUrgencyClass.HIGH -> R.string.reviewer_control_tower_urgency_high
        ReviewerUrgencyClass.WATCH -> R.string.reviewer_control_tower_urgency_watch
        ReviewerUrgencyClass.NORMAL -> R.string.reviewer_control_tower_urgency_normal
    }
}

@StringRes
internal fun ageBucketLabelRes(ageBucket: ReviewerDraftAgeBucket): Int {
    return when (ageBucket) {
        ReviewerDraftAgeBucket.FRESH -> R.string.reviewer_control_tower_age_fresh
        ReviewerDraftAgeBucket.AGING -> R.string.reviewer_control_tower_age_aging
        ReviewerDraftAgeBucket.STALE -> R.string.reviewer_control_tower_age_stale
        ReviewerDraftAgeBucket.OVERDUE -> R.string.reviewer_control_tower_age_overdue
    }
}

@StringRes
internal fun urgencyReasonLabelRes(reason: ReviewerUrgencyReason): Int {
    return when (reason) {
        ReviewerUrgencyReason.SYNC_FAILED -> R.string.reviewer_control_tower_urgency_reason_sync_failed
        ReviewerUrgencyReason.QOS_FAILED_OR_BLOCKED -> R.string.reviewer_control_tower_urgency_reason_qos_failed
        ReviewerUrgencyReason.QOS_PREREQUISITES_NOT_READY -> R.string.reviewer_control_tower_urgency_reason_qos_preflight
        ReviewerUrgencyReason.STALE_DRAFT -> R.string.reviewer_control_tower_urgency_reason_stale_draft
        ReviewerUrgencyReason.STALE_GUIDED_WORK -> R.string.reviewer_control_tower_urgency_reason_stale_guided
        ReviewerUrgencyReason.STALE_PENDING_SYNC -> R.string.reviewer_control_tower_urgency_reason_stale_pending
        ReviewerUrgencyReason.NONE -> R.string.reviewer_control_tower_urgency_reason_none
    }
}

@StringRes
internal fun queueStatusLabelRes(status: SupervisorQueueStatus): Int {
    return when (status) {
        SupervisorQueueStatus.UNTRIAGED -> R.string.reviewer_control_tower_queue_status_untriaged
        SupervisorQueueStatus.IN_REVIEW -> R.string.reviewer_control_tower_queue_status_in_review
        SupervisorQueueStatus.WAITING_FIELD_FEEDBACK -> R.string.reviewer_control_tower_queue_status_waiting_feedback
        SupervisorQueueStatus.RESOLVED -> R.string.reviewer_control_tower_queue_status_resolved
    }
}

@StringRes
internal fun queueActionLabelRes(actionType: SupervisorQueueActionType): Int {
    return when (actionType) {
        SupervisorQueueActionType.MARK_IN_REVIEW -> R.string.reviewer_control_tower_action_mark_in_review
        SupervisorQueueActionType.BULK_MARK_IN_REVIEW -> R.string.reviewer_control_tower_action_bulk_mark_in_review
        SupervisorQueueActionType.MARK_WAITING_FIELD_FEEDBACK -> R.string.reviewer_control_tower_action_mark_waiting_feedback
        SupervisorQueueActionType.MARK_RESOLVED -> R.string.reviewer_control_tower_action_mark_resolved
        SupervisorQueueActionType.REOPEN_TO_UNTRIAGED -> R.string.reviewer_control_tower_action_reopen
        SupervisorQueueActionType.RETRY_SYNC -> R.string.action_retry_sync
    }
}

@StringRes
internal fun workflowLabelRes(workflowType: ReportDraftOriginWorkflowType?): Int {
    return when (workflowType) {
        ReportDraftOriginWorkflowType.XFEEDER -> R.string.reviewer_control_tower_workflow_xfeeder
        ReportDraftOriginWorkflowType.RET -> R.string.reviewer_control_tower_workflow_ret
        ReportDraftOriginWorkflowType.PERFORMANCE -> R.string.reviewer_control_tower_workflow_performance
        null -> R.string.reviewer_control_tower_workflow_non_guided
    }
}

private val controlTowerDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

internal fun formatControlTowerEpoch(epochMillis: Long): String {
    return controlTowerDateFormatter.format(
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    )
}
