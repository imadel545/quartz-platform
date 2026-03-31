package com.quartz.platform.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QosCompletionAssessmentTest {

    @Test
    fun `assessment is valid for coherent qos completion payload`() {
        val summary = QosRunSummary(
            scriptId = "qos-script-1",
            scriptName = "Voix et SMS",
            configuredTechnologies = setOf("4G", "5G"),
            selectedTestFamilies = setOf(QosTestFamily.SMS, QosTestFamily.VOLTE_CALL),
            familyExecutionResults = listOf(
                QosFamilyExecutionResult(
                    family = QosTestFamily.SMS,
                    status = QosFamilyExecutionStatus.PASSED
                ),
                QosFamilyExecutionResult(
                    family = QosTestFamily.VOLTE_CALL,
                    status = QosFamilyExecutionStatus.FAILED,
                    failureReasonCode = QosExecutionIssueCode.NETWORK_UNAVAILABLE,
                    failureReason = "No ack"
                )
            ),
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
                    eventType = QosExecutionEventType.PASSED,
                    occurredAtEpochMillis = 1100L
                ),
                QosExecutionTimelineEvent(
                    family = QosTestFamily.VOLTE_CALL,
                    repetitionIndex = 1,
                    eventType = QosExecutionEventType.STARTED,
                    occurredAtEpochMillis = 1200L
                ),
                QosExecutionTimelineEvent(
                    family = QosTestFamily.VOLTE_CALL,
                    repetitionIndex = 1,
                    eventType = QosExecutionEventType.FAILED,
                    reasonCode = QosExecutionIssueCode.NETWORK_UNAVAILABLE,
                    reason = "No ack",
                    occurredAtEpochMillis = 1300L
                )
            ),
            targetTechnology = "4G",
            targetPhoneNumber = "+212600000001",
            iterationCount = 2,
            successCount = 1,
            failureCount = 1
        )

        val assessment = assessQosCompletion(summary)

        assertThat(assessment.canComplete).isTrue()
        assertThat(assessment.issues).isEmpty()
    }

    @Test
    fun `assessment reports missing script family completion and counters mismatch`() {
        val summary = QosRunSummary(
            scriptId = null,
            scriptName = null,
            selectedTestFamilies = setOf(QosTestFamily.THROUGHPUT_LATENCY),
            familyExecutionResults = listOf(
                QosFamilyExecutionResult(
                    family = QosTestFamily.THROUGHPUT_LATENCY,
                    status = QosFamilyExecutionStatus.NOT_RUN
                )
            ),
            iterationCount = 1,
            successCount = 1,
            failureCount = 0
        )

        val assessment = assessQosCompletion(summary)

        assertThat(assessment.canComplete).isFalse()
        assertThat(assessment.issues).contains(QosCompletionIssue.SCRIPT_REFERENCE_MISSING)
        assertThat(assessment.issues).contains(QosCompletionIssue.FAMILY_RESULT_INCOMPLETE)
        assertThat(assessment.issues).contains(QosCompletionIssue.COUNTERS_INCONSISTENT)
    }

    @Test
    fun `assessment reports phone and technology constraints for call families`() {
        val summary = QosRunSummary(
            scriptId = "qos-script-1",
            scriptName = "Voice",
            configuredTechnologies = setOf("5G"),
            selectedTestFamilies = setOf(QosTestFamily.CSFB_CALL),
            familyExecutionResults = listOf(
                QosFamilyExecutionResult(
                    family = QosTestFamily.CSFB_CALL,
                    status = QosFamilyExecutionStatus.PASSED
                )
            ),
            executionTimelineEvents = listOf(
                QosExecutionTimelineEvent(
                    family = QosTestFamily.CSFB_CALL,
                    repetitionIndex = 1,
                    eventType = QosExecutionEventType.PASSED,
                    occurredAtEpochMillis = 2000L
                )
            ),
            targetTechnology = "4G",
            targetPhoneNumber = null,
            iterationCount = 1,
            successCount = 1,
            failureCount = 0
        )

        val assessment = assessQosCompletion(summary)

        assertThat(assessment.canComplete).isFalse()
        assertThat(assessment.issues).contains(QosCompletionIssue.PHONE_TARGET_MISSING)
        assertThat(assessment.issues).contains(QosCompletionIssue.TARGET_TECHNOLOGY_INVALID)
    }

    @Test
    fun `assessment reports repetition coverage issue when configured repeat is not reached`() {
        val summary = QosRunSummary(
            scriptId = "qos-script-1",
            scriptName = "QoS",
            configuredRepeatCount = 2,
            selectedTestFamilies = setOf(QosTestFamily.SMS),
            familyExecutionResults = listOf(
                QosFamilyExecutionResult(
                    family = QosTestFamily.SMS,
                    status = QosFamilyExecutionStatus.PASSED
                )
            ),
            executionTimelineEvents = listOf(
                QosExecutionTimelineEvent(
                    family = QosTestFamily.SMS,
                    repetitionIndex = 1,
                    eventType = QosExecutionEventType.PASSED,
                    occurredAtEpochMillis = 1000L
                )
            ),
            targetPhoneNumber = "+212600000001",
            iterationCount = 1,
            successCount = 1,
            failureCount = 0
        )

        val assessment = assessQosCompletion(summary)

        assertThat(assessment.canComplete).isFalse()
        assertThat(assessment.issues).contains(QosCompletionIssue.REPETITION_COVERAGE_INCOMPLETE)
    }

    @Test
    fun `preflight requires started repetition before pass action`() {
        val summary = QosRunSummary(
            scriptId = "qos-script-1",
            scriptName = "QoS",
            selectedTestFamilies = setOf(QosTestFamily.THROUGHPUT_LATENCY)
        )

        val issues = assessQosFamilyPreflight(
            qosRunSummary = summary,
            family = QosTestFamily.THROUGHPUT_LATENCY,
            action = QosRunnerAction.MARK_PASSED,
            prerequisiteNetworkReady = true,
            prerequisiteBatterySufficient = true,
            prerequisiteLocationReady = true,
            reasonCode = null,
            failureReason = null
        )

        assertThat(issues).contains(QosPreflightIssue.REPETITION_NOT_STARTED)
    }

    @Test
    fun `assessment reports missing failure taxonomy for failed family`() {
        val summary = QosRunSummary(
            scriptId = "qos-script-1",
            scriptName = "QoS",
            selectedTestFamilies = setOf(QosTestFamily.SMS),
            familyExecutionResults = listOf(
                QosFamilyExecutionResult(
                    family = QosTestFamily.SMS,
                    status = QosFamilyExecutionStatus.FAILED,
                    failureReasonCode = null,
                    failureReason = "legacy message"
                )
            ),
            executionTimelineEvents = listOf(
                QosExecutionTimelineEvent(
                    family = QosTestFamily.SMS,
                    repetitionIndex = 1,
                    eventType = QosExecutionEventType.FAILED,
                    reason = "legacy message",
                    occurredAtEpochMillis = 2000L
                )
            ),
            targetPhoneNumber = "+212600000001",
            iterationCount = 0,
            successCount = 0,
            failureCount = 1
        )

        val assessment = assessQosCompletion(summary)

        assertThat(assessment.issues).contains(QosCompletionIssue.FAILURE_REASON_CODE_MISSING)
    }
}
