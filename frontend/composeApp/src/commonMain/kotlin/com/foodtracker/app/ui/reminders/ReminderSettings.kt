package com.foodtracker.app.ui.reminders

import com.foodtracker.app.data.MealEntry
import com.foodtracker.app.data.MealType
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.reminder_title_breakfast
import food_tracker_frontend.composeapp.generated.resources.reminder_title_dinner
import food_tracker_frontend.composeapp.generated.resources.reminder_title_lunch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.StringResource
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** One meal's daily reminder, at [hour]:[minute] local time. */
@Serializable
data class MealReminder(
    val meal: MealType,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = false,
)

@Serializable
data class ReminderSettings(val reminders: List<MealReminder>) {
    fun reminderFor(meal: MealType): MealReminder = reminders.first { it.meal == meal }

    fun with(updated: MealReminder): ReminderSettings =
        copy(reminders = reminders.map { if (it.meal == updated.meal) updated else it })

    companion object {
        /** Opt-in: every reminder starts off (see the add-meal-reminders design). */
        val DEFAULT = ReminderSettings(
            listOf(
                MealReminder(MealType.BREAKFAST, 8, 30),
                MealReminder(MealType.LUNCH, 13, 0),
                MealReminder(MealType.DINNER, 20, 0),
            ),
        )
    }
}

/** The meals that can have a reminder, in display order (no snacks). */
val REMINDER_MEALS: List<MealType> = ReminderSettings.DEFAULT.reminders.map { it.meal }

private val reminderJson = Json { ignoreUnknownKeys = true }

fun encodeReminderSettings(settings: ReminderSettings): String = reminderJson.encodeToString(settings)

/**
 * Stored settings, or the defaults (all off) for anything missing or unreadable - a corrupt value
 * must never crash the app or, worse, leave a reminder on that the user didn't turn on.
 */
fun decodeReminderSettings(stored: String?): ReminderSettings {
    val parsed = stored?.let { runCatching { reminderJson.decodeFromString<ReminderSettings>(it) }.getOrNull() }
    return ReminderSettings(
        ReminderSettings.DEFAULT.reminders.map { default ->
            parsed?.reminders
                ?.firstOrNull { it.meal == default.meal && it.hour in 0..23 && it.minute in 0..59 }
                ?: default
        },
    )
}

/**
 * The next time a reminder at [hour]:[minute] local time should fire: today if that's still
 * strictly ahead of [now] (so an alarm firing on the dot schedules tomorrow, not itself again),
 * otherwise tomorrow. A local time skipped by a DST jump resolves to the next valid instant.
 */
@OptIn(ExperimentalTime::class)
fun nextTriggerAt(now: Instant, hour: Int, minute: Int, timeZone: TimeZone): Instant {
    val time = LocalTime(hour, minute)
    val today = now.toLocalDateTime(timeZone).date
    val todayAt = LocalDateTime(today, time).toInstant(timeZone)
    return if (todayAt > now) todayAt else LocalDateTime(today.plus(1, DateTimeUnit.DAY), time).toInstant(timeZone)
}

/**
 * Whether a firing reminder for [meal] should notify. [mealsToday] is null when today's meals
 * couldn't be fetched in time: then remind anyway - a spurious nudge beats a missed one. Untagged
 * meals can't be attributed to breakfast/lunch/dinner, so they never suppress a reminder.
 * [queuedToday] are the types of today's meals still waiting to be sent (offline-queue): logged too.
 */
fun shouldShowReminder(meal: MealType, mealsToday: List<MealEntry>?, queuedToday: List<MealType?> = emptyList()): Boolean =
    meal !in queuedToday && (mealsToday == null || mealsToday.none { it.mealType == meal })

/** The notification's question for [meal], e.g. "¿Ya registraste tu almuerzo?" (only reminder meals). */
fun reminderTitle(meal: MealType): StringResource = when (meal) {
    MealType.BREAKFAST -> Res.string.reminder_title_breakfast
    MealType.LUNCH -> Res.string.reminder_title_lunch
    MealType.DINNER -> Res.string.reminder_title_dinner
    MealType.SNACK -> error("Snacks have no reminder")
}

fun timeLabel(hour: Int, minute: Int): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
