package com.foodtracker.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.WaterDay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlin.time.ExperimentalTime

fun waterLabel(totalMl: Int, targetMl: Int): String = "$totalMl / $targetMl ml"

/**
 * When a glass logged while Home shows [date] should count: null (the backend's "now") for today,
 * and noon local time for a past day - unambiguous across DST changes and well inside the day.
 */
@OptIn(ExperimentalTime::class)
fun loggedAtFor(date: String, today: String, timeZone: TimeZone = TimeZone.currentSystemDefault()): String? =
    if (date == today) null else LocalDate.parse(date).atTime(LocalTime(12, 0)).toInstant(timeZone).toString()

/** The day's water against the target, with one-tap "add a glass" and "undo the last entry". */
@Composable
fun WaterCard(
    water: WaterDay?,
    targetMl: Int,
    glassMl: Int,
    onAddGlass: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalMl = water?.totalMl ?: 0
    val progress = progressOf(totalMl.toDouble(), targetMl.toDouble())
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Water", style = MaterialTheme.typography.labelLarge)
            Text(
                waterLabel(totalMl, targetMl),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        // Going past the water target is fine, so unlike the macros it never turns red.
        LinearProgressIndicator(
            progress = { progress.fraction },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
        )
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            TextButton(onClick = onUndo, enabled = !water?.entries.isNullOrEmpty()) { Text("Undo") }
            FilledTonalButton(onClick = onAddGlass, enabled = water != null) { Text("+ $glassMl ml") }
        }
    }
}
