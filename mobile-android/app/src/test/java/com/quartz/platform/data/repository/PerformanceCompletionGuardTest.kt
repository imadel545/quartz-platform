package com.quartz.platform.data.repository

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.data.local.entity.PerformanceStepEntity
import com.quartz.platform.domain.model.PerformanceStepCode
import com.quartz.platform.domain.model.PerformanceStepStatus
import org.junit.Test

class PerformanceCompletionGuardTest {

    @Test
    fun `buildPerformanceCompletionGuard returns not completable when required step is missing`() {
        val steps = listOf(
            PerformanceStepEntity(
                sessionId = "perf-1",
                code = PerformanceStepCode.PRECONDITIONS_CHECK.name,
                required = true,
                status = PerformanceStepStatus.DONE.name,
                displayOrder = 0
            ),
            PerformanceStepEntity(
                sessionId = "perf-1",
                code = PerformanceStepCode.EXECUTE_TEST.name,
                required = true,
                status = PerformanceStepStatus.IN_PROGRESS.name,
                displayOrder = 1
            )
        )

        val guard = buildPerformanceCompletionGuard(steps)

        assertThat(guard.requiredStepCount).isEqualTo(2)
        assertThat(guard.completedRequiredStepCount).isEqualTo(1)
        assertThat(guard.missingRequiredStepCount).isEqualTo(1)
        assertThat(guard.canComplete).isFalse()
    }

    @Test
    fun `buildPerformanceCompletionGuard returns completable when all required steps are done`() {
        val steps = listOf(
            PerformanceStepEntity(
                sessionId = "perf-1",
                code = PerformanceStepCode.PRECONDITIONS_CHECK.name,
                required = true,
                status = PerformanceStepStatus.DONE.name,
                displayOrder = 0
            ),
            PerformanceStepEntity(
                sessionId = "perf-1",
                code = PerformanceStepCode.EXECUTE_TEST.name,
                required = true,
                status = PerformanceStepStatus.DONE.name,
                displayOrder = 1
            ),
            PerformanceStepEntity(
                sessionId = "perf-1",
                code = PerformanceStepCode.REVIEW_RESULT.name,
                required = false,
                status = PerformanceStepStatus.BLOCKED.name,
                displayOrder = 2
            )
        )

        val guard = buildPerformanceCompletionGuard(steps)

        assertThat(guard.requiredStepCount).isEqualTo(2)
        assertThat(guard.completedRequiredStepCount).isEqualTo(2)
        assertThat(guard.missingRequiredStepCount).isEqualTo(0)
        assertThat(guard.canComplete).isTrue()
    }
}
