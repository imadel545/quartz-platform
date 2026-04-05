package com.quartz.platform.presentation.performance.session

import com.quartz.platform.R
import com.quartz.platform.domain.model.NetworkStatus
import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.model.QosCompletionIssue
import com.quartz.platform.domain.model.QosExecutionEventType
import com.quartz.platform.domain.model.QosExecutionIssueCode
import com.quartz.platform.domain.model.QosExecutionTimelineEvent
import com.quartz.platform.domain.model.QosFamilyExecutionResult
import com.quartz.platform.domain.model.QosFamilyExecutionStatus
import com.quartz.platform.domain.model.QosPreflightIssue
import com.quartz.platform.domain.model.QosRunSummary
import com.quartz.platform.domain.model.QosRunnerAction
import com.quartz.platform.domain.model.QosTestFamily
import com.quartz.platform.domain.model.deriveQosExecutionSnapshot
import com.quartz.platform.domain.model.deriveQosPassFailCounters
import com.quartz.platform.domain.model.assessQosCompletion
import com.quartz.platform.domain.model.assessQosFamilyPreflight
import com.quartz.platform.domain.model.computeQosFamilyRunCoverage
import com.quartz.platform.domain.model.computeQosRunPlan
import com.quartz.platform.domain.model.qosExecutionEventSortOrder

internal fun deriveQosExecutionAggregate(state: PerformanceSessionUiState): PerformanceSessionUiState {
    val counters = deriveQosPassFailCounters(
        qosSummaryForAssessment(state)
    )
    return state.copy(
        qosIterationCountInput = counters.iterationCount.toString(),
        qosSuccessCountInput = counters.successCount.toString(),
        qosFailureCountInput = counters.failureCount.toString()
    )
}

internal fun deriveQosUiState(state: PerformanceSessionUiState): PerformanceSessionUiState {
    val aggregated = deriveQosExecutionAggregate(state)
    val isQosWorkflow = aggregated.selectedSessionWorkflowType == PerformanceWorkflowType.QOS_SCRIPT
    if (!isQosWorkflow) {
        return aggregated.copy(
            qosCompletionIssues = emptySet(),
            qosFamilyRunCoverageByType = emptyMap(),
            qosRunPlan = emptyList(),
            qosExecutionSnapshot = null,
            qosPreflightIssuesByFamily = emptyMap()
        )
    }
    val summary = qosSummaryForAssessment(aggregated)
    val runPlan = computeQosRunPlan(summary)
    val executionSnapshot = deriveQosExecutionSnapshot(
        qosRunSummary = summary,
        preconditionsReady = aggregated.prerequisiteNetworkReady &&
            aggregated.prerequisiteBatterySufficient &&
            aggregated.prerequisiteLocationReady
    )
    val coverageByFamily = aggregated.qosSelectedTestFamilies.associateWith { family ->
        computeQosFamilyRunCoverage(summary, family)
    }
    val preflightByFamily = coverageByFamily.mapValues { (family, _) ->
        assessQosFamilyPreflight(
            qosRunSummary = summary,
            family = family,
            action = QosRunnerAction.START,
            prerequisiteNetworkReady = aggregated.prerequisiteNetworkReady,
            prerequisiteBatterySufficient = aggregated.prerequisiteBatterySufficient,
            prerequisiteLocationReady = aggregated.prerequisiteLocationReady,
            reasonCode = aggregated.qosFamilyReasonCodeByType[family],
            failureReason = aggregated.qosFamilyFailureReasonByType[family]
        )
    }
    return aggregated.copy(
        qosCompletionIssues = assessQosCompletion(summary).issues,
        qosFamilyRunCoverageByType = coverageByFamily,
        qosRunPlan = runPlan,
        qosExecutionSnapshot = executionSnapshot,
        qosPreflightIssuesByFamily = preflightByFamily
    )
}

