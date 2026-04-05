package com.quartz.platform.presentation.report.draft

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quartz.platform.R
import com.quartz.platform.data.remote.simulation.SyncSimulationMode
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.ReportSyncTrace
import com.quartz.platform.presentation.performance.session.formatPerformanceEpoch
import com.quartz.platform.presentation.sync.syncStateLabelRes

@Composable
internal fun DebugLiveSyncTraceSnapshotCard(trace: ReportSyncTrace) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.debug_header_live_sync_snapshot),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(
                    R.string.label_sync_state,
                    stringResource(syncStateLabelRes(trace.state))
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.label_retry_count, trace.retryCount),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = trace.lastAttemptAtEpochMillis?.let { lastAttempt ->
                    stringResource(R.string.label_last_attempt, formatPerformanceEpoch(lastAttempt))
                } ?: stringResource(R.string.label_last_attempt_none),
                style = MaterialTheme.typography.bodySmall
            )
            if (!trace.failureReason.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.label_failure_reason, trace.failureReason),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (trace.state == ReportSyncState.FAILED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
internal fun DebugSyncSimulationCard(
    mode: SyncSimulationMode,
    onModeSelected: (SyncSimulationMode) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.debug_header_sync_simulation),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(R.string.debug_label_mode_active, mode.name),
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = mode != SyncSimulationMode.NORMAL_SUCCESS,
                onClick = { onModeSelected(SyncSimulationMode.NORMAL_SUCCESS) }
            ) {
                Text(stringResource(R.string.debug_action_force_normal_success))
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = mode != SyncSimulationMode.FAIL_NEXT_RETRYABLE,
                onClick = { onModeSelected(SyncSimulationMode.FAIL_NEXT_RETRYABLE) }
            ) {
                Text(stringResource(R.string.debug_action_fail_next_retryable))
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = mode != SyncSimulationMode.FAIL_ONCE_THEN_SUCCESS,
                onClick = { onModeSelected(SyncSimulationMode.FAIL_ONCE_THEN_SUCCESS) }
            ) {
                Text(stringResource(R.string.debug_action_fail_once_then_success))
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = mode != SyncSimulationMode.FAIL_NEXT_TERMINAL,
                onClick = { onModeSelected(SyncSimulationMode.FAIL_NEXT_TERMINAL) }
            ) {
                Text(stringResource(R.string.debug_action_fail_next_terminal))
            }
        }
    }
}

@Composable
internal fun DebugSyncDemoScriptCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.debug_header_demo_script),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(R.string.debug_demo_success_title),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.debug_demo_success_body),
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = stringResource(R.string.debug_demo_retryable_title),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.debug_demo_retryable_body),
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = stringResource(R.string.debug_demo_terminal_title),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.debug_demo_terminal_body),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
internal fun SyncTraceabilityDetails(trace: ReportSyncTrace) {
    trace.lastAttemptAtEpochMillis?.let { lastAttempt ->
        Text(
            text = stringResource(R.string.label_last_attempt, formatPerformanceEpoch(lastAttempt)),
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (trace.retryCount > 0) {
        Text(
            text = stringResource(R.string.label_retry_count, trace.retryCount),
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (!trace.failureReason.isNullOrBlank()) {
        val message = if (trace.state == ReportSyncState.FAILED) {
            stringResource(R.string.label_local_failure_reason, trace.failureReason)
        } else {
            stringResource(R.string.label_last_failure_reason, trace.failureReason)
        }
        val color = if (trace.state == ReportSyncState.FAILED) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}
