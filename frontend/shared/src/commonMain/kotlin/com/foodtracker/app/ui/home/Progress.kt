package com.foodtracker.app.ui.home

import kotlin.math.roundToInt

/**
 * How much of a daily target has been consumed.
 *
 * [fraction] is clamped to 0..1 so it can be drawn directly; [remaining] is how much is left
 * before reaching the target, and [excess] how far past it the day already is (one of them is 0).
 */
data class Progress(
    val fraction: Float,
    val isOver: Boolean,
    val remaining: Double,
    val excess: Double,
)

fun progressOf(consumed: Double, target: Double): Progress {
    val safeConsumed = consumed.coerceAtLeast(0.0)
    // A target of 0 can't be divided by: anything consumed counts as over, nothing as empty.
    if (target <= 0.0) {
        val over = safeConsumed > 0.0
        return Progress(
            fraction = if (over) 1f else 0f,
            isOver = over,
            remaining = 0.0,
            excess = safeConsumed,
        )
    }
    return Progress(
        fraction = (safeConsumed / target).coerceIn(0.0, 1.0).toFloat(),
        isOver = safeConsumed > target,
        remaining = (target - safeConsumed).coerceAtLeast(0.0),
        excess = (safeConsumed - target).coerceAtLeast(0.0),
    )
}

/**
 * [consumed] as a whole percentage of [target], not capped at 100 (a day can be at 112%); null
 * when the target isn't positive, since there's nothing to divide by.
 */
fun goalPercent(consumed: Double, target: Double): Int? =
    if (target <= 0.0) null else (consumed.coerceAtLeast(0.0) / target * 100).roundToInt()
