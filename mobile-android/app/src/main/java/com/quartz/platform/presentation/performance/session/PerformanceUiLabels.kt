package com.quartz.platform.presentation.performance.session

import androidx.annotation.StringRes
import com.quartz.platform.R
import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.PerformanceStepCode
import com.quartz.platform.domain.model.PerformanceStepStatus
import com.quartz.platform.domain.model.PerformanceWorkflowType

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
