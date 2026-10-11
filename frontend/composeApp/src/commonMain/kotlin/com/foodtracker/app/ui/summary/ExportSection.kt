package com.foodtracker.app.ui.summary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.ui.rememberFileSharer
import kotlinx.coroutines.launch

/**
 * "Export CSV": picks a date range (pre-selected with the period Summary is showing) and hands the
 * backend's CSV for it to the platform share sheet.
 */
@Composable
fun ExportSection(
    apiClient: FoodTrackerApiClient,
    periodStart: String,
    periodEnd: String,
    today: String,
    modifier: Modifier = Modifier,
) {
    val sharer = rememberFileSharer()
    val scope = rememberCoroutineScope()
    var showPicker by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { error = null; showPicker = true },
            enabled = !isExporting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isExporting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Exporting…")
            } else {
                Text("Export CSV")
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
    }

    if (showPicker) {
        ExportRangeDialog(
            initialStart = periodStart,
            initialEnd = periodEnd,
            today = today,
            onDismiss = { showPicker = false },
            onConfirm = { start, end ->
                showPicker = false
                scope.launch {
                    isExporting = true
                    try {
                        val csv = apiClient.exportMealsCsv(start, end)
                        sharer.share(exportFileName(start, end), "text/csv", csv)
                    } catch (e: Exception) {
                        error = "Couldn't export: ${e.message ?: "unknown error"}"
                    } finally {
                        isExporting = false
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportRangeDialog(
    initialStart: String,
    initialEnd: String,
    today: String,
    onDismiss: () -> Unit,
    onConfirm: (start: String, end: String) -> Unit,
) {
    val todayMillis = dateToPickerMillis(today)
    val todayYear = today.take(4).toInt()
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = dateToPickerMillis(initialStart),
        initialSelectedEndDateMillis = dateToPickerMillis(initialEnd),
        yearRange = 2020..todayYear,
        selectableDates = remember(todayMillis) {
            object : SelectableDates {
                // No meals can exist after today.
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
                override fun isSelectableYear(year: Int) = year <= todayYear
            }
        },
    )
    val start = state.selectedStartDateMillis?.let(::pickerMillisToDate)
    val end = state.selectedEndDateMillis?.let(::pickerMillisToDate)
    val rangeError = exportRangeError(start, end)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(start!!, end!!) }, enabled = rangeError == null) { Text("Export") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DateRangePicker(
            state = state,
            // The title slot (above the selected-range headline) explains why Export is disabled.
            title = {
                Text(
                    rangeError ?: "Export meals in this range",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (rangeError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
            showModeToggle = false,
            modifier = Modifier.weight(1f),
        )
    }
}
