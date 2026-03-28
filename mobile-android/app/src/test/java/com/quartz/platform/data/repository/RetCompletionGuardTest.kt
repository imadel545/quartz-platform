package com.quartz.platform.data.repository

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.data.local.entity.RetStepEntity
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
}
