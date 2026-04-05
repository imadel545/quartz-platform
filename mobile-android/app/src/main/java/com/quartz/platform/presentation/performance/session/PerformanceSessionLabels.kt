package com.quartz.platform.presentation.performance.session

import androidx.annotation.StringRes
import com.quartz.platform.R
import com.quartz.platform.domain.model.NetworkStatus
import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.PerformanceStepCode
import com.quartz.platform.domain.model.PerformanceStepStatus
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.model.QosCompletionIssue
import com.quartz.platform.domain.model.QosExecutionEngineState
import com.quartz.platform.domain.model.QosExecutionEventType
import com.quartz.platform.domain.model.QosExecutionIssueCode
import com.quartz.platform.domain.model.QosFamilyExecutionStatus
import com.quartz.platform.domain.model.QosPreflightIssue
import com.quartz.platform.domain.model.QosRecoveryState
import com.quartz.platform.domain.model.QosRunPlanItemStatus
import com.quartz.platform.domain.model.QosTestFamily
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val performanceDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun formatPerformanceEpoch(epochMillis: Long): String {
    return performanceDateFormatter.format(
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    )
}

@StringRes
fun performanceWorkflowTypeLabelRes(workflowType: PerformanceWorkflowType): Int {
    return when (workflowType) {
        PerformanceWorkflowType.THROUGHPUT -> R.string.performance_workflow_throughput
        PerformanceWorkflowType.QOS_SCRIPT -> R.string.performance_workflow_qos
    }
}

@StringRes
fun performanceSessionStatusLabelRes(status: PerformanceSessionStatus): Int {
    return when (status) {
        PerformanceSessionStatus.CREATED -> R.string.performance_status_created
        PerformanceSessionStatus.IN_PROGRESS -> R.string.performance_status_in_progress
        PerformanceSessionStatus.COMPLETED -> R.string.performance_status_completed
        PerformanceSessionStatus.CANCELLED -> R.string.performance_status_cancelled
    }
}

@StringRes
fun performanceStepCodeLabelRes(code: PerformanceStepCode): Int {
    return when (code) {
        PerformanceStepCode.PRECONDITIONS_CHECK -> R.string.performance_step_preconditions
        PerformanceStepCode.EXECUTE_TEST -> R.string.performance_step_execute_test
        PerformanceStepCode.REVIEW_RESULT -> R.string.performance_step_review_result
        PerformanceStepCode.SEND_RESULT -> R.string.performance_step_send_result
    }
}

@StringRes
fun performanceStepStatusLabelRes(status: PerformanceStepStatus): Int {
    return when (status) {
        PerformanceStepStatus.TODO -> R.string.performance_step_status_todo
        PerformanceStepStatus.IN_PROGRESS -> R.string.performance_step_status_in_progress
        PerformanceStepStatus.DONE -> R.string.performance_step_status_done
        PerformanceStepStatus.BLOCKED -> R.string.performance_step_status_blocked
    }
}

@StringRes
fun qosTestFamilyLabelRes(family: QosTestFamily): Int {
    return when (family) {
        QosTestFamily.THROUGHPUT_LATENCY -> R.string.qos_test_family_throughput_latency
        QosTestFamily.VIDEO_STREAMING -> R.string.qos_test_family_video_streaming
        QosTestFamily.SMS -> R.string.qos_test_family_sms
        QosTestFamily.VOLTE_CALL -> R.string.qos_test_family_volte_call
        QosTestFamily.CSFB_CALL -> R.string.qos_test_family_csfb_call
        QosTestFamily.EMERGENCY_CALL -> R.string.qos_test_family_emergency_call
        QosTestFamily.STANDARD_CALL -> R.string.qos_test_family_standard_call
    }
}

@StringRes
fun qosFamilyExecutionStatusLabelRes(status: QosFamilyExecutionStatus): Int {
    return when (status) {
        QosFamilyExecutionStatus.NOT_RUN -> R.string.performance_qos_family_status_not_run
        QosFamilyExecutionStatus.PASSED -> R.string.performance_qos_family_status_passed
        QosFamilyExecutionStatus.FAILED -> R.string.performance_qos_family_status_failed
        QosFamilyExecutionStatus.BLOCKED -> R.string.performance_qos_family_status_blocked
    }
}

@StringRes
fun qosExecutionEventTypeLabelRes(eventType: QosExecutionEventType): Int {
    return when (eventType) {
        QosExecutionEventType.STARTED -> R.string.performance_qos_event_started
        QosExecutionEventType.PAUSED -> R.string.performance_qos_event_paused
        QosExecutionEventType.RESUMED -> R.string.performance_qos_event_resumed
        QosExecutionEventType.PASSED -> R.string.performance_qos_family_status_passed
        QosExecutionEventType.FAILED -> R.string.performance_qos_family_status_failed
        QosExecutionEventType.BLOCKED -> R.string.performance_qos_family_status_blocked
    }
}

