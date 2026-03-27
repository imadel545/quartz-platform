package com.quartz.platform.data.remote.simulation

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.data.remote.SyncPushResult
import com.quartz.platform.domain.model.SyncAggregateType
import com.quartz.platform.domain.model.SyncJob
import com.quartz.platform.domain.model.SyncJobStatus
import com.quartz.platform.domain.model.SyncOperationType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DebugSyncSimulationControllerTest {

    @Test
    fun `fail next retryable returns retryable once then resets to normal`() = runTest {
        val controller = DebugSyncSimulationController()
        val job = testJob()

        controller.setMode(SyncSimulationMode.FAIL_NEXT_RETRYABLE)
        val first = controller.overridePushResult(job)
        val second = controller.overridePushResult(job)

        assertThat(first).isInstanceOf(SyncPushResult.RetryableFailure::class.java)
        assertThat(second).isNull()
        assertThat(controller.observeMode().first()).isEqualTo(SyncSimulationMode.NORMAL_SUCCESS)
    }

    @Test
    fun `fail next terminal returns terminal once then resets to normal`() = runTest {
        val controller = DebugSyncSimulationController()
        val job = testJob()

        controller.setMode(SyncSimulationMode.FAIL_NEXT_TERMINAL)
        val first = controller.overridePushResult(job)
        val second = controller.overridePushResult(job)

        assertThat(first).isInstanceOf(SyncPushResult.TerminalFailure::class.java)
        assertThat(second).isNull()
        assertThat(controller.observeMode().first()).isEqualTo(SyncSimulationMode.NORMAL_SUCCESS)
    }

    @Test
    fun `fail once then success fails first push and succeeds next push for same payload`() = runTest {
        val controller = DebugSyncSimulationController()
        val job = testJob()

        controller.setMode(SyncSimulationMode.FAIL_ONCE_THEN_SUCCESS)
        val first = controller.overridePushResult(job)
        val second = controller.overridePushResult(job)

        assertThat(first).isInstanceOf(SyncPushResult.RetryableFailure::class.java)
        assertThat(second).isNull()
    }

    private fun testJob(payloadReference: String = "report_draft:1:rev:1"): SyncJob {
        return SyncJob(
            id = "job-1",
            aggregateType = SyncAggregateType.REPORT,
            aggregateId = "draft-1",
            operationType = SyncOperationType.UPLOAD,
            payloadReference = payloadReference,
            status = SyncJobStatus.PENDING,
            retryCount = 0,
            nextAttemptAtEpochMillis = null,
            lastAttemptAtEpochMillis = null,
            lastError = null
        )
    }
}
