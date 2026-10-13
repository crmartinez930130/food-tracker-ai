package com.foodtracker.app.ui.summary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.WeightEntry
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.weight_chart_description
import food_tracker_frontend.composeapp.generated.resources.weight_chart_empty
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

/**
 * Weight over the period as a smooth curve through each day's last entry. Uses the same day slots as
 * [CalorieChart] (one slot per day of [periodDays], full width), so a point sits right under that
 * day's calorie bar. Days without an entry are skipped, with the line joining across them.
 */
@Composable
fun WeightChart(periodDays: List<String>, points: List<WeightEntry>, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val scale = weightScale(points.map { it.weightKg })
    val description = weightChartDescription(points)

    Canvas(modifier = modifier.fillMaxWidth().height(140.dp).semantics { contentDescription = description }) {
        if (periodDays.isEmpty() || points.isEmpty()) return@Canvas
        val slot = size.width / periodDays.size
        val top = 8.dp.toPx()
        val bottom = size.height - 8.dp.toPx()
        val span = (scale.endInclusive - scale.start).takeIf { it > 0 } ?: 1.0
        val dayIndex = periodDays.withIndex().associate { (index, day) -> day to index }

        fun yFor(kg: Double): Float = (bottom - (bottom - top) * ((kg - scale.start) / span)).toFloat()

        val positions = points.mapNotNull { point ->
            dayIndex[point.day]?.let { index -> Offset(index * slot + slot / 2f, yFor(point.weightKg)) }
        }

        // Range labels on the left: the axis isn't zero-based, so show where it starts and ends.
        listOf(scale.endInclusive to top, scale.start to bottom).forEach { (kg, y) ->
            val label = textMeasurer.measure("${kg.roundToInt()} kg", labelStyle)
            drawText(label, topLeft = Offset(0f, (y - label.size.height / 2f).coerceIn(0f, size.height - label.size.height)))
        }

        if (positions.size >= 2) {
            val line = Path().apply {
                moveTo(positions.first().x, positions.first().y)
                smoothControlPoints(positions).forEach { segment ->
                    cubicTo(segment.control1.x, segment.control1.y, segment.control2.x, segment.control2.y, segment.end.x, segment.end.y)
                }
            }
            // A soft fill under the curve, fading toward the bottom.
            val fill = Path().apply {
                addPath(line)
                lineTo(positions.last().x, bottom)
                lineTo(positions.first().x, bottom)
                close()
            }
            drawPath(fill, brush = Brush.verticalGradient(listOf(colors.primary.copy(alpha = 0.2f), Color.Transparent), startY = top, endY = bottom))
            drawPath(line, color = colors.primary, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
        positions.forEach { drawCircle(color = colors.primary, radius = 4.dp.toPx(), center = it) }
    }
}

@Composable
private fun weightChartDescription(points: List<WeightEntry>): String {
    if (points.isEmpty()) return stringResource(Res.string.weight_chart_empty)
    val first = points.first()
    val last = points.last()
    val change = last.weightKg - first.weightKg
    return stringResource(
        Res.string.weight_chart_description,
        first.day,
        last.day,
        formatKg(first.weightKg),
        formatKg(last.weightKg),
        formatKgChange(change),
    )
}

/** One decimal, e.g. 72.4 kg. */
fun formatKg(kg: Double): String = "${kgNumber(kg)} kg"

/** The number alone, one decimal: 72.4. */
fun kgNumber(kg: Double): String = "${(kg * 10).roundToInt() / 10.0}"

/** Signed with one decimal, e.g. +0.6 kg / −0.8 kg / ±0 kg. */
fun formatKgChange(change: Double): String {
    val tenths = (change * 10).roundToInt()
    return when {
        tenths > 0 -> "+${tenths / 10.0} kg"
        tenths < 0 -> "−${-tenths / 10.0} kg"
        else -> "±0 kg"
    }
}
