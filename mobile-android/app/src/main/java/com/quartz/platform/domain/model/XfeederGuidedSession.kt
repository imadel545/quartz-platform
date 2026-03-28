package com.quartz.platform.domain.model

import com.quartz.platform.domain.model.workflow.WorkflowClosureSummary
import com.quartz.platform.domain.model.workflow.WorkflowCompletionGuard
import com.quartz.platform.domain.model.workflow.WorkflowSessionIdentity
import com.quartz.platform.domain.model.workflow.WorkflowSessionStatus
import com.quartz.platform.domain.model.workflow.WorkflowStepState
import com.quartz.platform.domain.model.workflow.WorkflowStepStatus

data class XfeederGuidedSession(
    val id: String,
    val siteId: String,
    val sectorId: String,
    val sectorCode: String,
    val measurementZoneRadiusMeters: Int,
    val measurementZoneExtensionReason: String,
    val proximityModeEnabled: Boolean,
    val status: XfeederSessionStatus,
    val sectorOutcome: XfeederSectorOutcome,
    val closureEvidence: XfeederClosureEvidence,
    val notes: String,
    val resultSummary: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val completedAtEpochMillis: Long?,
    val steps: List<XfeederGuidedStep>
) {
    val identity: WorkflowSessionIdentity
        get() = WorkflowSessionIdentity(
            sessionId = id,
            siteId = siteId,
            scopeId = sectorId,
            scopeCode = sectorCode
        )

    val closureSummary: WorkflowClosureSummary<XfeederSectorOutcome>
        get() = WorkflowClosureSummary(
            outcome = sectorOutcome,
            notes = notes,
            resultSummary = resultSummary
        )

    fun completionGuard(): WorkflowCompletionGuard = WorkflowCompletionGuard.fromSteps(steps)
}

typealias XfeederGuidedStep = WorkflowStepState<XfeederStepCode>

data class XfeederClosureEvidence(
    val relatedSectorCode: String,
    val unreliableReason: XfeederUnreliableReason?,
    val observedSectorCount: Int?
)

enum class XfeederUnreliableReason {
    NO_MAJORITY_SECTOR,
    UNSTABLE_SECTOR_SWITCHING
}

enum class XfeederClosureEvidenceIssue {
    RELATED_SECTOR_REQUIRED,
    UNRELIABLE_REASON_REQUIRED,
    OBSERVED_SECTOR_COUNT_INVALID
}

typealias XfeederSessionStatus = WorkflowSessionStatus
typealias XfeederStepStatus = WorkflowStepStatus

enum class XfeederSectorOutcome {
    NOT_TESTED,
    WAITING_NETWORK,
    OK,
    CROSSED,
    MIXFEEDER,
    UNRELIABLE
}

enum class XfeederStepCode {
    PRECONDITION_NETWORK_READY,
    PRECONDITION_MEASUREMENT_ZONE_READY,
    OBSERVE_CONNECTED_CELLS,
    CHECK_SECTOR_CROSSING,
    CHECK_MIXFEEDER_ALTERNANCE,
    FINALIZE_SECTOR_SUMMARY
}

fun validateClosureEvidenceForFinalization(
    outcome: XfeederSectorOutcome,
    evidence: XfeederClosureEvidence
): XfeederClosureEvidenceIssue? {
    return when (outcome) {
        XfeederSectorOutcome.CROSSED,
        XfeederSectorOutcome.MIXFEEDER -> {
            if (evidence.relatedSectorCode.isBlank()) {
                XfeederClosureEvidenceIssue.RELATED_SECTOR_REQUIRED
            } else {
                null
            }
        }

        XfeederSectorOutcome.UNRELIABLE -> {
            when {
                evidence.unreliableReason == null -> {
                    XfeederClosureEvidenceIssue.UNRELIABLE_REASON_REQUIRED
                }

                evidence.observedSectorCount == null || evidence.observedSectorCount < 2 -> {
                    XfeederClosureEvidenceIssue.OBSERVED_SECTOR_COUNT_INVALID
                }

                else -> null
            }
        }

        else -> null
    }
}
