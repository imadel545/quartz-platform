package com.quartz.platform.presentation.report.draft

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quartz.platform.R
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.model.QosReportClosureProjection
import com.quartz.platform.domain.model.ReportClosureProjection
import com.quartz.platform.domain.model.RetReportClosureProjection
import com.quartz.platform.domain.model.ThroughputReportClosureProjection
import com.quartz.platform.domain.model.XfeederReportClosureProjection
import com.quartz.platform.domain.model.qosExecutionEventSortOrder
import com.quartz.platform.presentation.performance.session.formatPerformanceEpoch
import com.quartz.platform.presentation.performance.session.networkStatusLabelRes
import com.quartz.platform.presentation.performance.session.performanceSessionStatusLabelRes
import com.quartz.platform.presentation.performance.session.performanceWorkflowTypeLabelRes
import com.quartz.platform.presentation.performance.session.qosExecutionEngineStateLabelRes
import com.quartz.platform.presentation.performance.session.qosExecutionEventTypeLabelRes
import com.quartz.platform.presentation.performance.session.qosFamilyExecutionStatusLabelRes
import com.quartz.platform.presentation.performance.session.qosIssueCodeLabelRes
import com.quartz.platform.presentation.performance.session.qosRecoveryStateLabelRes
import com.quartz.platform.presentation.performance.session.qosTestFamilyLabelRes
import com.quartz.platform.presentation.ret.session.retResultOutcomeLabelRes
import com.quartz.platform.presentation.ret.session.retSessionStatusLabelRes
import com.quartz.platform.presentation.xfeeder.session.xfeederSectorOutcomeLabelRes
import com.quartz.platform.presentation.xfeeder.session.xfeederUnreliableReasonLabelRes

