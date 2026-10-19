package com.foodtracker.app.widget

import android.content.Context
import androidx.glance.material3.ColorProviders
import com.foodtracker.app.data.LogWaterRequest
import com.foodtracker.app.data.cache.OutboxSync
import com.foodtracker.app.data.cache.SaveOutcome
import com.foodtracker.app.data.cache.cacheDatabase
import com.foodtracker.app.data.cache.epochMillisNow
import com.foodtracker.app.data.cache.storedSettingsDayRepository
import com.foodtracker.app.data.cache.storedSettingsOutbox
import com.foodtracker.app.ui.home.cacheAge
import com.foodtracker.app.ui.settings.PreferencesStore
import com.foodtracker.app.ui.settings.applyAppLocale
import com.foodtracker.app.ui.settings.effectiveServerUrl
import com.foodtracker.app.ui.theme.DarkColors
import com.foodtracker.app.ui.theme.LightColors
import com.foodtracker.app.util.currentInstantIso
import com.foodtracker.app.util.todayDateString
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.calorie_ring_left
import food_tracker_frontend.composeapp.generated.resources.calorie_ring_of_target
import food_tracker_frontend.composeapp.generated.resources.calorie_ring_over
import food_tracker_frontend.composeapp.generated.resources.home_calories_title
import food_tracker_frontend.composeapp.generated.resources.macro_carbs
import food_tracker_frontend.composeapp.generated.resources.macro_fat
import food_tracker_frontend.composeapp.generated.resources.macro_progress_grams
import food_tracker_frontend.composeapp.generated.resources.macro_protein
import food_tracker_frontend.composeapp.generated.resources.month_short_names
import food_tracker_frontend.composeapp.generated.resources.water_add_glass
import food_tracker_frontend.composeapp.generated.resources.water_title
import food_tracker_frontend.composeapp.generated.resources.widget_data_from
import food_tracker_frontend.composeapp.generated.resources.widget_data_from_other_day
import food_tracker_frontend.composeapp.generated.resources.widget_log_meal
import food_tracker_frontend.composeapp.generated.resources.widget_no_data
import food_tracker_frontend.composeapp.generated.resources.widget_water_progress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray
import kotlin.math.roundToInt

/** The app's own color scheme for the widgets; Glance picks light or dark from the system. */
internal val WidgetColors = ColorProviders(light = LightColors, dark = DarkColors)

/**
 * Today's state from the phone's copy and queue, for the stored server (a widget may draw in a fresh
 * process), again whenever they change and on every redraw request, which re-reads the server, the date
 * and the clock (a new day, an older copy). Widgets collect it inside their content: a Glance session that
 * is still open only recomposes on update, so data read before composing would stay old.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun todayWidgetStates(context: Context): Flow<TodayWidgetState> {
    val database = cacheDatabase(context)
    return WidgetUpdates.redraws
        .flatMapLatest {
            val serverUrl = effectiveServerUrl(PreferencesStore.load(context))
            todayWidgetStates(database.dayCacheDao(), database.outboxDao(), serverUrl, todayDateString(), ::epochMillisNow)
        }
        .flowOn(Dispatchers.IO)
}

/**
 * Logs one glass of today's goal size through the offline queue, as Home's button does: sent at once
 * when the backend answers, queued (and a background send asked for) when it doesn't. The database
 * watcher then redraws the widgets: a queued glass changes the queue, and a sent one is followed by a
 * refresh of today, as Home does after its own glass.
 */
internal suspend fun logWidgetGlass(context: Context) = withContext(Dispatchers.IO) {
    val glassMl = todayWidgetStates(context).first().glassMl
    val preferences = PreferencesStore.load(context)
    val serverUrl = effectiveServerUrl(preferences)
    val database = cacheDatabase(context)
    val today = todayDateString()
    val (outbox, outboxClient) = storedSettingsOutbox(
        serverUrl,
        preferences.accessToken,
        database.outboxDao(),
        database.dayCacheDao(),
        requestSync = { OutboxSync.request(context) },
    )
    val (days, daysClient) = storedSettingsDayRepository(serverUrl, preferences.accessToken, database.dayCacheDao())
    try {
        // A refusal is returned, not queued: like Home, the widget leaves the day as it was.
        val outcome = outbox.logWater(LogWaterRequest(glassMl, loggedAt = currentInstantIso()), today)
        if (outcome is SaveOutcome.Saved) days.refresh(today)
    } finally {
        outboxClient.close()
        daysClient.close()
    }
}

/** The texts "Today's calories" draws, resolved in the app's language before composing. */
internal data class TodayTexts(
    val title: String,
    val target: String,
    val leftOrOver: String,
    val macros: String,
    val water: String,
    val waterTitle: String,
    val note: String?,
)

internal suspend fun todayTexts(context: Context, state: TodayWidgetState): TodayTexts {
    applyAppLocale(context)
    val progress = state.calorieProgress
    val macro: suspend (label: String, grams: Int, target: Int) -> String = { label, grams, target ->
        "$label ${getString(Res.string.macro_progress_grams, grams, target)}"
    }
    return TodayTexts(
        title = getString(Res.string.home_calories_title),
        target = getString(Res.string.calorie_ring_of_target, state.targetCalories),
        leftOrOver = if (progress.isOver) {
            getString(Res.string.calorie_ring_over, progress.excess.roundToInt())
        } else {
            getString(Res.string.calorie_ring_left, progress.remaining.roundToInt())
        },
        macros = listOf(
            macro(getString(Res.string.macro_protein), state.proteinGrams, state.targetProteinGrams),
            macro(getString(Res.string.macro_fat), state.fatGrams, state.targetFatGrams),
            macro(getString(Res.string.macro_carbs), state.carbsGrams, state.targetCarbsGrams),
        ).joinToString(" · "),
        water = getString(Res.string.widget_water_progress, state.waterMl, state.targetWaterMl),
        waterTitle = getString(Res.string.water_title),
        note = freshnessNote(state.freshness),
    )
}

private suspend fun freshnessNote(freshness: WidgetFreshness): String? = when (freshness) {
    WidgetFreshness.Fresh -> null
    WidgetFreshness.NoData -> getString(Res.string.widget_no_data)
    is WidgetFreshness.Stale -> {
        val age = cacheAge(freshness.since, LocalDate.parse(todayDateString()))
        val date = age.date
        if (date == null) {
            getString(Res.string.widget_data_from, age.time)
        } else {
            val month = getStringArray(Res.array.month_short_names)[date.month.number - 1]
            getString(Res.string.widget_data_from_other_day, "${date.day} $month", age.time)
        }
    }
}

/** The quick-actions widget's two labels, in the app's language. */
internal data class ActionTexts(val glass: String, val logMeal: String)

internal suspend fun actionTexts(context: Context, glassMl: Int): ActionTexts {
    applyAppLocale(context)
    return ActionTexts(glass = getString(Res.string.water_add_glass, glassMl), logMeal = getString(Res.string.widget_log_meal))
}
