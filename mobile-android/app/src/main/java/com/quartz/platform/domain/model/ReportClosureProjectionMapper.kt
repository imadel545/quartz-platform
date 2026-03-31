package com.quartz.platform.domain.model

fun GuidedSessionClosureProjection.toReportClosureProjection(): XfeederReportClosureProjection {
    return XfeederReportClosureProjection(
        sessionId = sessionId,
        siteId = siteId,
        sectorId = sectorId,
        sectorCode = sectorCode,
        sectorOutcome = sectorOutcome,
        relatedSectorCode = relatedSectorCode,
        unreliableReason = unreliableReason,
        observedSectorCount = observedSectorCount,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}

fun RetClosureProjection.toReportClosureProjection(): RetReportClosureProjection {
    return RetReportClosureProjection(
        sessionId = sessionId,
        siteId = siteId,
        sectorId = sectorId,
        sectorCode = sectorCode,
        sessionStatus = sessionStatus,
        resultOutcome = resultOutcome,
        requiredStepCount = requiredStepCount,
        completedRequiredStepCount = completedRequiredStepCount,
        measurementZoneRadiusMeters = measurementZoneRadiusMeters,
        proximityModeEnabled = proximityModeEnabled,
        resultSummary = resultSummary,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}

fun PerformanceSession.toReportClosureProjection(): ReportClosureProjection {
    val requiredStepCount = steps.count { step -> step.required }
    val completedRequiredStepCount = steps.count { step ->
        step.required && step.status == PerformanceStepStatus.DONE
    }
    return when (workflowType) {
        PerformanceWorkflowType.THROUGHPUT -> {
            ThroughputReportClosureProjection(
                sessionId = id,
                siteId = siteId,
                siteCode = siteCode,
                sessionStatus = status,
                preconditionsReady = preconditionsReady,
                requiredStepCount = requiredStepCount,
                completedRequiredStepCount = completedRequiredStepCount,
                downloadMbps = throughputMetrics.downloadMbps,
                uploadMbps = throughputMetrics.uploadMbps,
                latencyMs = throughputMetrics.latencyMs,
                minDownloadMbps = throughputMetrics.minDownloadMbps,
                minUploadMbps = throughputMetrics.minUploadMbps,
                maxLatencyMs = throughputMetrics.maxLatencyMs,
                resultSummary = resultSummary.takeIf { summary -> summary.isNotBlank() },
                updatedAtEpochMillis = updatedAtEpochMillis
            )
        }

        PerformanceWorkflowType.QOS_SCRIPT -> {
            QosReportClosureProjection(
                sessionId = id,
                siteId = siteId,
                siteCode = siteCode,
                sessionStatus = status,
                preconditionsReady = preconditionsReady,
                requiredStepCount = requiredStepCount,
                completedRequiredStepCount = completedRequiredStepCount,
                scriptName = qosRunSummary.scriptName,
                configuredRepeatCount = qosRunSummary.configuredRepeatCount,
                configuredTechnologies = qosRunSummary.configuredTechnologies,
                scriptSnapshotUpdatedAtEpochMillis = qosRunSummary.scriptSnapshotUpdatedAtEpochMillis,
                testFamilies = qosRunSummary.selectedTestFamilies,
                familyExecutionResults = qosRunSummary.familyExecutionResults,
                targetTechnology = qosRunSummary.targetTechnology,
                iterationCount = qosRunSummary.iterationCount,
                successCount = qosRunSummary.successCount,
                failureCount = qosRunSummary.failureCount,
                resultSummary = resultSummary.takeIf { summary -> summary.isNotBlank() },
                updatedAtEpochMillis = updatedAtEpochMillis
            )
        }
    }
}
