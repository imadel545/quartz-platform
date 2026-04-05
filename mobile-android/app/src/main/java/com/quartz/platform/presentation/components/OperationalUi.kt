package com.quartz.platform.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
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

data class OperationalMetric(
    val value: String,
    val label: String,
    val severity: OperationalSeverity = OperationalSeverity.NORMAL
)

@Composable
fun OperationalSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
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
    metrics: List<OperationalMetric> = emptyList(),
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
        if (metrics.isNotEmpty()) {
            OperationalMetricRow(metrics = metrics)
        }
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
@OptIn(ExperimentalLayoutApi::class)
fun OperationalMetricRow(
    metrics: List<OperationalMetric>,
    modifier: Modifier = Modifier
) {
    if (metrics.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        metrics.forEach { metric ->
            OperationalMetricBadge(metric = metric)
        }
    }
}

@Composable
fun OperationalEmptyStateCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    OperationalSectionCard(
        modifier = modifier,
        title = title
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        action?.invoke()
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
fun OperationalStateBanner(
    title: String,
    message: String,
    severity: OperationalSeverity,
    modifier: Modifier = Modifier,
    hint: String? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = severityContainerColor(severity)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = signalColor(severity)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MissionPrimaryActionBar(
    modifier: Modifier = Modifier,
    primaryAction: @Composable () -> Unit,
    secondaryAction: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        primaryAction()
        secondaryAction?.invoke()
    }
}

@Composable
fun MissionPrimaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        onClick = onClick
    ) {
        Text(label)
    }
}

@Composable
private fun OperationalSignalBadge(signal: OperationalSignal) {
    Surface(
        color = severityContainerColor(signal.severity),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            text = signal.text,
            style = MaterialTheme.typography.labelMedium,
            color = signalColor(signal.severity)
        )
    }
}

@Composable
private fun OperationalMetricBadge(metric: OperationalMetric) {
    Surface(
        color = severityContainerColor(metric.severity),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 88.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = metric.value,
                style = MaterialTheme.typography.titleSmall,
                color = signalColor(metric.severity)
            )
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun signalColor(severity: OperationalSeverity) = when (severity) {
    OperationalSeverity.NORMAL -> MaterialTheme.colorScheme.onSurface
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
