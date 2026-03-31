package com.quartz.platform.data.repository

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.data.local.entity.RetSessionEntity
import com.quartz.platform.data.local.entity.RetStepEntity
import com.quartz.platform.domain.model.RetResultOutcome
import com.quartz.platform.domain.model.RetSessionStatus
import com.quartz.platform.domain.model.RetStepCode
import com.quartz.platform.domain.model.RetStepStatus
import org.junit.Test

class RetCompletionGuardTest {

    @Test
    fun `buildRetCompletionGuard returns not completable when required step is not done`() {
        val steps = listOf(
            RetStepEntity(
                sessionId = "ret-1",
                code = RetStepCode.CALIBRATION_PRECHECK.name,
                required = true,
                status = RetStepStatus.IN_PROGRESS.name,
                displayOrder = 0
            ),
            RetStepEntity(
                sessionId = "ret-1",
                code = RetStepCode.VALIDATION_CAPTURE.name,
                required = true,
                status = RetStepStatus.DONE.name,
                displayOrder = 1
            )
        )

        val guard = buildRetCompletionGuard(steps)

        assertThat(guard.requiredStepCount).isEqualTo(2)
        assertThat(guard.completedRequiredStepCount).isEqualTo(1)
        assertThat(guard.missingRequiredStepCount).isEqualTo(1)
        assertThat(guard.canComplete).isFalse()
    }

    @Test
    fun `buildRetCompletionGuard returns completable when all required steps are done`() {
        val steps = listOf(
            RetStepEntity(
                sessionId = "ret-1",
                code = RetStepCode.CALIBRATION_PRECHECK.name,
                required = true,
                status = RetStepStatus.DONE.name,
                displayOrder = 0
            ),
            RetStepEntity(
                sessionId = "ret-1",
                code = RetStepCode.VALIDATION_CAPTURE.name,
                required = true,
                status = RetStepStatus.DONE.name,
                displayOrder = 1
            ),
            RetStepEntity(
                sessionId = "ret-1",
                code = RetStepCode.RESTORE_TILT_AND_RESULT.name,
                required = false,
                status = RetStepStatus.BLOCKED.name,
                displayOrder = 2
            )
        )

        val guard = buildRetCompletionGuard(steps)

        assertThat(guard.requiredStepCount).isEqualTo(2)
        assertThat(guard.completedRequiredStepCount).isEqualTo(2)
        assertThat(guard.missingRequiredStepCount).isEqualTo(0)
        assertThat(guard.canComplete).isTrue()
    }

    @Test
    fun `toSiteRetClosureProjections keeps latest completed session by sector with required-step progress`() {
        val sessions = listOf(
            RetSessionEntity(
                id = "ret-old",
                siteId = "site-1",
                sectorId = "sector-a",
                sectorCode = "A",
                measurementZoneRadiusMeters = 70,
                measurementZoneExtensionReason = "",
                proximityModeEnabled = false,
                proximityReferenceAltitudeMeters = null,
                proximityReferenceAltitudeSource = "UNAVAILABLE",
                status = RetSessionStatus.COMPLETED.name,
                resultOutcome = RetResultOutcome.FAIL.name,
                notes = "",
                resultSummary = "Ancien",
                createdAtEpochMillis = 10L,
                updatedAtEpochMillis = 20L,
                completedAtEpochMillis = 20L
            ),
            RetSessionEntity(
                id = "ret-latest",
                siteId = "site-1",
                sectorId = "sector-a",
                sectorCode = "A",
                measurementZoneRadiusMeters = 100,
                measurementZoneExtensionReason = "signal faible",
                proximityModeEnabled = true,
                proximityReferenceAltitudeMeters = 120.0,
                proximityReferenceAltitudeSource = "TECHNICAL_DEFAULT",
                status = RetSessionStatus.COMPLETED.name,
                resultOutcome = RetResultOutcome.PASS.name,
                notes = "",
                resultSummary = "Conforme",
                createdAtEpochMillis = 30L,
                updatedAtEpochMillis = 40L,
                completedAtEpochMillis = 40L
            ),
            RetSessionEntity(
                id = "ret-in-progress",
                siteId = "site-1",
                sectorId = "sector-b",
                sectorCode = "B",
                measurementZoneRadiusMeters = 70,
                measurementZoneExtensionReason = "",
                proximityModeEnabled = false,
                proximityReferenceAltitudeMeters = null,
                proximityReferenceAltitudeSource = "UNAVAILABLE",
                status = RetSessionStatus.IN_PROGRESS.name,
                resultOutcome = RetResultOutcome.NOT_RUN.name,
                notes = "",
                resultSummary = "",
                createdAtEpochMillis = 50L,
                updatedAtEpochMillis = 50L,
                completedAtEpochMillis = null
            )
        )
        val stepsBySession = mapOf(
            "ret-latest" to listOf(
                RetStepEntity(
                    sessionId = "ret-latest",
                    code = RetStepCode.CALIBRATION_PRECHECK.name,
                    required = true,
                    status = RetStepStatus.DONE.name,
                    displayOrder = 0
                ),
                RetStepEntity(
                    sessionId = "ret-latest",
                    code = RetStepCode.VALIDATION_CAPTURE.name,
                    required = true,
                    status = RetStepStatus.IN_PROGRESS.name,
                    displayOrder = 1
                ),
                RetStepEntity(
                    sessionId = "ret-latest",
                    code = RetStepCode.RESTORE_TILT_AND_RESULT.name,
                    required = false,
                    status = RetStepStatus.TODO.name,
                    displayOrder = 2
                )
            )
        )

        val projections = toSiteRetClosureProjections(
            sessions = sessions,
            stepsBySession = stepsBySession
        )

        assertThat(projections).hasSize(1)
        assertThat(projections.first().sessionId).isEqualTo("ret-latest")
        assertThat(projections.first().requiredStepCount).isEqualTo(2)
        assertThat(projections.first().completedRequiredStepCount).isEqualTo(1)
        assertThat(projections.first().resultOutcome).isEqualTo(RetResultOutcome.PASS)
        assertThat(projections.first().measurementZoneRadiusMeters).isEqualTo(100)
        assertThat(projections.first().proximityModeEnabled).isTrue()
    }
}
