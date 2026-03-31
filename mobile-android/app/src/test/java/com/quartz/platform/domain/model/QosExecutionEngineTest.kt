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
        assertThat(snapshot.activeFamily).isEqualTo(QosTestFamily.SMS)
        assertThat(snapshot.activeRepetitionIndex).isEqualTo(1)
        assertThat(snapshot.plannedRunCount).isEqualTo(2)
        assertThat(snapshot.pendingRunCount).isEqualTo(1)
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
}

