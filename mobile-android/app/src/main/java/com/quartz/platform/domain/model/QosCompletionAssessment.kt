package com.quartz.platform.domain.model

enum class QosCompletionIssue {
    SCRIPT_REFERENCE_MISSING,
    TEST_FAMILIES_MISSING,
    FAMILY_RESULT_INCOMPLETE,
    REPETITION_COVERAGE_INCOMPLETE,
    FAILURE_REASON_CODE_MISSING,
    PHONE_TARGET_MISSING,
    TARGET_TECHNOLOGY_INVALID,
    COUNTERS_INCONSISTENT
}

enum class QosPreflightIssue {
    PREREQUISITES_NOT_READY,
    SCRIPT_REFERENCE_MISSING,
    FAMILY_NOT_SELECTED,
    PHONE_TARGET_MISSING,
    TARGET_TECHNOLOGY_INVALID,
    REPETITION_ALREADY_STARTED,
    REPETITION_ALREADY_COMPLETED,
    ANOTHER_REPETITION_ACTIVE,
    REPETITION_NOT_STARTED,
    REPETITION_NOT_PAUSED,
    FAILURE_REASON_CODE_REQUIRED
}

enum class QosRunnerAction {
    START,
    PAUSE,
    RESUME,
    MARK_PASSED,
    MARK_FAILED,
    MARK_BLOCKED
}

data class QosFamilyRunCoverage(
    val requiredRepetitions: Int,
    val activeRepetitionIndex: Int?,
    val nextRepetitionIndex: Int,
    val startedCount: Int,
    val passedCount: Int,
    val failedCount: Int,
    val blockedCount: Int
) {
    val terminalCount: Int
        get() = passedCount + failedCount + blockedCount

    val passFailTerminalCount: Int
        get() = passedCount + failedCount
}

data class QosCompletionAssessment(
    val issues: Set<QosCompletionIssue>
) {
    val canComplete: Boolean
        get() = issues.isEmpty()
}

private val phoneRequiredFamilies: Set<QosTestFamily> = setOf(
    QosTestFamily.SMS,
    QosTestFamily.VOLTE_CALL,
    QosTestFamily.CSFB_CALL,
    QosTestFamily.EMERGENCY_CALL,
    QosTestFamily.STANDARD_CALL
)

fun assessQosCompletion(qosRunSummary: QosRunSummary): QosCompletionAssessment {
    val issues = linkedSetOf<QosCompletionIssue>()

    if (qosRunSummary.scriptId.isNullOrBlank() || qosRunSummary.scriptName.isNullOrBlank()) {
        issues += QosCompletionIssue.SCRIPT_REFERENCE_MISSING
    }
    if (qosRunSummary.selectedTestFamilies.isEmpty()) {
        issues += QosCompletionIssue.TEST_FAMILIES_MISSING
    }

    val byFamily = qosRunSummary.familyExecutionResults.associateBy { result -> result.family }
    val selectedResults = qosRunSummary.selectedTestFamilies.map { family -> family to byFamily[family] }
    val requiredRepetitions = qosRunSummary.configuredRepeatCount?.coerceAtLeast(1) ?: 1

    if (selectedResults.any { (_, result) ->
            val status = result?.status ?: QosFamilyExecutionStatus.NOT_RUN
            status != QosFamilyExecutionStatus.PASSED && status != QosFamilyExecutionStatus.FAILED
        }
    ) {
        issues += QosCompletionIssue.FAMILY_RESULT_INCOMPLETE
    }

    if (selectedResults.any { (_, result) ->
            (result?.status == QosFamilyExecutionStatus.FAILED || result?.status == QosFamilyExecutionStatus.BLOCKED) &&
                result.failureReasonCode == null
        }
    ) {
        issues += QosCompletionIssue.FAILURE_REASON_CODE_MISSING
    }

    if (selectedResults.any { (_, result) ->
            (result?.status == QosFamilyExecutionStatus.FAILED || result?.status == QosFamilyExecutionStatus.BLOCKED) &&
                result.failureReasonCode == QosExecutionIssueCode.UNKNOWN &&
                result.failureReason.isNullOrBlank()
        }
    ) {
        issues += QosCompletionIssue.FAILURE_REASON_CODE_MISSING
    }

    val timelineCoverageIncomplete = qosRunSummary.selectedTestFamilies.any { family ->
        computeQosFamilyRunCoverage(qosRunSummary, family).passFailTerminalCount < requiredRepetitions
    }
    if (timelineCoverageIncomplete) {
        issues += QosCompletionIssue.REPETITION_COVERAGE_INCOMPLETE
    }

    val requiresPhoneTarget = qosRunSummary.selectedTestFamilies.any { family ->
        family in phoneRequiredFamilies
    }
    if (requiresPhoneTarget && qosRunSummary.targetPhoneNumber.isNullOrBlank()) {
        issues += QosCompletionIssue.PHONE_TARGET_MISSING
    }

    if (qosRunSummary.configuredTechnologies.isNotEmpty()) {
        val targetTechnology = qosRunSummary.targetTechnology
        if (targetTechnology.isNullOrBlank() || targetTechnology !in qosRunSummary.configuredTechnologies) {
            issues += QosCompletionIssue.TARGET_TECHNOLOGY_INVALID
        }
    }

    val counters = deriveQosPassFailCounters(qosRunSummary)
    if (qosRunSummary.successCount != counters.successCount ||
        qosRunSummary.failureCount != counters.failureCount ||
        qosRunSummary.iterationCount != counters.iterationCount
    ) {
        issues += QosCompletionIssue.COUNTERS_INCONSISTENT
    }

    return QosCompletionAssessment(issues = issues)
}

