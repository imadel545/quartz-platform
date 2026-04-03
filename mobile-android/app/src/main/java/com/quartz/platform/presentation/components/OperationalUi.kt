package com.quartz.platform.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class OperationalSeverity {
    NORMAL,
    SUCCESS,
    WARNING,
    CRITICAL
}

data class OperationalSignal(
    val text: String,
    val severity: OperationalSeverity = OperationalSeverity.NORMAL
)

@Composable
fun OperationalSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun OperationalSignalRow(
    signals: List<OperationalSignal>,
    modifier: Modifier = Modifier,
    maxVisibleSignals: Int = 3
) {
    if (signals.isEmpty()) return
    val clampedMax = maxVisibleSignals.coerceAtLeast(1)
    val visibleSignals = signals.take(clampedMax)
    val remaining = (signals.size - visibleSignals.size).coerceAtLeast(0)
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        visibleSignals.forEach { signal ->
            OperationalSignalBadge(signal = signal)
        }
        if (remaining > 0) {
            OperationalSignalBadge(
                signal = OperationalSignal(
                    text = "+$remaining",
                    severity = OperationalSeverity.NORMAL
                )
            )
        }
    }
}

@Composable
fun OperationalMessageCard(
    title: String,
    message: String,
    severity: OperationalSeverity,
    modifier: Modifier = Modifier
) {
    val containerColor = severityContainerColor(severity)
    val contentColor = signalColor(severity)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
        }
    }
}

@Composable
fun MissionHeaderCard(
    title: String,
    subtitle: String,
    signals: List<OperationalSignal>,
    modifier: Modifier = Modifier,
    primaryAction: (@Composable () -> Unit)? = null,
    secondaryActions: @Composable (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    OperationalSectionCard(
        modifier = modifier,
        title = title,
        subtitle = subtitle
    ) {
        OperationalSignalRow(signals = signals)
        if (primaryAction != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                primaryAction()
            }
        }
        secondaryActions?.invoke()
        content?.invoke()
    }
}

@Composable
fun AdvancedDisclosureButton(
    expanded: Boolean,
    onToggle: () -> Unit,
    showLabel: String,
    hideLabel: String,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        modifier = modifier.fillMaxWidth(),
        onClick = onToggle
    ) {
        Text(if (expanded) hideLabel else showLabel)
    }
}

@Composable
private fun OperationalSignalBadge(signal: OperationalSignal) {
    Surface(
        color = severityContainerColor(signal.severity),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            text = signal.text,
            style = MaterialTheme.typography.labelMedium,
            color = signalColor(signal.severity)
        )
    }
}

@Composable
private fun signalColor(severity: OperationalSeverity) = when (severity) {
    OperationalSeverity.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
    OperationalSeverity.SUCCESS -> MaterialTheme.colorScheme.primary
    OperationalSeverity.WARNING -> MaterialTheme.colorScheme.tertiary
    OperationalSeverity.CRITICAL -> MaterialTheme.colorScheme.error
}

@Composable
private fun severityContainerColor(severity: OperationalSeverity): Color = when (severity) {
    OperationalSeverity.NORMAL -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    OperationalSeverity.SUCCESS -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    OperationalSeverity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
    OperationalSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
}
