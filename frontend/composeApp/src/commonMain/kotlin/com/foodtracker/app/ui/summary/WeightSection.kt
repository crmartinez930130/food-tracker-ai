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
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.action_cancel
import food_tracker_frontend.composeapp.generated.resources.action_delete
import food_tracker_frontend.composeapp.generated.resources.action_save
import food_tracker_frontend.composeapp.generated.resources.weight_dialog_title
import food_tracker_frontend.composeapp.generated.resources.weight_empty
import food_tracker_frontend.composeapp.generated.resources.weight_error_range
import food_tracker_frontend.composeapp.generated.resources.weight_field
import food_tracker_frontend.composeapp.generated.resources.weight_latest
import food_tracker_frontend.composeapp.generated.resources.weight_log
import food_tracker_frontend.composeapp.generated.resources.weight_title
import org.jetbrains.compose.resources.stringResource

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
                Text(stringResource(Res.string.weight_title), style = MaterialTheme.typography.titleMedium)
                if (perDay.isNotEmpty()) {
                    val change = perDay.last().weightKg - perDay.first().weightKg
                    Text(
                        stringResource(Res.string.weight_latest, formatKg(perDay.last().weightKg), formatKgChange(change)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FilledTonalButton(onClick = { showLogDialog = true }) { Text(stringResource(Res.string.weight_log)) }
        }

        Spacer(Modifier.height(8.dp))
        if (perDay.isEmpty()) {
            Text(
                stringResource(Res.string.weight_empty),
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
                    TextButton(onClick = { onDelete(entry) }) { Text(stringResource(Res.string.action_delete)) }
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
    var invalid by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.weight_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    invalid = false
                },
                label = { Text(stringResource(Res.string.weight_field)) },
                singleLine = true,
                isError = invalid,
                supportingText = if (invalid) {
                    { Text(stringResource(Res.string.weight_error_range)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val kg = parseWeightInput(text)
                if (kg == null) invalid = true else onSave(kg)
            }) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
