package com.foodtracker.app.ui.summary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.WeightEntry

/**
 * Weight for the Summary period: latest value and change, the trend chart (aligned with the
 * calorie chart's days) and the period's entries. Stateless apart from the log dialog's field;
 * loading and saving belong to the caller.
 */
@Composable
fun WeightSection(
    periodDays: List<String>,
    entries: List<WeightEntry>,
    onLog: (weightKg: Double) -> Unit,
    onDelete: (WeightEntry) -> Unit,
    lastKnownWeight: Double?,
    modifier: Modifier = Modifier,
) {
    val perDay = latestPerDay(entries)
    var showLogDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Weight", style = MaterialTheme.typography.titleMedium)
                if (perDay.isNotEmpty()) {
                    val change = perDay.last().weightKg - perDay.first().weightKg
                    Text(
                        "${formatKg(perDay.last().weightKg)} · ${formatKgChange(change)} this period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FilledTonalButton(onClick = { showLogDialog = true }) { Text("Log weight") }
        }

        Spacer(Modifier.height(8.dp))
        if (perDay.isEmpty()) {
            Text(
                "No weight logged in this period. Log today's weight to start your trend.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            WeightChart(periodDays = periodDays, points = perDay)
            Spacer(Modifier.height(8.dp))
            entries.asReversed().forEach { entry ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(entry.day, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(formatKg(entry.weightKg), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { onDelete(entry) }) { Text("Delete") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    if (showLogDialog) {
        LogWeightDialog(
            initialValue = lastKnownWeight,
            onSave = { kg ->
                showLogDialog = false
                onLog(kg)
            },
            onDismiss = { showLogDialog = false },
        )
    }
}

/** Same bounds the backend enforces, checked here first for a friendlier message. */
internal fun parseWeightInput(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 && it <= 500.0 }

@Composable
private fun LogWeightDialog(initialValue: Double?, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initialValue?.let { formatKg(it).removeSuffix(" kg") } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log today's weight") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    error = null
                },
                label = { Text("Weight (kg)") },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { message -> { Text(message) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val kg = parseWeightInput(text)
                if (kg == null) error = "Enter a weight between 0 and 500 kg." else onSave(kg)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
