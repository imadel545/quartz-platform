package com.quartz.platform.domain.model

data class XfeederGuidedSession(
    val id: String,
    val siteId: String,
    val sectorId: String,
    val sectorCode: String,
    val status: XfeederSessionStatus,
    val sectorOutcome: XfeederSectorOutcome,
    val closureEvidence: XfeederClosureEvidence,
    val notes: String,
    val resultSummary: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val completedAtEpochMillis: Long?,
    val steps: List<XfeederGuidedStep>
)

data class XfeederGuidedStep(
    val code: XfeederStepCode,
    val required: Boolean,
    val status: XfeederStepStatus
)

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

enum class XfeederSessionStatus {
    CREATED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

enum class XfeederStepStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    BLOCKED
}

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