fun computeQosFamilyRunCoverage(
    qosRunSummary: QosRunSummary,
    family: QosTestFamily
): QosFamilyRunCoverage {
    val events = qosRunSummary.executionTimelineEvents
        .filter { event -> event.family == family }
        .sortedWith(
            compareBy<QosExecutionTimelineEvent> { event ->
                if (event.checkpointSequence > 0) {
                    event.checkpointSequence
                } else {
                    Int.MAX_VALUE
                }
            }.thenBy { event -> event.repetitionIndex }
                .thenBy { event -> event.occurredAtEpochMillis }
                .thenBy { event -> qosExecutionEventSortOrder(event.eventType) }
        )

    val legacyStatus = qosRunSummary.familyExecutionResults.firstOrNull { result ->
        result.family == family
    }?.status
    if (events.isEmpty() && (legacyStatus == QosFamilyExecutionStatus.PASSED || legacyStatus == QosFamilyExecutionStatus.FAILED)) {
        return QosFamilyRunCoverage(
            requiredRepetitions = qosRunSummary.configuredRepeatCount?.coerceAtLeast(1) ?: 1,
            activeRepetitionIndex = null,
            nextRepetitionIndex = 2,
            startedCount = 1,
            passedCount = if (legacyStatus == QosFamilyExecutionStatus.PASSED) 1 else 0,
            failedCount = if (legacyStatus == QosFamilyExecutionStatus.FAILED) 1 else 0,
            blockedCount = 0
        )
    }

    val startedRepetitionIndexes = linkedSetOf<Int>()
    val terminalRepetitionIndexes = linkedSetOf<Int>()
    var passedCount = 0
    var failedCount = 0
    var blockedCount = 0

    events.forEach { event ->
        when (event.eventType) {
            QosExecutionEventType.STARTED -> startedRepetitionIndexes += event.repetitionIndex
            QosExecutionEventType.PAUSED -> Unit
            QosExecutionEventType.RESUMED -> Unit
            QosExecutionEventType.PASSED -> {
                terminalRepetitionIndexes += event.repetitionIndex
                passedCount += 1
            }
            QosExecutionEventType.FAILED -> {
                terminalRepetitionIndexes += event.repetitionIndex
                failedCount += 1
            }
            QosExecutionEventType.BLOCKED -> {
                terminalRepetitionIndexes += event.repetitionIndex
                blockedCount += 1
            }
        }
    }

    val activeRepetitionIndex = startedRepetitionIndexes
        .filterNot { index -> index in terminalRepetitionIndexes }
        .maxOrNull()
    val maxRepetitionIndex = events.maxOfOrNull { event -> event.repetitionIndex } ?: 0

    return QosFamilyRunCoverage(
        requiredRepetitions = qosRunSummary.configuredRepeatCount?.coerceAtLeast(1) ?: 1,
        activeRepetitionIndex = activeRepetitionIndex,
        nextRepetitionIndex = maxRepetitionIndex + 1,
        startedCount = startedRepetitionIndexes.size,
        passedCount = passedCount,
        failedCount = failedCount,
        blockedCount = blockedCount
    )
}

