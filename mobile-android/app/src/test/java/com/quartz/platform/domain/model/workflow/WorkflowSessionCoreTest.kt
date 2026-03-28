package com.quartz.platform.domain.model.workflow

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.domain.model.XfeederClosureEvidence
import com.quartz.platform.domain.model.XfeederGeospatialPolicy
import com.quartz.platform.domain.model.XfeederGuidedSession
import com.quartz.platform.domain.model.XfeederGuidedStep
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederSessionStatus
import com.quartz.platform.domain.model.XfeederStepCode
import com.quartz.platform.domain.model.XfeederStepStatus
import org.junit.Test

class WorkflowSessionCoreTest {

    @Test
    fun `completion guard counts required steps only`() {
        val guard = WorkflowCompletionGuard.fromSteps(
            listOf(
                WorkflowStepState(
                    code = "PRECONDITION",
                    required = true,
                    status = WorkflowStepStatus.DONE
                ),
                WorkflowStepState(
                    code = "MEASUREMENT",
                    required = true,
                    status = WorkflowStepStatus.IN_PROGRESS
                ),
                WorkflowStepState(
                    code = "OPTIONAL_NOTE",
                    required = false,
                    status = WorkflowStepStatus.TODO
                )
            )
        )

        assertThat(guard.requiredStepCount).isEqualTo(2)
        assertThat(guard.completedRequiredStepCount).isEqualTo(1)
        assertThat(guard.missingRequiredStepCount).isEqualTo(1)
        assertThat(guard.canComplete).isFalse()
    }

    @Test
    fun `xfeeder session exposes reusable workflow core views`() {
        val session = XfeederGuidedSession(
            id = "session-1",
            siteId = "site-1",
            sectorId = "sector-1",
            sectorCode = "S0",
            measurementZoneRadiusMeters = XfeederGeospatialPolicy.DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS,
            measurementZoneExtensionReason = "",
            proximityModeEnabled = false,
            status = XfeederSessionStatus.IN_PROGRESS,
            sectorOutcome = XfeederSectorOutcome.MIXFEEDER,
            closureEvidence = XfeederClosureEvidence(
                relatedSectorCode = "S1",
                unreliableReason = null,
                observedSectorCount = null
            ),
            notes = "Alternance observée",
            resultSummary = "Session shell guidée",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L,
            completedAtEpochMillis = null,
            steps = listOf(
                XfeederGuidedStep(
                    code = XfeederStepCode.PRECONDITION_NETWORK_READY,
                    required = true,
                    status = XfeederStepStatus.DONE
                ),
                XfeederGuidedStep(
                    code = XfeederStepCode.CHECK_MIXFEEDER_ALTERNANCE,
                    required = true,
                    status = XfeederStepStatus.TODO
                )
            )
        )

        assertThat(session.identity.sessionId).isEqualTo("session-1")
        assertThat(session.identity.scopeId).isEqualTo("sector-1")
        assertThat(session.identity.scopeCode).isEqualTo("S0")
        assertThat(session.closureSummary.outcome).isEqualTo(XfeederSectorOutcome.MIXFEEDER)
        assertThat(session.closureSummary.notes).contains("Alternance")
        assertThat(session.completionGuard().canComplete).isFalse()
        assertThat(session.completionGuard().missingRequiredStepCount).isEqualTo(1)
    }
}
