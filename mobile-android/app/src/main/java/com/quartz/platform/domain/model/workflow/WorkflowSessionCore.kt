package com.quartz.platform.domain.model.workflow

data class WorkflowSessionIdentity(
    val sessionId: String,
    val siteId: String,
    val scopeId: String,
    val scopeCode: String
)

enum class WorkflowSessionStatus {
    CREATED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

enum class WorkflowStepStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    BLOCKED
}

data class WorkflowStepState<StepCode>(
    val code: StepCode,
    val required: Boolean,
    val status: WorkflowStepStatus
)

data class WorkflowCompletionGuard(
    val requiredStepCount: Int,
    val completedRequiredStepCount: Int
) {
    val canComplete: Boolean
        get() = missingRequiredStepCount == 0

    val missingRequiredStepCount: Int
        get() = (requiredStepCount - completedRequiredStepCount).coerceAtLeast(0)

    companion object {
        fun <StepCode> fromSteps(steps: List<WorkflowStepState<StepCode>>): WorkflowCompletionGuard {
            val required = steps.count { step -> step.required }
            val completedRequired = steps.count { step ->
                step.required && step.status == WorkflowStepStatus.DONE
            }
            return WorkflowCompletionGuard(
                requiredStepCount = required,
                completedRequiredStepCount = completedRequired
            )
        }

        fun fromRequiredStatuses(stepStatuses: List<Pair<Boolean, WorkflowStepStatus>>): WorkflowCompletionGuard {
            val required = stepStatuses.count { (required, _) -> required }
            val completedRequired = stepStatuses.count { (required, status) ->
                required && status == WorkflowStepStatus.DONE
            }
            return WorkflowCompletionGuard(
                requiredStepCount = required,
                completedRequiredStepCount = completedRequired
            )
        }
    }
}

data class WorkflowClosureSummary<Outcome>(
    val outcome: Outcome,
    val notes: String,
    val resultSummary: String
)