fun assessQosFamilyPreflight(
    qosRunSummary: QosRunSummary,
    family: QosTestFamily,
    action: QosRunnerAction,
    preconditionsReady: Boolean,
    reasonCode: QosExecutionIssueCode?,
    failureReason: String?
): Set<QosPreflightIssue> {
    val issues = linkedSetOf<QosPreflightIssue>()
    if (!preconditionsReady) {
        issues += QosPreflightIssue.PREREQUISITES_NOT_READY
    }
    if (qosRunSummary.scriptId.isNullOrBlank() || qosRunSummary.scriptName.isNullOrBlank()) {
        issues += QosPreflightIssue.SCRIPT_REFERENCE_MISSING
    }
    if (family !in qosRunSummary.selectedTestFamilies) {
        issues += QosPreflightIssue.FAMILY_NOT_SELECTED
    }
    if (family in phoneRequiredFamilies && qosRunSummary.targetPhoneNumber.isNullOrBlank()) {
        issues += QosPreflightIssue.PHONE_TARGET_MISSING
    }
    if (qosRunSummary.configuredTechnologies.isNotEmpty()) {
        val targetTechnology = qosRunSummary.targetTechnology
        if (targetTechnology.isNullOrBlank() || targetTechnology !in qosRunSummary.configuredTechnologies) {
            issues += QosPreflightIssue.TARGET_TECHNOLOGY_INVALID
        }
    }

    val coverage = computeQosFamilyRunCoverage(qosRunSummary, family)
    val activeRun = deriveQosExecutionSnapshot(
        qosRunSummary = qosRunSummary,
        preconditionsReady = preconditionsReady
    )
    when (action) {
        QosRunnerAction.START -> {
            if (coverage.activeRepetitionIndex != null) {
                issues += QosPreflightIssue.REPETITION_ALREADY_STARTED
            }
            if (coverage.passFailTerminalCount >= coverage.requiredRepetitions) {
                issues += QosPreflightIssue.REPETITION_ALREADY_COMPLETED
            }
            if (
                activeRun.activeFamily != null &&
                activeRun.activeRepetitionIndex != null &&
                activeRun.activeFamily != family
            ) {
                issues += QosPreflightIssue.ANOTHER_REPETITION_ACTIVE
            }
        }
        QosRunnerAction.PAUSE -> {
            if (coverage.activeRepetitionIndex == null) {
                issues += QosPreflightIssue.REPETITION_NOT_STARTED
            }
        }
        QosRunnerAction.RESUME -> {
            val hasPausedRun = computeQosRunPlan(qosRunSummary)
                .any { run ->
                    run.family == family &&
                        run.status == QosRunPlanItemStatus.PAUSED
                }
            if (!hasPausedRun) {
                issues += QosPreflightIssue.REPETITION_NOT_PAUSED
            }
        }
        QosRunnerAction.MARK_PASSED -> {
            if (coverage.activeRepetitionIndex == null) {
                issues += QosPreflightIssue.REPETITION_NOT_STARTED
            }
        }
        QosRunnerAction.MARK_FAILED,
        QosRunnerAction.MARK_BLOCKED -> {
            if (coverage.activeRepetitionIndex == null) {
                issues += QosPreflightIssue.REPETITION_NOT_STARTED
            }
            if (reasonCode == null) {
                issues += QosPreflightIssue.FAILURE_REASON_CODE_REQUIRED
            }
            if (reasonCode == QosExecutionIssueCode.UNKNOWN && failureReason.isNullOrBlank()) {
                issues += QosPreflightIssue.FAILURE_REASON_CODE_REQUIRED
            }
        }
    }

    return issues
}
