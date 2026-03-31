package com.quartz.platform.data.repository

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.domain.model.QosFamilyExecutionResult
import com.quartz.platform.domain.model.QosFamilyExecutionStatus
import com.quartz.platform.domain.model.QosExecutionIssueCode
import com.quartz.platform.domain.model.QosRunSummary
import com.quartz.platform.domain.model.QosTestFamily
import org.junit.Test

class PerformanceQosCompletionConsistencyTest {

    @Test
    fun `validateQosCompletionConsistency accepts coherent completed families`() {
        val summary = QosRunSummary(
            scriptId = "qos-script-1",
            scriptName = "Voix / SMS",
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
                    failureReason = "No response"
                )
            ),
            executionTimelineEvents = listOf(
                com.quartz.platform.domain.model.QosExecutionTimelineEvent(
                    family = QosTestFamily.SMS,
                    repetitionIndex = 1,
                    eventType = com.quartz.platform.domain.model.QosExecutionEventType.PASSED,
                    occurredAtEpochMillis = 1000L
                ),
                com.quartz.platform.domain.model.QosExecutionTimelineEvent(
                    family = QosTestFamily.VOLTE_CALL,
                    repetitionIndex = 1,
                    eventType = com.quartz.platform.domain.model.QosExecutionEventType.FAILED,
                    reasonCode = QosExecutionIssueCode.NETWORK_UNAVAILABLE,
                    reason = "No response",
                    occurredAtEpochMillis = 1100L
                )
            ),
            targetTechnology = "4G",
            targetPhoneNumber = "+212600000001",
            iterationCount = 2,
            successCount = 1,
            failureCount = 1
        )

        val result = runCatching { validateQosCompletionConsistency(summary) }

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `validateQosCompletionConsistency rejects incomplete or incoherent families`() {
        val summary = QosRunSummary(
            scriptId = "qos-script-1",
            scriptName = "Voix / SMS",
            configuredTechnologies = setOf("4G", "5G"),
            selectedTestFamilies = setOf(QosTestFamily.SMS, QosTestFamily.VOLTE_CALL),
            familyExecutionResults = listOf(
                QosFamilyExecutionResult(
                    family = QosTestFamily.SMS,
                    status = QosFamilyExecutionStatus.NOT_RUN
                ),
                QosFamilyExecutionResult(
                    family = QosTestFamily.VOLTE_CALL,
                    status = QosFamilyExecutionStatus.FAILED,
                    failureReasonCode = QosExecutionIssueCode.NETWORK_UNAVAILABLE,
                    failureReason = "No response"
                )
            ),
            targetTechnology = "4G",
            targetPhoneNumber = "+212600000001",
            iterationCount = 2,
            successCount = 1,
            failureCount = 1
        )

        val result = runCatching { validateQosCompletionConsistency(summary) }

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `validateQosCompletionConsistency rejects failed family without failure reason`() {
        val summary = QosRunSummary(
            scriptId = "qos-script-1",
            scriptName = "QoS",
            configuredTechnologies = setOf("4G"),
            selectedTestFamilies = setOf(QosTestFamily.THROUGHPUT_LATENCY),
            familyExecutionResults = listOf(
                QosFamilyExecutionResult(
                    family = QosTestFamily.THROUGHPUT_LATENCY,
                    status = QosFamilyExecutionStatus.FAILED
                )
            ),
            executionTimelineEvents = listOf(
                com.quartz.platform.domain.model.QosExecutionTimelineEvent(
                    family = QosTestFamily.THROUGHPUT_LATENCY,
                    repetitionIndex = 1,
                    eventType = com.quartz.platform.domain.model.QosExecutionEventType.FAILED,
                    occurredAtEpochMillis = 1000L
                )
            ),
            targetTechnology = "4G",
            iterationCount = 1,
            successCount = 0,
            failureCount = 1
        )

        val result = runCatching { validateQosCompletionConsistency(summary) }

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `validateQosCompletionConsistency rejects phone-required family without target phone`() {
        val summary = QosRunSummary(
            scriptId = "qos-script-1",
            scriptName = "Voix / SMS",
            configuredTechnologies = setOf("4G"),
            selectedTestFamilies = setOf(QosTestFamily.SMS),
            familyExecutionResults = listOf(
                QosFamilyExecutionResult(
                    family = QosTestFamily.SMS,
                    status = QosFamilyExecutionStatus.PASSED
                )
            ),
            executionTimelineEvents = listOf(
                com.quartz.platform.domain.model.QosExecutionTimelineEvent(
                    family = QosTestFamily.SMS,
                    repetitionIndex = 1,
                    eventType = com.quartz.platform.domain.model.QosExecutionEventType.PASSED,
                    occurredAtEpochMillis = 1000L
                )
            ),
            targetTechnology = "4G",
            iterationCount = 1,
            successCount = 1,
            failureCount = 0
        )

        val result = runCatching { validateQosCompletionConsistency(summary) }

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `validateQosCompletionConsistency rejects target technology outside configured script set`() {
        val summary = QosRunSummary(
            scriptId = "qos-script-1",
            scriptName = "Latence + Débit",
            configuredTechnologies = setOf("5G"),
            selectedTestFamilies = setOf(QosTestFamily.THROUGHPUT_LATENCY),
            familyExecutionResults = listOf(
                QosFamilyExecutionResult(
                    family = QosTestFamily.THROUGHPUT_LATENCY,
                    status = QosFamilyExecutionStatus.PASSED
                )
            ),
            executionTimelineEvents = listOf(
                com.quartz.platform.domain.model.QosExecutionTimelineEvent(
                    family = QosTestFamily.THROUGHPUT_LATENCY,
                    repetitionIndex = 1,
                    eventType = com.quartz.platform.domain.model.QosExecutionEventType.PASSED,
                    occurredAtEpochMillis = 1000L
                )
            ),
            targetTechnology = "4G",
            iterationCount = 1,
            successCount = 1,
            failureCount = 0
        )

        val result = runCatching { validateQosCompletionConsistency(summary) }

        assertThat(result.isFailure).isTrue()
    }
}
