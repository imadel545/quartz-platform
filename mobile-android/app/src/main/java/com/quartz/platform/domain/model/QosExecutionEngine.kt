package com.quartz.platform.domain.model

enum class QosExecutionEngineState {
    READY,
    PREFLIGHT_BLOCKED,
    RUNNING,
    PAUSED,
    RESUMED,
    COMPLETED,
    FAILED,
    BLOCKED
}

enum class QosRunPlanItemStatus {
    PENDING,
    RUNNING,
    PAUSED,
    PASSED,
    FAILED,
    BLOCKED
}

data class QosRunPlanItem(
    val family: QosTestFamily,
    val repetitionIndex: Int,
    val status: QosRunPlanItemStatus,
    val lastEventType: QosExecutionEventType? = null,
    val lastEventAtEpochMillis: Long? = null
)

data class QosExecutionSnapshot(
    val engineState: QosExecutionEngineState,
    val requiredRepeatCount: Int,
    val plannedRunCount: Int,
    val pendingRunCount: Int,
    val runningRunCount: Int,
    val pausedRunCount: Int,
    val passedRunCount: Int,
    val failedRunCount: Int,
    val blockedRunCount: Int,
    val activeFamily: QosTestFamily? = null,
    val activeRepetitionIndex: Int? = null
) {
    val remainingRunCount: Int
        get() = pendingRunCount + runningRunCount + pausedRunCount
}

internal fun qosExecutionEventSortOrder(eventType: QosExecutionEventType): Int {
    return when (eventType) {
        QosExecutionEventType.STARTED -> 0
        QosExecutionEventType.PAUSED -> 1
        QosExecutionEventType.RESUMED -> 2
        QosExecutionEventType.PASSED -> 3
        QosExecutionEventType.FAILED -> 4
        QosExecutionEventType.BLOCKED -> 5
    }
}

fun computeQosRunPlan(qosRunSummary: QosRunSummary): List<QosRunPlanItem> {
    val selectedFamilies = qosRunSummary.selectedTestFamilies.sortedBy { family -> family.name }
    if (selectedFamilies.isEmpty()) return emptyList()

    val requiredRepeatCount = qosRunSummary.configuredRepeatCount?.coerceAtLeast(1) ?: 1
    val maxTimelineRepetition = qosRunSummary.executionTimelineEvents
        .filter { event -> event.family in qosRunSummary.selectedTestFamilies }
        .maxOfOrNull { event -> event.repetitionIndex.coerceAtLeast(1) }
        ?: 0
    val maxRepetition = maxOf(requiredRepeatCount, maxTimelineRepetition)

    val planByKey = linkedMapOf<Pair<QosTestFamily, Int>, QosRunPlanItem>()
    selectedFamilies.forEach { family ->
        (1..maxRepetition).forEach { repetitionIndex ->
            planByKey[family to repetitionIndex] = QosRunPlanItem(
                family = family,
                repetitionIndex = repetitionIndex,
                status = QosRunPlanItemStatus.PENDING
            )
        }
    }

    val orderedEvents = qosRunSummary.executionTimelineEvents
        .filter { event -> event.family in qosRunSummary.selectedTestFamilies }
        .sortedWith(
            compareBy<QosExecutionTimelineEvent> { event -> event.occurredAtEpochMillis }
                .thenBy { event -> event.family.name }
                .thenBy { event -> event.repetitionIndex }
                .thenBy { event -> qosExecutionEventSortOrder(event.eventType) }
        )

    orderedEvents.forEach { event ->
        val normalizedRepetition = event.repetitionIndex.coerceAtLeast(1)
        val key = event.family to normalizedRepetition
        val current = planByKey[key] ?: QosRunPlanItem(
            family = event.family,
            repetitionIndex = normalizedRepetition,
            status = QosRunPlanItemStatus.PENDING
        )

        val nextStatus = when (event.eventType) {
            QosExecutionEventType.STARTED -> {
                if (current.status == QosRunPlanItemStatus.PENDING) {
                    QosRunPlanItemStatus.RUNNING
                } else {
                    current.status
                }
            }
            QosExecutionEventType.PAUSED -> {
                if (current.status == QosRunPlanItemStatus.RUNNING || current.status == QosRunPlanItemStatus.PAUSED) {
                    QosRunPlanItemStatus.PAUSED
                } else {
                    current.status
                }
            }
            QosExecutionEventType.RESUMED -> {
                if (current.status == QosRunPlanItemStatus.PAUSED || current.status == QosRunPlanItemStatus.RUNNING) {
                    QosRunPlanItemStatus.RUNNING
                } else {
                    current.status
                }
            }
            QosExecutionEventType.PASSED -> QosRunPlanItemStatus.PASSED
            QosExecutionEventType.FAILED -> QosRunPlanItemStatus.FAILED
            QosExecutionEventType.BLOCKED -> QosRunPlanItemStatus.BLOCKED
        }

        planByKey[key] = current.copy(
            status = nextStatus,
            lastEventType = event.eventType,
            lastEventAtEpochMillis = event.occurredAtEpochMillis
        )
    }

    return planByKey.values.sortedWith(
        compareBy<QosRunPlanItem> { item -> item.family.name }
            .thenBy { item -> item.repetitionIndex }
    )
}

