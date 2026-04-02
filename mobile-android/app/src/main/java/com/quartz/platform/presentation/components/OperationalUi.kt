package com.quartz.platform.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    modifier: Modifier = Modifier
) {
    if (signals.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        signals.take(3).forEach { signal ->
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        text = signal.text,
                        color = signalColor(signal.severity)
                    )
                }
            )
        }
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
private fun signalColor(severity: OperationalSeverity) = when (severity) {
    OperationalSeverity.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
    OperationalSeverity.SUCCESS -> MaterialTheme.colorScheme.primary
    OperationalSeverity.WARNING -> MaterialTheme.colorScheme.tertiary
    OperationalSeverity.CRITICAL -> MaterialTheme.colorScheme.error
}
