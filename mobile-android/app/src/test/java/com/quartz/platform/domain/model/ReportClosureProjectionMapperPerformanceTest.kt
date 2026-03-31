package com.quartz.platform.domain.model

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.domain.model.workflow.WorkflowStepState
import com.quartz.platform.domain.model.workflow.WorkflowStepStatus
import org.junit.Test

class ReportClosureProjectionMapperPerformanceTest {

    @Test
    fun `maps qos session families into closure projection`() {
        val session = PerformanceSession(
            id = "perf-1",
            siteId = "site-1",
            siteCode = "S-001",
            workflowType = PerformanceWorkflowType.QOS_SCRIPT,
            operatorName = "Operator",
            technology = "4G",
            status = PerformanceSessionStatus.COMPLETED,
            prerequisiteNetworkReady = true,
            prerequisiteBatterySufficient = true,
            prerequisiteLocationReady = true,
            throughputMetrics = ThroughputMetrics(),
            qosRunSummary = QosRunSummary(
                scriptId = "script-1",
                scriptName = "QoS Voice",
                configuredRepeatCount = 3,
                configuredTechnologies = setOf("4G", "5G"),
                scriptSnapshotUpdatedAtEpochMillis = 1234L,
                selectedTestFamilies = setOf(QosTestFamily.SMS, QosTestFamily.VOLTE_CALL),
                familyExecutionResults = listOf(
                    QosFamilyExecutionResult(
                        family = QosTestFamily.SMS,
                        status = QosFamilyExecutionStatus.PASSED
                    ),
                    QosFamilyExecutionResult(
                        family = QosTestFamily.VOLTE_CALL,
                        status = QosFamilyExecutionStatus.FAILED,
                        failureReason = "No carrier"
                    )
                ),
                executionTimelineEvents = listOf(
                    QosExecutionTimelineEvent(
                        family = QosTestFamily.SMS,
                        repetitionIndex = 1,
                        eventType = QosExecutionEventType.STARTED,
                        occurredAtEpochMillis = 2000L
                    ),
                    QosExecutionTimelineEvent(
                        family = QosTestFamily.SMS,
                        repetitionIndex = 1,
                        eventType = QosExecutionEventType.PASSED,
                        occurredAtEpochMillis = 2100L
                    ),
                    QosExecutionTimelineEvent(
                        family = QosTestFamily.VOLTE_CALL,
                        repetitionIndex = 1,
                        eventType = QosExecutionEventType.FAILED,
                        reason = "No carrier",
                        occurredAtEpochMillis = 2200L
                    )
                ),
                targetTechnology = "4G",
                iterationCount = 2,
                successCount = 1,
                failureCount = 1
            ),
            notes = "",
            resultSummary = "Résumé",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L,
            completedAtEpochMillis = 3L,
            steps = listOf(
                WorkflowStepState(
                    code = PerformanceStepCode.PRECONDITIONS_CHECK,
                    required = true,
                    status = WorkflowStepStatus.DONE
                )
            )
        )

        val projection = session.toReportClosureProjection()

        assertThat(projection).isInstanceOf(QosReportClosureProjection::class.java)
        val qos = projection as QosReportClosureProjection
        assertThat(qos.configuredRepeatCount).isEqualTo(3)
        assertThat(qos.configuredTechnologies).containsExactly("4G", "5G")
        assertThat(qos.scriptSnapshotUpdatedAtEpochMillis).isEqualTo(1234L)
        assertThat(qos.testFamilies).containsExactly(QosTestFamily.SMS, QosTestFamily.VOLTE_CALL)
        assertThat(qos.completedFamilyCount).isEqualTo(2)
        assertThat(qos.failedFamilyCount).isEqualTo(1)
        assertThat(qos.timelineFamilyCoverageCount).isEqualTo(2)
        assertThat(qos.requiredRepeatCount).isEqualTo(3)
        assertThat(qos.passFailRunCount).isEqualTo(2)
        assertThat(qos.familiesMeetingRequiredRepeatCount).isEqualTo(0)
        assertThat(qos.executionEngineState).isEqualTo(QosExecutionEngineState.FAILED)
        assertThat(qos.recoveryState).isEqualTo(QosRecoveryState.NONE)
        assertThat(qos.checkpointCount).isEqualTo(3)
        assertThat(qos.nextFamily).isEqualTo(QosTestFamily.SMS)
        assertThat(qos.nextRepetitionIndex).isEqualTo(2)
        assertThat(qos.executionTimelineEvents).hasSize(3)
        assertThat(qos.familyExecutionResults).hasSize(2)
    }
}
