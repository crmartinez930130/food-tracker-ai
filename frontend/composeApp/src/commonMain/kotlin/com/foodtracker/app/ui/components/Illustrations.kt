package com.foodtracker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Which empty state an [EmptyIllustration] decorates. */
enum class IllustrationKind { PLATE, SCALE, STAR }

private val star5: RoundedPolygon = RoundedPolygon.star(numVerticesPerRadius = 5, innerRadius = 0.48f, rounding = CornerRounding(0.18f))

/**
 * A small decorative drawing for an empty state, made for the app and drawn with the scheme's
 * colors so it follows light and dark. Decorative only: the text next to it carries the meaning.
 */
@Composable
fun EmptyIllustration(kind: IllustrationKind, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val badgePath = remember { Path() }
    val starPath = remember { Path() }
    Canvas(modifier.size(width = 160.dp, height = 120.dp)) {
        when (kind) {
            IllustrationKind.PLATE -> drawPlate(
                plate = colors.surfaceContainerHighest,
                rim = colors.outlineVariant,
                cutlery = colors.primary,
                sparkle = colors.tertiary,
            )
            IllustrationKind.SCALE -> drawScale(
                body = colors.tertiaryContainer,
                dial = colors.surface,
                marks = colors.onSurfaceVariant,
                needle = colors.tertiary,
            )
            IllustrationKind.STAR -> {
                val side = size.height * 0.92f
                translate((size.width - side) / 2f, (size.height - side) / 2f) {
                    drawPath(cubicsToPath(ExpressiveShapes.softBurst.cubics, Size(side, side), badgePath), colors.secondaryContainer)
                }
                val starSide = size.height * 0.44f
                translate((size.width - starSide) / 2f, (size.height - starSide) / 2f) {
                    drawPath(cubicsToPath(star5.cubics, Size(starSide, starSide), starPath), colors.primary)
                }
                drawSparkle(Offset(size.width * 0.84f, size.height * 0.2f), size.height * 0.07f, colors.tertiary)
            }
        }
    }
}

/** An empty plate between a fork and a knife, with a little sparkle. */
private fun DrawScope.drawPlate(plate: Color, rim: Color, cutlery: Color, sparkle: Color) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.height * 0.42f
    drawCircle(plate, radius, center)
    drawCircle(rim, radius * 0.72f, center, style = Stroke(width = 2.dp.toPx()))

    val handleWidth = size.height * 0.06f
    val top = size.height * 0.14f
    val bottom = size.height * 0.88f
    // Fork: three tines over a handle.
    val forkX = center.x - radius - size.height * 0.16f
    val tineWidth = handleWidth * 0.42f
    for (i in -1..1) {
        drawRoundRect(
            cutlery,
            topLeft = Offset(forkX + i * tineWidth * 1.6f - tineWidth / 2f, top),
            size = Size(tineWidth, size.height * 0.2f),
            cornerRadius = CornerRadius(tineWidth / 2f),
        )
    }
    drawRoundRect(
        cutlery,
        topLeft = Offset(forkX - handleWidth * 1.1f, top + size.height * 0.18f),
        size = Size(handleWidth * 2.2f, size.height * 0.07f),
        cornerRadius = CornerRadius(handleWidth),
    )
    drawRoundRect(
        cutlery,
        topLeft = Offset(forkX - handleWidth / 2f, top + size.height * 0.22f),
        size = Size(handleWidth, bottom - top - size.height * 0.22f),
        cornerRadius = CornerRadius(handleWidth / 2f),
    )
    // Knife: a wider rounded blade over a handle.
    val knifeX = center.x + radius + size.height * 0.16f
    drawRoundRect(
        cutlery,
        topLeft = Offset(knifeX - handleWidth * 0.8f, top),
        size = Size(handleWidth * 1.6f, size.height * 0.4f),
        cornerRadius = CornerRadius(handleWidth * 0.8f),
    )
    drawRoundRect(
        cutlery,
        topLeft = Offset(knifeX - handleWidth / 2f, top + size.height * 0.3f),
        size = Size(handleWidth, bottom - top - size.height * 0.3f),
        cornerRadius = CornerRadius(handleWidth / 2f),
    )
    drawSparkle(Offset(center.x + radius * 0.62f, center.y - radius * 0.62f), size.height * 0.07f, sparkle)
}

/** A bathroom scale: rounded body, a half dial with ticks and a needle. */
private fun DrawScope.drawScale(body: Color, dial: Color, marks: Color, needle: Color) {
    val side = size.height * 0.86f
    val left = (size.width - side) / 2f
    val top = (size.height - side) / 2f
    drawRoundRect(body, topLeft = Offset(left, top), size = Size(side, side), cornerRadius = CornerRadius(side * 0.22f))

    val hub = Offset(size.width / 2f, top + side * 0.5f)
    val dialRadius = side * 0.3f
    drawArc(dial, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = Offset(hub.x - dialRadius, hub.y - dialRadius), size = Size(dialRadius * 2, dialRadius * 2))
    val tick = 2.dp.toPx()
    listOf(-60.0, -90.0, -120.0).forEach { degrees ->
        val radians = degrees * PI / 180.0
        val outer = Offset(hub.x + (dialRadius * 0.86f * cos(radians)).toFloat(), hub.y + (dialRadius * 0.86f * sin(radians)).toFloat())
        val inner = Offset(hub.x + (dialRadius * 0.66f * cos(radians)).toFloat(), hub.y + (dialRadius * 0.66f * sin(radians)).toFloat())
        drawLine(marks, inner, outer, strokeWidth = tick, cap = StrokeCap.Round)
    }
    val needleRadians = -55.0 * PI / 180.0
    val tip = Offset(hub.x + (dialRadius * 0.8f * cos(needleRadians)).toFloat(), hub.y + (dialRadius * 0.8f * sin(needleRadians)).toFloat())
    drawLine(needle, hub, tip, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(needle, 4.dp.toPx(), hub)
    // The platform's pad, a soft line near the bottom.
    drawRoundRect(dial, topLeft = Offset(left + side * 0.2f, top + side * 0.72f), size = Size(side * 0.6f, side * 0.08f), cornerRadius = CornerRadius(side * 0.04f))
}

/** A four-point sparkle (two crossed, pinched diamonds). */
private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val pinch = radius * 0.28f
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x + pinch, center.y - pinch, center.x + radius, center.y)
        quadraticTo(center.x + pinch, center.y + pinch, center.x, center.y + radius)
        quadraticTo(center.x - pinch, center.y + pinch, center.x - radius, center.y)
        quadraticTo(center.x - pinch, center.y - pinch, center.x, center.y - radius)
        close()
    }
    drawPath(path, color)
}