@StringRes
fun qosIssueCodeLabelRes(code: QosExecutionIssueCode): Int {
    return when (code) {
        QosExecutionIssueCode.PREREQUISITE_NOT_READY -> R.string.qos_issue_code_prerequisite_not_ready
        QosExecutionIssueCode.BATTERY_INSUFFICIENT -> R.string.qos_issue_code_battery_insufficient
        QosExecutionIssueCode.LOCATION_UNAVAILABLE -> R.string.qos_issue_code_location_unavailable
        QosExecutionIssueCode.TARGET_TECHNOLOGY_MISMATCH -> R.string.qos_issue_code_target_technology_mismatch
        QosExecutionIssueCode.PHONE_TARGET_MISSING -> R.string.qos_issue_code_phone_target_missing
        QosExecutionIssueCode.NETWORK_UNAVAILABLE -> R.string.qos_issue_code_network_unavailable
        QosExecutionIssueCode.THRESHOLD_NOT_MET -> R.string.qos_issue_code_threshold_not_met
        QosExecutionIssueCode.OPERATOR_ABORTED -> R.string.qos_issue_code_operator_aborted
        QosExecutionIssueCode.UNKNOWN -> R.string.qos_issue_code_unknown
    }
}

@StringRes
fun qosIssueCodeActionRes(code: QosExecutionIssueCode): Int {
    return when (code) {
        QosExecutionIssueCode.PREREQUISITE_NOT_READY -> R.string.qos_issue_action_prerequisite_not_ready
        QosExecutionIssueCode.BATTERY_INSUFFICIENT -> R.string.qos_issue_action_battery_insufficient
        QosExecutionIssueCode.LOCATION_UNAVAILABLE -> R.string.qos_issue_action_location_unavailable
        QosExecutionIssueCode.TARGET_TECHNOLOGY_MISMATCH -> R.string.qos_issue_action_target_technology_mismatch
        QosExecutionIssueCode.PHONE_TARGET_MISSING -> R.string.qos_issue_action_phone_target_missing
        QosExecutionIssueCode.NETWORK_UNAVAILABLE -> R.string.qos_issue_action_network_unavailable
        QosExecutionIssueCode.THRESHOLD_NOT_MET -> R.string.qos_issue_action_threshold_not_met
        QosExecutionIssueCode.OPERATOR_ABORTED -> R.string.qos_issue_action_operator_aborted
        QosExecutionIssueCode.UNKNOWN -> R.string.qos_issue_action_unknown
    }
}

fun qosReasonOptionsForFamily(family: QosTestFamily): List<QosExecutionIssueCode> {
    return when (family) {
        QosTestFamily.THROUGHPUT_LATENCY,
        QosTestFamily.VIDEO_STREAMING -> listOf(
            QosExecutionIssueCode.NETWORK_UNAVAILABLE,
            QosExecutionIssueCode.BATTERY_INSUFFICIENT,
            QosExecutionIssueCode.LOCATION_UNAVAILABLE,
            QosExecutionIssueCode.THRESHOLD_NOT_MET,
            QosExecutionIssueCode.OPERATOR_ABORTED,
            QosExecutionIssueCode.UNKNOWN
        )

        QosTestFamily.SMS,
        QosTestFamily.VOLTE_CALL,
        QosTestFamily.CSFB_CALL,
        QosTestFamily.EMERGENCY_CALL,
        QosTestFamily.STANDARD_CALL -> listOf(
            QosExecutionIssueCode.PHONE_TARGET_MISSING,
            QosExecutionIssueCode.NETWORK_UNAVAILABLE,
            QosExecutionIssueCode.BATTERY_INSUFFICIENT,
            QosExecutionIssueCode.LOCATION_UNAVAILABLE,
            QosExecutionIssueCode.OPERATOR_ABORTED,
            QosExecutionIssueCode.UNKNOWN
        )
    }
}

@StringRes
fun qosCompletionIssueLabelRes(issue: QosCompletionIssue): Int {
    return when (issue) {
        QosCompletionIssue.SCRIPT_REFERENCE_MISSING -> R.string.error_performance_qos_issue_script_reference_missing
        QosCompletionIssue.TEST_FAMILIES_MISSING -> R.string.error_performance_qos_issue_test_families_missing
        QosCompletionIssue.FAMILY_RESULT_INCOMPLETE -> R.string.error_performance_qos_issue_family_result_incomplete
        QosCompletionIssue.REPETITION_COVERAGE_INCOMPLETE -> R.string.error_performance_qos_issue_repetition_coverage_incomplete
        QosCompletionIssue.FAILURE_REASON_CODE_MISSING -> R.string.error_performance_qos_issue_failure_reason_missing
        QosCompletionIssue.PHONE_TARGET_MISSING -> R.string.error_performance_qos_issue_phone_target_missing
        QosCompletionIssue.TARGET_TECHNOLOGY_INVALID -> R.string.error_performance_qos_issue_target_technology_invalid
        QosCompletionIssue.COUNTERS_INCONSISTENT -> R.string.error_performance_qos_issue_counters_inconsistent
    }
}

