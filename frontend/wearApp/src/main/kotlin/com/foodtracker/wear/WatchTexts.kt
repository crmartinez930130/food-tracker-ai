package com.foodtracker.wear

import android.content.Context
import com.foodtracker.app.ui.home.cacheAge
import com.foodtracker.app.util.todayDateString
import com.foodtracker.app.widget.TodayWidgetState
import com.foodtracker.app.widget.WidgetFreshness
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlin.math.roundToInt

/** What the screen and the tile say about today, resolved in [context]'s language (see [localized]). */
data class WatchTexts(
    val title: String,
    val target: String,
    val leftOrOver: String,
    val protein: String,
    val fat: String,
    val carbs: String,
    val water: String,
    val note: String?,
)

fun watchTexts(context: Context, state: TodayWidgetState): WatchTexts {
    val progress = state.calorieProgress
    return WatchTexts(
        title = context.getString(R.string.today_title),
        target = context.getString(R.string.kcal_of_goal, state.targetCalories),
        leftOrOver = if (progress.isOver) {
            context.getString(R.string.kcal_over, progress.excess.roundToInt())
        } else {
            context.getString(R.string.kcal_left, progress.remaining.roundToInt())
        },
        protein = context.getString(R.string.macro_protein, state.proteinGrams, state.targetProteinGrams),
        fat = context.getString(R.string.macro_fat, state.fatGrams, state.targetFatGrams),
        carbs = context.getString(R.string.macro_carbs, state.carbsGrams, state.targetCarbsGrams),
        water = context.getString(R.string.water_progress, state.waterMl, state.targetWaterMl),
        note = when (val freshness = state.freshness) {
            WidgetFreshness.Fresh -> null
            WidgetFreshness.NoData -> context.getString(R.string.no_data)
            is WidgetFreshness.Stale -> {
                val age = cacheAge(freshness.since, LocalDate.parse(todayDateString()))
                val date = age.date
                if (date == null) {
                    context.getString(R.string.data_from, age.time)
                } else {
                    context.getString(R.string.data_from_other_day, "${date.day}/${date.month.number}", age.time)
                }
            }
        },
    )
}