internal fun qosSummaryForAssessment(state: PerformanceSessionUiState): QosRunSummary {
    val families = state.qosSelectedTestFamilies
    val familyResults = families.map { family ->
        val derivedStatus = resolveFamilyStatusFromTimeline(
            timelineEvents = state.qosExecutionTimelineEvents,
            family = family
        )
        QosFamilyExecutionResult(
            family = family,
            status = derivedStatus ?: state.qosFamilyStatusByType[family] ?: QosFamilyExecutionStatus.NOT_RUN,
            failureReasonCode = state.qosFamilyReasonCodeByType[family],
            failureReason = state.qosFamilyFailureReasonByType[family]
                ?.trim()
                ?.takeIf { value -> value.isNotBlank() }
        )
    }.sortedBy { result -> result.family.name }
    val counters = deriveQosPassFailCounters(
        QosRunSummary(
            selectedTestFamilies = families,
            familyExecutionResults = familyResults,
            executionTimelineEvents = state.qosExecutionTimelineEvents
        )
    )
    return QosRunSummary(
        scriptId = state.qosSelectedScriptId,
        scriptName = state.qosSelectedScriptName,
        configuredRepeatCount = state.qosConfiguredRepeatInput.toIntOrNull()?.coerceAtLeast(1) ?: 1,
        configuredTechnologies = state.qosConfiguredTechnologies,
        scriptSnapshotUpdatedAtEpochMillis = state.qosScriptSnapshotUpdatedAtEpochMillis,
        selectedTestFamilies = families,
        familyExecutionResults = familyResults,
        executionTimelineEvents = state.qosExecutionTimelineEvents,
        targetTechnology = state.qosTargetTechnologyInput.trim().ifBlank { null },
        targetPhoneNumber = state.qosTargetPhoneInput.trim().ifBlank { null },
        iterationCount = counters.iterationCount,
        successCount = counters.successCount,
        failureCount = counters.failureCount
    )
}

internal fun qosCompletionIssueToErrorRes(issue: QosCompletionIssue): Int {
    return when (issue) {
        QosCompletionIssue.SCRIPT_REFERENCE_MISSING,
        QosCompletionIssue.TEST_FAMILIES_MISSING,
        QosCompletionIssue.FAMILY_RESULT_INCOMPLETE -> R.string.error_performance_qos_script_required
        QosCompletionIssue.REPETITION_COVERAGE_INCOMPLETE -> R.string.error_performance_qos_repetition_coverage_required
        QosCompletionIssue.FAILURE_REASON_CODE_MISSING -> R.string.error_performance_qos_failed_reason_required
        QosCompletionIssue.PHONE_TARGET_MISSING -> R.string.error_performance_qos_phone_required
        QosCompletionIssue.TARGET_TECHNOLOGY_INVALID -> R.string.error_performance_qos_target_technology_required
        QosCompletionIssue.COUNTERS_INCONSISTENT -> R.string.error_performance_qos_result_inconsistent
    }
}

internal fun qosPreflightIssueToErrorRes(issue: QosPreflightIssue): Int {
    return when (issue) {
        QosPreflightIssue.NETWORK_NOT_READY -> R.string.error_performance_qos_issue_network_not_ready
        QosPreflightIssue.BATTERY_NOT_READY -> R.string.error_performance_qos_issue_battery_not_ready
        QosPreflightIssue.LOCATION_NOT_READY -> R.string.error_performance_qos_issue_location_not_ready
        QosPreflightIssue.SCRIPT_REFERENCE_MISSING -> R.string.error_performance_qos_script_required
        QosPreflightIssue.FAMILY_NOT_SELECTED -> R.string.error_performance_qos_script_required
        QosPreflightIssue.PHONE_TARGET_MISSING -> R.string.error_performance_qos_phone_required
        QosPreflightIssue.TARGET_TECHNOLOGY_INVALID -> R.string.error_performance_qos_target_technology_required
        QosPreflightIssue.REPETITION_ALREADY_STARTED -> R.string.error_performance_qos_repetition_already_started
        QosPreflightIssue.REPETITION_ALREADY_COMPLETED -> R.string.error_performance_qos_repetition_coverage_required
        QosPreflightIssue.ANOTHER_REPETITION_ACTIVE -> R.string.error_performance_qos_another_repetition_active
        QosPreflightIssue.REPETITION_NOT_STARTED -> R.string.error_performance_qos_repetition_not_started
        QosPreflightIssue.REPETITION_NOT_PAUSED -> R.string.error_performance_qos_repetition_not_paused
        QosPreflightIssue.FAILURE_REASON_CODE_REQUIRED -> R.string.error_performance_qos_failed_reason_required
    }
}

