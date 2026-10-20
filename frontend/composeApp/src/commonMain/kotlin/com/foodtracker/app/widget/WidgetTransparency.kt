package com.foodtracker.app.widget

import kotlin.math.roundToInt

/** How finely the configuration slider moves the background transparency, in percentage points. */
const val TRANSPARENCY_STEP = 5

/** A stored transparency kept within 0 (opaque) to 100 (no background), whatever was saved. */
fun clampTransparency(percent: Int): Int = percent.coerceIn(0, 100)

/** The slider's raw position as a transparency on a [TRANSPARENCY_STEP] step. */
fun snapTransparency(raw: Float): Int = clampTransparency((raw / TRANSPARENCY_STEP).roundToInt() * TRANSPARENCY_STEP)

/** The background's alpha for a transparency: 0% is fully opaque (1), 100% fully clear (0). */
fun backgroundAlpha(percent: Int): Float = (100 - clampTransparency(percent)) / 100f
