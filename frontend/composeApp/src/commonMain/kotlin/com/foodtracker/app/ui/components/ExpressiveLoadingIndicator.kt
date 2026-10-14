package com.foodtracker.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import kotlin.math.floor

/** Where an endless morph through `count` shapes is: from which shape, to which, and how far. */
data class MorphStep(val from: Int, val to: Int, val progress: Float)

/**
 * Maps an ever-growing [phase] (1.0 per shape) to the pair of shapes being morphed. Wraps around
 * the sequence; [progress] stays in 0..1 and eases in and out so each shape briefly settles.
 */
fun morphStep(phase: Float, count: Int): MorphStep {
    require(count > 0) { "Need at least one shape" }
    val whole = floor(phase)
    val fraction = phase - whole
    val from = (((whole.toInt() % count) + count) % count)
    val eased = fraction * fraction * (3f - 2f * fraction)
    return MorphStep(from = from, to = (from + 1) % count, progress = eased.coerceIn(0f, 1f))
}

/**
 * Material 3 Expressive-style loading indicator: a shape that morphs through [ExpressiveShapes]
 * while turning. Indeterminate; announced as a progress bar for accessibility.
 */
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color? = null,
) {
    if (LocalReduceMotion.current) {
        StillIndicator(modifier, size, color, containerColor)
        return
    }
    val shapes = ExpressiveShapes.loadingSequence
    val morphs = remember { shapes.indices.map { Morph(shapes[it], shapes[(it + 1) % shapes.size]) } }
    val transition = rememberInfiniteTransition(label = "expressive-loading")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = shapes.size.toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = 650 * shapes.size, easing = LinearEasing), RepeatMode.Restart),
        label = "morph-phase",
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2600, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation",
    )
    val path = remember { Path() }

    Canvas(modifier.size(size).semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }) {
        containerColor?.let { drawCircle(it) }
        val step = morphStep(phase, shapes.size)
        val inset = this.size.minDimension * if (containerColor != null) 0.2f else 0.04f
        val inner = Size(this.size.width - 2 * inset, this.size.height - 2 * inset)
        cubicsToPath(morphs[step.from].asCubics(step.progress), inner, path)
        rotate(rotation) {
            translate(inset, inset) { drawPath(path, color) }
        }
    }
}

/** Under reduce motion: the same indicator, standing still on the cookie shape. */
@Composable
private fun StillIndicator(modifier: Modifier, size: Dp, color: Color, containerColor: Color?) {
    val path = remember { Path() }
    Canvas(modifier.size(size).semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }) {
        containerColor?.let { drawCircle(it) }
        val inset = this.size.minDimension * if (containerColor != null) 0.2f else 0.04f
        val inner = Size(this.size.width - 2 * inset, this.size.height - 2 * inset)
        cubicsToPath(ExpressiveShapes.cookie9.cubics, inner, path)
        translate(inset, inset) { drawPath(path, color) }
    }
}