internal fun qosPreflightIssueToReasonCode(issue: QosPreflightIssue): QosExecutionIssueCode? {
    return when (issue) {
        QosPreflightIssue.NETWORK_NOT_READY -> QosExecutionIssueCode.NETWORK_UNAVAILABLE
        QosPreflightIssue.BATTERY_NOT_READY -> QosExecutionIssueCode.BATTERY_INSUFFICIENT
        QosPreflightIssue.LOCATION_NOT_READY -> QosExecutionIssueCode.LOCATION_UNAVAILABLE
        QosPreflightIssue.PHONE_TARGET_MISSING -> QosExecutionIssueCode.PHONE_TARGET_MISSING
        QosPreflightIssue.TARGET_TECHNOLOGY_INVALID -> QosExecutionIssueCode.TARGET_TECHNOLOGY_MISMATCH
        QosPreflightIssue.REPETITION_ALREADY_STARTED,
        QosPreflightIssue.REPETITION_ALREADY_COMPLETED,
        QosPreflightIssue.ANOTHER_REPETITION_ACTIVE,
        QosPreflightIssue.REPETITION_NOT_STARTED,
        QosPreflightIssue.REPETITION_NOT_PAUSED -> QosExecutionIssueCode.OPERATOR_ABORTED
        QosPreflightIssue.SCRIPT_REFERENCE_MISSING,
        QosPreflightIssue.FAMILY_NOT_SELECTED,
        QosPreflightIssue.FAILURE_REASON_CODE_REQUIRED -> null
    }
}

internal fun resolveFamilyStatusFromTimeline(
    timelineEvents: List<QosExecutionTimelineEvent>,
    family: QosTestFamily
): QosFamilyExecutionStatus? {
    val latestTerminal = timelineEvents
        .asSequence()
        .filter { event -> event.family == family }
        .filter { event ->
            event.eventType == QosExecutionEventType.PASSED ||
                event.eventType == QosExecutionEventType.FAILED ||
                event.eventType == QosExecutionEventType.BLOCKED
        }
        .maxWithOrNull(
            compareBy<QosExecutionTimelineEvent> { event ->
                if (event.checkpointSequence > 0) {
                    event.checkpointSequence
                } else {
                    Int.MAX_VALUE
                }
            }.thenBy { event -> event.occurredAtEpochMillis }
                .thenBy { event -> event.repetitionIndex }
                .thenBy { event -> qosExecutionEventSortOrder(event.eventType) }
        )
        ?: return null

    return when (latestTerminal.eventType) {
        QosExecutionEventType.PASSED -> QosFamilyExecutionStatus.PASSED
        QosExecutionEventType.FAILED -> QosFamilyExecutionStatus.FAILED
        QosExecutionEventType.BLOCKED -> QosFamilyExecutionStatus.BLOCKED
        QosExecutionEventType.STARTED,
        QosExecutionEventType.PAUSED,
        QosExecutionEventType.RESUMED -> null
    }
}

internal fun <T> Set<T>.toggle(item: T): Set<T> {
    return if (contains(item)) this - item else this + item
}

internal fun NetworkStatus?.isReady(): Boolean = this == NetworkStatus.AVAILABLE