fun deriveQosExecutionSnapshot(
    qosRunSummary: QosRunSummary,
    preconditionsReady: Boolean
): QosExecutionSnapshot {
    val requiredRepeatCount = qosRunSummary.configuredRepeatCount?.coerceAtLeast(1) ?: 1
    val runPlan = computeQosRunPlan(qosRunSummary)
    val plannedRunCount = runPlan.size

    val pendingRunCount = runPlan.count { item -> item.status == QosRunPlanItemStatus.PENDING }
    val runningRunCount = runPlan.count { item -> item.status == QosRunPlanItemStatus.RUNNING }
    val pausedRunCount = runPlan.count { item -> item.status == QosRunPlanItemStatus.PAUSED }
    val passedRunCount = runPlan.count { item -> item.status == QosRunPlanItemStatus.PASSED }
    val failedRunCount = runPlan.count { item -> item.status == QosRunPlanItemStatus.FAILED }
    val blockedRunCount = runPlan.count { item -> item.status == QosRunPlanItemStatus.BLOCKED }

    val activeRun = runPlan.firstOrNull { item ->
        item.status == QosRunPlanItemStatus.RUNNING || item.status == QosRunPlanItemStatus.PAUSED
    }
    val latestEventType = qosRunSummary.executionTimelineEvents
        .filter { event -> event.family in qosRunSummary.selectedTestFamilies }
        .maxWithOrNull(
            compareBy<QosExecutionTimelineEvent> { event -> event.occurredAtEpochMillis }
                .thenBy { event -> event.family.name }
                .thenBy { event -> event.repetitionIndex }
                .thenBy { event -> qosExecutionEventSortOrder(event.eventType) }
        )
        ?.eventType

    val preflightBlocked = !preconditionsReady ||
        qosRunSummary.scriptId.isNullOrBlank() ||
        qosRunSummary.scriptName.isNullOrBlank() ||
        qosRunSummary.selectedTestFamilies.isEmpty()

    val state = when {
        preflightBlocked -> QosExecutionEngineState.PREFLIGHT_BLOCKED
        activeRun?.status == QosRunPlanItemStatus.PAUSED -> QosExecutionEngineState.PAUSED
        activeRun?.status == QosRunPlanItemStatus.RUNNING && latestEventType == QosExecutionEventType.RESUMED -> {
            QosExecutionEngineState.RESUMED
        }
        activeRun?.status == QosRunPlanItemStatus.RUNNING -> QosExecutionEngineState.RUNNING
        plannedRunCount > 0 && pendingRunCount == 0 && runningRunCount == 0 && pausedRunCount == 0 -> {
            QosExecutionEngineState.COMPLETED
        }
        latestEventType == QosExecutionEventType.FAILED -> QosExecutionEngineState.FAILED
        latestEventType == QosExecutionEventType.BLOCKED -> QosExecutionEngineState.BLOCKED
        else -> QosExecutionEngineState.READY
    }

    return QosExecutionSnapshot(
        engineState = state,
        requiredRepeatCount = requiredRepeatCount,
        plannedRunCount = plannedRunCount,
        pendingRunCount = pendingRunCount,
        runningRunCount = runningRunCount,
        pausedRunCount = pausedRunCount,
        passedRunCount = passedRunCount,
        failedRunCount = failedRunCount,
        blockedRunCount = blockedRunCount,
        activeFamily = activeRun?.family,
        activeRepetitionIndex = activeRun?.repetitionIndex
    )
}

data class QosPassFailCounters(
    val iterationCount: Int,
    val successCount: Int,
    val failureCount: Int
)

fun deriveQosPassFailCounters(qosRunSummary: QosRunSummary): QosPassFailCounters {
    val timelinePassedCount = qosRunSummary.executionTimelineEvents.count { event ->
        event.eventType == QosExecutionEventType.PASSED
    }
    val timelineFailedCount = qosRunSummary.executionTimelineEvents.count { event ->
        event.eventType == QosExecutionEventType.FAILED
    }
    val passedCount = if (timelinePassedCount + timelineFailedCount > 0) {
        timelinePassedCount
    } else {
        qosRunSummary.selectedTestFamilies.count { family ->
            qosRunSummary.familyExecutionResults.firstOrNull { result -> result.family == family }?.status ==
                QosFamilyExecutionStatus.PASSED
        }
    }
    val failedCount = if (timelinePassedCount + timelineFailedCount > 0) {
        timelineFailedCount
    } else {
        qosRunSummary.selectedTestFamilies.count { family ->
            qosRunSummary.familyExecutionResults.firstOrNull { result -> result.family == family }?.status ==
                QosFamilyExecutionStatus.FAILED
        }
    }
    return QosPassFailCounters(
        iterationCount = passedCount + failedCount,
        successCount = passedCount,
        failureCount = failedCount
    )
}