@StringRes
fun qosPreflightIssueLabelRes(issue: QosPreflightIssue): Int {
    return when (issue) {
        QosPreflightIssue.NETWORK_NOT_READY -> R.string.error_performance_qos_issue_network_not_ready
        QosPreflightIssue.BATTERY_NOT_READY -> R.string.error_performance_qos_issue_battery_not_ready
        QosPreflightIssue.LOCATION_NOT_READY -> R.string.error_performance_qos_issue_location_not_ready
        QosPreflightIssue.SCRIPT_REFERENCE_MISSING -> R.string.error_performance_qos_issue_script_reference_missing
        QosPreflightIssue.FAMILY_NOT_SELECTED -> R.string.error_performance_qos_issue_family_not_selected
        QosPreflightIssue.PHONE_TARGET_MISSING -> R.string.error_performance_qos_issue_phone_target_missing
        QosPreflightIssue.TARGET_TECHNOLOGY_INVALID -> R.string.error_performance_qos_issue_target_technology_invalid
        QosPreflightIssue.REPETITION_ALREADY_STARTED -> R.string.error_performance_qos_issue_repetition_already_started
        QosPreflightIssue.REPETITION_ALREADY_COMPLETED -> R.string.error_performance_qos_issue_repetition_coverage_incomplete
        QosPreflightIssue.ANOTHER_REPETITION_ACTIVE -> R.string.error_performance_qos_issue_another_repetition_active
        QosPreflightIssue.REPETITION_NOT_STARTED -> R.string.error_performance_qos_issue_repetition_not_started
        QosPreflightIssue.REPETITION_NOT_PAUSED -> R.string.error_performance_qos_issue_repetition_not_paused
        QosPreflightIssue.FAILURE_REASON_CODE_REQUIRED -> R.string.error_performance_qos_issue_failure_reason_missing
    }
}

@StringRes
fun networkStatusLabelRes(status: NetworkStatus?): Int {
    return when (status) {
        NetworkStatus.AVAILABLE -> R.string.performance_device_network_available
        NetworkStatus.UNAVAILABLE -> R.string.performance_device_network_unavailable
        null -> R.string.value_not_available
    }
}

@StringRes
fun qosExecutionEngineStateLabelRes(state: QosExecutionEngineState): Int {
    return when (state) {
        QosExecutionEngineState.READY -> R.string.performance_qos_engine_state_ready
        QosExecutionEngineState.PREFLIGHT_BLOCKED -> R.string.performance_qos_engine_state_preflight_blocked
        QosExecutionEngineState.RUNNING -> R.string.performance_qos_engine_state_running
        QosExecutionEngineState.PAUSED -> R.string.performance_qos_engine_state_paused
        QosExecutionEngineState.RESUMED -> R.string.performance_qos_engine_state_resumed
        QosExecutionEngineState.COMPLETED -> R.string.performance_qos_engine_state_completed
        QosExecutionEngineState.FAILED -> R.string.performance_qos_engine_state_failed
        QosExecutionEngineState.BLOCKED -> R.string.performance_qos_engine_state_blocked
    }
}

@StringRes
fun qosRecoveryStateLabelRes(state: QosRecoveryState): Int {
    return when (state) {
        QosRecoveryState.NONE -> R.string.performance_qos_recovery_state_none
        QosRecoveryState.RESUME_AVAILABLE -> R.string.performance_qos_recovery_state_resume_available
        QosRecoveryState.INVARIANT_BROKEN -> R.string.performance_qos_recovery_state_invariant_broken
    }
}

@StringRes
fun qosRunPlanStatusLabelRes(status: QosRunPlanItemStatus): Int {
    return when (status) {
        QosRunPlanItemStatus.PENDING -> R.string.performance_qos_run_plan_status_pending
        QosRunPlanItemStatus.RUNNING -> R.string.performance_qos_run_plan_status_running
        QosRunPlanItemStatus.PAUSED -> R.string.performance_qos_run_plan_status_paused
        QosRunPlanItemStatus.PASSED -> R.string.performance_qos_run_plan_status_passed
        QosRunPlanItemStatus.FAILED -> R.string.performance_qos_run_plan_status_failed
        QosRunPlanItemStatus.BLOCKED -> R.string.performance_qos_run_plan_status_blocked
    }
}
