package com.foodtracker.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.WaterDay
import com.foodtracker.app.ui.components.BigNumber
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.water_add_glass
import food_tracker_frontend.composeapp.generated.resources.water_title
import food_tracker_frontend.composeapp.generated.resources.water_undo
import kotlin.time.ExperimentalTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.stringResource

/** The unit after the big water figure: "/ 2000 ml". */
fun waterTargetLabel(targetMl: Int): String = "/ $targetMl ml"

/**
 * When a glass logged while showing [date] should count: null (the backend's "now") for today,
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
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.tertiaryContainer, contentColor = colors.onTertiaryContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(Res.string.water_title), style = MaterialTheme.typography.titleMedium)
            BigNumber(
                value = "$totalMl",
                unit = waterTargetLabel(targetMl),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                unitStyle = MaterialTheme.typography.titleSmall,
                unitColor = colors.onTertiaryContainer,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(12.dp))
            // Going past the water target is fine, so unlike the macros it never turns red.
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = colors.tertiary,
                trackColor = colors.surface,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                TextButton(
                    onClick = onUndo,
                    enabled = !water?.entries.isNullOrEmpty(),
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.onTertiaryContainer),
                ) { Text(stringResource(Res.string.water_undo)) }
                FilledTonalButton(
                    onClick = onAddGlass,
                    enabled = water != null,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.tertiary, contentColor = colors.onTertiary),
                ) { Text(stringResource(Res.string.water_add_glass, glassMl)) }
            }
        }
    }
}
