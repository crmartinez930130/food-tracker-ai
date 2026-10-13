package com.foodtracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** One part of a [SegmentedBar]; [detail] is shown after the label in the legend ("45%", "120 g"). */
data class BarSegment(val label: String, val value: Double, val color: Color, val detail: String? = null)

/**
 * Each value's share of the total, in order. Negative values count as zero; when nothing is
 * positive every share is zero (an empty bar, not an even split).
 */
fun segmentFractions(values: List<Double>): List<Float> {
    val clamped = values.map { it.coerceAtLeast(0.0) }
    val total = clamped.sum()
    if (total <= 0.0) return clamped.map { 0f }
    return clamped.map { (it / total).toFloat() }
}

/** Proportional colored segments with a dot legend - Greenwave's "Serene" bar. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SegmentedBar(
    segments: List<BarSegment>,
    modifier: Modifier = Modifier,
    showLegend: Boolean = true,
) {
    val fractions = segmentFractions(segments.map { it.value })
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(6.dp)),
        ) {
            segments.zip(fractions).filter { (_, fraction) -> fraction > 0f }.forEach { (segment, fraction) ->
                Box(
                    Modifier
                        .weight(fraction)
                        .height(12.dp)
                        .background(segment.color, RoundedCornerShape(6.dp)),
                )
            }
        }
        if (showLegend) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                segments.forEach { segment ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Box(Modifier.size(8.dp).background(segment.color, CircleShape))
                        Text(
                            segment.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                        segment.detail?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
