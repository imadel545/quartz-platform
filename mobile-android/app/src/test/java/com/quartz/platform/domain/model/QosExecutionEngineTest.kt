package com.quartz.platform.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QosExecutionEngineTest {

    @Test
    fun `derive snapshot returns paused state with active run when latest event is paused`() {
        val summary = QosRunSummary(
            scriptId = "script-1",
            scriptName = "QoS",
            configuredRepeatCount = 2,
            selectedTestFamilies = setOf(QosTestFamily.SMS),
            executionTimelineEvents = listOf(
                QosExecutionTimelineEvent(
                    family = QosTestFamily.SMS,
                    repetitionIndex = 1,
                    eventType = QosExecutionEventType.STARTED,
                    occurredAtEpochMillis = 1000L
                ),
                QosExecutionTimelineEvent(
                    family = QosTestFamily.SMS,
                    repetitionIndex = 1,
                    eventType = QosExecutionEventType.PAUSED,
                    occurredAtEpochMillis = 1100L
                )
            )
        )

        val snapshot = deriveQosExecutionSnapshot(
            qosRunSummary = summary,
            preconditionsReady = true
        )

        assertThat(snapshot.engineState).isEqualTo(QosExecutionEngineState.PAUSED)
        assertThat(snapshot.recoveryState).isEqualTo(QosRecoveryState.RESUME_AVAILABLE)
        assertThat(snapshot.activeFamily).isEqualTo(QosTestFamily.SMS)
        assertThat(snapshot.activeRepetitionIndex).isEqualTo(1)
        assertThat(snapshot.plannedRunCount).isEqualTo(2)
        assertThat(snapshot.pendingRunCount).isEqualTo(1)
        assertThat(snapshot.nextFamily).isEqualTo(QosTestFamily.SMS)
        assertThat(snapshot.nextRepetitionIndex).isEqualTo(2)
        assertThat(snapshot.checkpointCount).isEqualTo(2)
    }

    @Test
    fun `compute run plan includes dynamic repetition above configured repeat`() {
        val summary = QosRunSummary(
            scriptId = "script-1",
            scriptName = "QoS",
            configuredRepeatCount = 1,
            selectedTestFamilies = setOf(QosTestFamily.THROUGHPUT_LATENCY),
            executionTimelineEvents = listOf(
                QosExecutionTimelineEvent(
                    family = QosTestFamily.THROUGHPUT_LATENCY,
                    repetitionIndex = 2,
                    eventType = QosExecutionEventType.FAILED,
                    occurredAtEpochMillis = 1200L
                )
            )
        )

        val runPlan = computeQosRunPlan(summary)

        assertThat(runPlan).hasSize(2)
        assertThat(runPlan.last().repetitionIndex).isEqualTo(2)
        assertThat(runPlan.last().status).isEqualTo(QosRunPlanItemStatus.FAILED)
    }

    @Test
    fun `run plan uses checkpoint sequence ordering when timestamps are equal`() {
        val summary = QosRunSummary(
            scriptId = "script-1",
            scriptName = "QoS",
            configuredRepeatCount = 1,
            selectedTestFamilies = setOf(QosTestFamily.SMS),
            executionTimelineEvents = listOf(
                QosExecutionTimelineEvent(
                    family = QosTestFamily.SMS,
                    repetitionIndex = 1,
                    eventType = QosExecutionEventType.STARTED,
                    occurredAtEpochMillis = 1000L,
                    checkpointSequence = 1
                ),
                QosExecutionTimelineEvent(
                    family = QosTestFamily.SMS,
                    repetitionIndex = 1,
                    eventType = QosExecutionEventType.PAUSED,
                    occurredAtEpochMillis = 1000L,
                    checkpointSequence = 2
                ),
                QosExecutionTimelineEvent(
                    family = QosTestFamily.SMS,
                    repetitionIndex = 1,
                    eventType = QosExecutionEventType.RESUMED,
                    occurredAtEpochMillis = 1000L,
                    checkpointSequence = 3
                )
            )
        )

        val snapshot = deriveQosExecutionSnapshot(summary, preconditionsReady = true)

        assertThat(snapshot.engineState).isEqualTo(QosExecutionEngineState.RESUMED)
        assertThat(snapshot.activeFamily).isEqualTo(QosTestFamily.SMS)
        assertThat(snapshot.activeRepetitionIndex).isEqualTo(1)
        assertThat(snapshot.checkpointCount).isEqualTo(3)
    }
}
