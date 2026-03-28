package com.quartz.platform.domain.model

import com.quartz.platform.domain.model.workflow.WorkflowClosureSummary
import com.quartz.platform.domain.model.workflow.WorkflowCompletionGuard
import com.quartz.platform.domain.model.workflow.WorkflowSessionIdentity
import com.quartz.platform.domain.model.workflow.WorkflowSessionStatus
import com.quartz.platform.domain.model.workflow.WorkflowStepState
import com.quartz.platform.domain.model.workflow.WorkflowStepStatus

data class RetGuidedSession(
    val id: String,
    val siteId: String,
    val sectorId: String,
    val sectorCode: String,
    val status: RetSessionStatus,
    val resultOutcome: RetResultOutcome,
    val notes: String,
    val resultSummary: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val completedAtEpochMillis: Long?,
    val steps: List<RetGuidedStep>
) {
    val identity: WorkflowSessionIdentity
        get() = WorkflowSessionIdentity(
            sessionId = id,
            siteId = siteId,
            scopeId = sectorId,
            scopeCode = sectorCode
        )

    val closureSummary: WorkflowClosureSummary<RetResultOutcome>
        get() = WorkflowClosureSummary(
            outcome = resultOutcome,
            notes = notes,
            resultSummary = resultSummary
        )

    fun completionGuard(): WorkflowCompletionGuard = WorkflowCompletionGuard.fromSteps(steps)
}

typealias RetSessionStatus = WorkflowSessionStatus
typealias RetStepStatus = WorkflowStepStatus
typealias RetGuidedStep = WorkflowStepState<RetStepCode>

enum class RetResultOutcome {
    NOT_RUN,
    PASS,
    FAIL,
    INCONCLUSIVE
}

enum class RetStepCode {
    CALIBRATION_PRECHECK,
    VALIDATION_CAPTURE,
    RESTORE_TILT_AND_RESULT
}