@Composable
internal fun GuidedClosureProjectionCard(
    projections: List<ReportClosureProjection>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.report_closure_projection_header),
                style = MaterialTheme.typography.titleSmall
            )

            if (projections.isEmpty()) {
                Text(
                    text = stringResource(R.string.report_closure_projection_empty),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                projections.forEach { projection ->
                    when (projection) {
                        is XfeederReportClosureProjection -> {
                            XfeederClosureProjectionContent(projection)
                        }

                        is RetReportClosureProjection -> {
                            RetClosureProjectionContent(projection)
                        }

                        is ThroughputReportClosureProjection -> {
                            ThroughputClosureProjectionContent(projection)
                        }

                        is QosReportClosureProjection -> {
                            QosClosureProjectionContent(projection)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun XfeederClosureProjectionContent(
    projection: XfeederReportClosureProjection
) {
    Text(
        text = stringResource(R.string.report_closure_workflow_xfeeder),
        style = MaterialTheme.typography.labelLarge
    )
    Text(
        text = stringResource(R.string.report_closure_sector, projection.sectorCode),
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        text = stringResource(
            R.string.report_closure_outcome,
            stringResource(xfeederSectorOutcomeLabelRes(projection.sectorOutcome))
        ),
        style = MaterialTheme.typography.bodySmall
    )
    projection.relatedSectorCode?.let { related ->
        Text(
            text = stringResource(R.string.report_closure_related_sector, related),
            style = MaterialTheme.typography.bodySmall
        )
    }
    projection.unreliableReason?.let { reason ->
        Text(
            text = stringResource(
                R.string.report_closure_unreliable_reason,
                stringResource(xfeederUnreliableReasonLabelRes(reason))
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    projection.observedSectorCount?.let { observedCount ->
        Text(
            text = stringResource(R.string.report_closure_observed_sector_count, observedCount),
            style = MaterialTheme.typography.bodySmall
        )
    }
    Text(
        text = stringResource(R.string.label_updated_at, formatPerformanceEpoch(projection.updatedAtEpochMillis)),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun RetClosureProjectionContent(
    projection: RetReportClosureProjection
) {
    Text(
        text = stringResource(R.string.report_closure_workflow_ret),
        style = MaterialTheme.typography.labelLarge
    )
    Text(
        text = stringResource(R.string.report_closure_sector, projection.sectorCode),
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        text = stringResource(
            R.string.report_closure_ret_result,
            stringResource(retResultOutcomeLabelRes(projection.resultOutcome))
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_ret_status,
            stringResource(retSessionStatusLabelRes(projection.sessionStatus))
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_ret_required_step_progress,
            projection.completedRequiredStepCount,
            projection.requiredStepCount
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_ret_measurement_zone,
            projection.measurementZoneRadiusMeters
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_ret_proximity_mode,
            if (projection.proximityModeEnabled) {
                stringResource(R.string.value_yes)
            } else {
                stringResource(R.string.value_no)
            }
        ),
        style = MaterialTheme.typography.bodySmall
    )
    projection.resultSummary?.let { summary ->
        Text(
            text = stringResource(R.string.report_closure_ret_result_summary, summary),
            style = MaterialTheme.typography.bodySmall
        )
    }
    Text(
        text = stringResource(R.string.label_updated_at, formatPerformanceEpoch(projection.updatedAtEpochMillis)),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun ThroughputClosureProjectionContent(
    projection: ThroughputReportClosureProjection
) {
    Text(
        text = stringResource(
            R.string.report_closure_workflow_performance,
            stringResource(performanceWorkflowTypeLabelRes(PerformanceWorkflowType.THROUGHPUT))
        ),
        style = MaterialTheme.typography.labelLarge
    )
    Text(
        text = stringResource(R.string.report_closure_performance_site, projection.siteCode),
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        text = stringResource(
            R.string.report_closure_performance_status,
            stringResource(performanceSessionStatusLabelRes(projection.sessionStatus))
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_performance_required_step_progress,
            projection.completedRequiredStepCount,
            projection.requiredStepCount
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_performance_prerequisites,
            if (projection.preconditionsReady) {
                stringResource(R.string.value_yes)
            } else {
                stringResource(R.string.value_no)
            }
        ),
        style = MaterialTheme.typography.bodySmall
    )
    PerformanceObservedDiagnosticsSection(
        observedNetworkStatus = projection.observedNetworkStatus,
        observedBatteryLevelPercent = projection.observedBatteryLevelPercent,
        observedLocationAvailable = projection.observedLocationAvailable,
        observedSignalsCapturedAtEpochMillis = projection.observedSignalsCapturedAtEpochMillis
    )
    projection.downloadMbps?.let { value ->
        Text(
            text = stringResource(R.string.report_closure_performance_download, value),
            style = MaterialTheme.typography.bodySmall
        )
    }
    projection.uploadMbps?.let { value ->
        Text(
            text = stringResource(R.string.report_closure_performance_upload, value),
            style = MaterialTheme.typography.bodySmall
        )
    }
    projection.latencyMs?.let { value ->
        Text(
            text = stringResource(R.string.report_closure_performance_latency, value),
            style = MaterialTheme.typography.bodySmall
        )
    }
    if (projection.minDownloadMbps != null || projection.minUploadMbps != null || projection.maxLatencyMs != null) {
        Text(
            text = stringResource(
                R.string.report_closure_performance_thresholds,
                projection.minDownloadMbps?.toString() ?: "-",
                projection.minUploadMbps?.toString() ?: "-",
                projection.maxLatencyMs?.toString() ?: "-"
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    projection.resultSummary?.let { summary ->
        Text(
            text = stringResource(R.string.report_closure_performance_result_summary, summary),
            style = MaterialTheme.typography.bodySmall
        )
    }
    Text(
        text = stringResource(R.string.label_updated_at, formatPerformanceEpoch(projection.updatedAtEpochMillis)),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun QosClosureProjectionContent(
    projection: QosReportClosureProjection
) {
    Text(
        text = stringResource(
            R.string.report_closure_workflow_performance,
            stringResource(performanceWorkflowTypeLabelRes(PerformanceWorkflowType.QOS_SCRIPT))
        ),
        style = MaterialTheme.typography.labelLarge
    )
    Text(
        text = stringResource(R.string.report_closure_performance_site, projection.siteCode),
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        text = stringResource(
            R.string.report_closure_performance_status,
            stringResource(performanceSessionStatusLabelRes(projection.sessionStatus))
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_performance_required_step_progress,
            projection.completedRequiredStepCount,
            projection.requiredStepCount
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_performance_prerequisites,
            if (projection.preconditionsReady) {
                stringResource(R.string.value_yes)
            } else {
                stringResource(R.string.value_no)
            }
        ),
        style = MaterialTheme.typography.bodySmall
    )
    PerformanceObservedDiagnosticsSection(
        observedNetworkStatus = projection.observedNetworkStatus,
        observedBatteryLevelPercent = projection.observedBatteryLevelPercent,
        observedLocationAvailable = projection.observedLocationAvailable,
        observedSignalsCapturedAtEpochMillis = projection.observedSignalsCapturedAtEpochMillis
    )
    Text(
        text = stringResource(
            R.string.report_closure_performance_qos_script,
            projection.scriptName ?: stringResource(R.string.value_not_available)
        ),
        style = MaterialTheme.typography.bodySmall
    )
    projection.configuredRepeatCount?.let { repeat ->
        Text(
            text = stringResource(R.string.report_closure_performance_qos_repeat_configured, repeat),
            style = MaterialTheme.typography.bodySmall
        )
    }
    if (projection.configuredTechnologies.isNotEmpty()) {
        Text(
            text = stringResource(
                R.string.report_closure_performance_qos_configured_technologies,
                projection.configuredTechnologies.sorted().joinToString(", ")
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    projection.scriptSnapshotUpdatedAtEpochMillis?.let { snapshotAt ->
        Text(
            text = stringResource(
                R.string.report_closure_performance_qos_script_snapshot_at,
                formatPerformanceEpoch(snapshotAt)
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    if (projection.testFamilies.isNotEmpty()) {
        val familyLabels = buildList {
            for (family in projection.testFamilies) {
                add(stringResource(qosTestFamilyLabelRes(family)))
            }
        }
        Text(
            text = stringResource(
                R.string.report_closure_performance_qos_test_families,
                familyLabels.joinToString(", ")
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    projection.targetTechnology?.let { technology ->
        Text(
            text = stringResource(R.string.report_closure_performance_qos_target_technology, technology),
            style = MaterialTheme.typography.bodySmall
        )
        if (projection.configuredTechnologies.isNotEmpty() && technology !in projection.configuredTechnologies) {
            Text(
                text = stringResource(R.string.report_closure_performance_qos_target_technology_mismatch),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
    if (projection.familyExecutionResults.isNotEmpty()) {
        Text(
            text = stringResource(R.string.report_closure_performance_qos_family_results_header),
            style = MaterialTheme.typography.bodySmall
        )
        projection.familyExecutionResults
            .sortedBy { result -> result.family.name }
            .forEach { result ->
                val line = stringResource(
                    R.string.report_closure_performance_qos_family_result_line,
                    stringResource(qosTestFamilyLabelRes(result.family)),
                    stringResource(qosFamilyExecutionStatusLabelRes(result.status))
                )
                val classifiedReason = result.failureReasonCode?.let { code ->
                    stringResource(
                        R.string.report_closure_performance_qos_reason_code,
                        stringResource(qosIssueCodeLabelRes(code))
                    )
                }
                Text(
                    text = buildString {
                        append(line)
                        if (!classifiedReason.isNullOrBlank()) {
                            append(" (")
                            append(classifiedReason)
                            append(")")
                        }
                        result.failureReason?.takeIf { it.isNotBlank() }?.let { reason ->
                            append(" (")
                            append(stringResource(R.string.label_failure_reason, reason))
                            append(")")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
    }
    Text(
        text = stringResource(
            R.string.report_closure_performance_qos_run_coverage,
            projection.familiesMeetingRequiredRepeatCount,
            projection.selectedFamilyCount,
            projection.requiredRepeatCount,
            projection.passFailRunCount,
            projection.blockedRunCount
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_performance_qos_engine_state,
            stringResource(qosExecutionEngineStateLabelRes(projection.executionEngineState))
        ),
        style = MaterialTheme.typography.bodySmall
    )
    if (projection.recoveryState != com.quartz.platform.domain.model.QosRecoveryState.NONE) {
        Text(
            text = stringResource(
                R.string.report_closure_performance_qos_recovery_state,
                stringResource(qosRecoveryStateLabelRes(projection.recoveryState))
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    projection.activeFamily?.let { activeFamily ->
        Text(
            text = stringResource(
                R.string.report_closure_performance_qos_active_run,
                stringResource(qosTestFamilyLabelRes(activeFamily)),
                projection.activeRepetitionIndex ?: 1
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    projection.nextFamily?.let { nextFamily ->
        Text(
            text = stringResource(
                R.string.report_closure_performance_qos_next_run,
                stringResource(qosTestFamilyLabelRes(nextFamily)),
                projection.nextRepetitionIndex ?: 1
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    Text(
        text = stringResource(
            R.string.report_closure_performance_qos_plan_progress,
            projection.plannedRunCount,
            projection.pendingRunCount
        ),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        text = stringResource(
            R.string.report_closure_performance_qos_checkpoint_count,
            projection.checkpointCount
        ),
        style = MaterialTheme.typography.bodySmall
    )
    if (projection.executionTimelineEvents.isNotEmpty()) {
        Text(
            text = stringResource(R.string.report_closure_performance_qos_timeline_header),
            style = MaterialTheme.typography.bodySmall
        )
        val orderedEvents = projection.executionTimelineEvents.sortedWith(
            compareByDescending<com.quartz.platform.domain.model.QosExecutionTimelineEvent> { event ->
                event.checkpointSequence
            }.thenBy { event ->
                event.family.name
            }.thenBy { event ->
                event.repetitionIndex
            }.thenBy { event ->
                event.occurredAtEpochMillis
            }.thenBy { event ->
                qosExecutionEventSortOrder(event.eventType)
            }
        )
        val visibleEvents = orderedEvents.take(12)
        visibleEvents.forEach { event ->
            val line = stringResource(
                R.string.report_closure_performance_qos_timeline_line,
                formatPerformanceEpoch(event.occurredAtEpochMillis),
                stringResource(qosTestFamilyLabelRes(event.family)),
                event.repetitionIndex,
                stringResource(qosExecutionEventTypeLabelRes(event.eventType))
            )
            val classifiedReason = event.reasonCode?.let { code ->
                stringResource(
                    R.string.report_closure_performance_qos_reason_code,
                    stringResource(qosIssueCodeLabelRes(code))
                )
            }
            Text(
                text = buildString {
                    append(line)
                    if (!classifiedReason.isNullOrBlank()) {
                        append(" (")
                        append(classifiedReason)
                        append(")")
                    }
                    event.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                        append(" (")
                        append(stringResource(R.string.label_failure_reason, reason))
                        append(")")
                    }
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
        val hiddenCount = orderedEvents.size - visibleEvents.size
        if (hiddenCount > 0) {
            Text(
                text = stringResource(
                    R.string.report_closure_performance_qos_timeline_more,
                    hiddenCount
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    Text(
        text = stringResource(
            R.string.report_closure_performance_qos_results,
            projection.iterationCount,
            projection.successCount,
            projection.failureCount
        ),
        style = MaterialTheme.typography.bodySmall
    )
    projection.resultSummary?.let { summary ->
        Text(
            text = stringResource(R.string.report_closure_performance_result_summary, summary),
            style = MaterialTheme.typography.bodySmall
        )
    }
    Text(
        text = stringResource(R.string.label_updated_at, formatPerformanceEpoch(projection.updatedAtEpochMillis)),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun PerformanceObservedDiagnosticsSection(
    observedNetworkStatus: com.quartz.platform.domain.model.NetworkStatus?,
    observedBatteryLevelPercent: Int?,
    observedLocationAvailable: Boolean?,
    observedSignalsCapturedAtEpochMillis: Long?
) {
    if (
        observedNetworkStatus == null &&
        observedBatteryLevelPercent == null &&
        observedLocationAvailable == null &&
        observedSignalsCapturedAtEpochMillis == null
    ) {
        return
    }

    observedNetworkStatus?.let { networkStatus ->
        Text(
            text = stringResource(
                R.string.report_closure_performance_device_network,
                stringResource(networkStatusLabelRes(networkStatus))
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    observedBatteryLevelPercent?.let { batteryPercent ->
        val batteryLabel = buildString {
            append("$batteryPercent%")
            append(" • ")
            append(
                if (batteryPercent >= com.quartz.platform.domain.model.PerformanceSession.MIN_RECOMMENDED_BATTERY_PERCENT) {
                    "OK"
                } else {
                    "LOW"
                }
            )
        }
        Text(
            text = stringResource(R.string.report_closure_performance_device_battery, batteryLabel),
            style = MaterialTheme.typography.bodySmall
        )
    }
    observedLocationAvailable?.let { locationAvailable ->
        Text(
            text = stringResource(
                R.string.report_closure_performance_device_location,
                if (locationAvailable) {
                    stringResource(R.string.value_yes)
                } else {
                    stringResource(R.string.value_no)
                }
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    observedSignalsCapturedAtEpochMillis?.let { capturedAt ->
        Text(
            text = stringResource(
                R.string.report_closure_performance_device_captured_at,
                formatPerformanceEpoch(capturedAt)
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
