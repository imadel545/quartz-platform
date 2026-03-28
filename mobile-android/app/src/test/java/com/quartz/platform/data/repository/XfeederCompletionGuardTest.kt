package com.quartz.platform.data.repository

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.data.local.entity.XfeederStepEntity
import com.quartz.platform.domain.model.XfeederClosureEvidence
import com.quartz.platform.domain.model.XfeederClosureEvidenceIssue
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederStepCode
import com.quartz.platform.domain.model.XfeederStepStatus
import com.quartz.platform.domain.model.XfeederUnreliableReason
import com.quartz.platform.domain.model.validateClosureEvidenceForFinalization
import org.junit.Test

class XfeederCompletionGuardTest {

    @Test
    fun `hasIncompleteRequiredSteps returns true when required step is not done`() {
        val steps = listOf(
            XfeederStepEntity(
                sessionId = "s1",
                code = XfeederStepCode.PRECONDITION_NETWORK_READY.name,
                required = true,
                status = XfeederStepStatus.IN_PROGRESS.name,
                displayOrder = 0
            ),
            XfeederStepEntity(
                sessionId = "s1",
                code = XfeederStepCode.FINALIZE_SECTOR_SUMMARY.name,
                required = false,
                status = XfeederStepStatus.TODO.name,
                displayOrder = 1
            )
        )

        val guard = buildCompletionGuard(steps)

        assertThat(guard.requiredStepCount).isEqualTo(1)
        assertThat(guard.completedRequiredStepCount).isEqualTo(0)
        assertThat(guard.missingRequiredStepCount).isEqualTo(1)
        assertThat(guard.canComplete).isFalse()
        assertThat(hasIncompleteRequiredSteps(steps)).isTrue()
    }

    @Test
    fun `hasIncompleteRequiredSteps returns false when all required steps are done`() {
        val steps = listOf(
            XfeederStepEntity(
                sessionId = "s1",
                code = XfeederStepCode.PRECONDITION_NETWORK_READY.name,
                required = true,
                status = XfeederStepStatus.DONE.name,
                displayOrder = 0
            ),
            XfeederStepEntity(
                sessionId = "s1",
                code = XfeederStepCode.CHECK_SECTOR_CROSSING.name,
                required = true,
                status = XfeederStepStatus.DONE.name,
                displayOrder = 1
            )
        )

        val guard = buildCompletionGuard(steps)

        assertThat(guard.requiredStepCount).isEqualTo(2)
        assertThat(guard.completedRequiredStepCount).isEqualTo(2)
        assertThat(guard.missingRequiredStepCount).isEqualTo(0)
        assertThat(guard.canComplete).isTrue()
        assertThat(hasIncompleteRequiredSteps(steps)).isFalse()
    }

    @Test
    fun `validateClosureEvidenceForFinalization requires related sector for crossed`() {
        val issue = validateClosureEvidenceForFinalization(
            outcome = XfeederSectorOutcome.CROSSED,
            evidence = XfeederClosureEvidence(
                relatedSectorCode = "",
                unreliableReason = null,
                observedSectorCount = null
            )
        )

        assertThat(issue).isEqualTo(XfeederClosureEvidenceIssue.RELATED_SECTOR_REQUIRED)
    }

    @Test
    fun `validateClosureEvidenceForFinalization requires reason and sector count for unreliable`() {
        val missingReason = validateClosureEvidenceForFinalization(
            outcome = XfeederSectorOutcome.UNRELIABLE,
            evidence = XfeederClosureEvidence(
                relatedSectorCode = "",
                unreliableReason = null,
                observedSectorCount = 3
            )
        )
        val invalidCount = validateClosureEvidenceForFinalization(
            outcome = XfeederSectorOutcome.UNRELIABLE,
            evidence = XfeederClosureEvidence(
                relatedSectorCode = "",
                unreliableReason = XfeederUnreliableReason.NO_MAJORITY_SECTOR,
                observedSectorCount = 1
            )
        )

        assertThat(missingReason).isEqualTo(XfeederClosureEvidenceIssue.UNRELIABLE_REASON_REQUIRED)
        assertThat(invalidCount).isEqualTo(XfeederClosureEvidenceIssue.OBSERVED_SECTOR_COUNT_INVALID)
    }
}
