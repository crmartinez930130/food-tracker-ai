package com.foodtracker.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.roundToInt

/**
 * True when the system asks to remove animations: every effect of ours then shows its final state
 * at once. Provided by App from the platform setting.
 */
val LocalReduceMotion = compositionLocalOf { false }

/** Android's "Remove animations" sets the animator duration scale to 0. */
fun animationsDisabled(animatorDurationScale: Float): Boolean = animatorDurationScale == 0f

/** [target], counting up to it from 0 on first show and to each new value after (snaps under reduce motion). */
@Composable
fun animatedCount(target: Int): Int {
    val reduceMotion = LocalReduceMotion.current
    val value = remember { Animatable(if (reduceMotion) target.toFloat() else 0f) }
    LaunchedEffect(target, reduceMotion) {
        if (reduceMotion) {
            value.snapTo(target.toFloat())
        } else {
            value.animateTo(target.toFloat(), tween(durationMillis = 700, easing = FastOutSlowInEasing))
        }
    }
    return value.value.roundToInt()
}

/** [target] (0..1), growing from 0 on first show and moving to each new value after, with a soft spring. */
@Composable
fun animatedFraction(target: Float): Float {
    val reduceMotion = LocalReduceMotion.current
    val value = remember { Animatable(if (reduceMotion) target else 0f) }
    LaunchedEffect(target, reduceMotion) {
        if (reduceMotion) {
            value.snapTo(target)
        } else {
            value.animateTo(target, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
        }
    }
    return value.value
}

/** Shrinks slightly while pressed and springs back on release - the expressive "button reacts" feel. */
fun Modifier.pressBounce(interactionSource: InteractionSource): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
        label = "pressBounce",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Which shape the calorie badge takes. */
enum class BadgeShape { IN_PROGRESS, GOAL_REACHED }

/** The cookie until the day reaches 100% of the goal, then the sunburst. */
fun badgeShapeFor(percent: Int?): BadgeShape =
    if (percent != null && percent >= 100) BadgeShape.GOAL_REACHED else BadgeShape.IN_PROGRESS
