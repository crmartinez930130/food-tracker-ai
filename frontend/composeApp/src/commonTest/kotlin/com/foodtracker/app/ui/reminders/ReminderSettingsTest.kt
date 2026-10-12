package com.foodtracker.app.ui.reminders

import com.foodtracker.app.data.MealEntry
import com.foodtracker.app.data.MealType
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.reminder_title_breakfast
import food_tracker_frontend.composeapp.generated.resources.reminder_title_dinner
import food_tracker_frontend.composeapp.generated.resources.reminder_title_lunch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class ReminderSettingsTest {

    private val madrid = TimeZone.of("Europe/Madrid")
    private fun at(local: String, zone: TimeZone = madrid): Instant = LocalDateTime.parse(local).toInstant(zone)
    private fun Instant.local(zone: TimeZone = madrid): String = toLocalDateTime(zone).toString()

    @Test
    fun `defaults are breakfast, lunch and dinner, all off`() {
        val defaults = ReminderSettings.DEFAULT.reminders
        assertEquals(listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER), defaults.map { it.meal })
        assertEquals(listOf("08:30", "13:00", "20:00"), defaults.map { timeLabel(it.hour, it.minute) })
        assertFalse(defaults.any { it.enabled })
    }

    @Test
    fun `settings round-trip through JSON`() {
        val settings = ReminderSettings.DEFAULT.with(MealReminder(MealType.LUNCH, 12, 45, enabled = true))

        assertEquals(settings, decodeReminderSettings(encodeReminderSettings(settings)))
    }

    @Test
    fun `missing or unreadable settings fall back to the defaults`() {
        assertEquals(ReminderSettings.DEFAULT, decodeReminderSettings(null))
        assertEquals(ReminderSettings.DEFAULT, decodeReminderSettings("not json"))
        assertEquals(ReminderSettings.DEFAULT, decodeReminderSettings("""{"reminders":"nope"}"""))
    }

    @Test
    fun `partial or invalid entries keep the valid ones and default the rest`() {
        val stored = """{"reminders":[
            {"meal":"lunch","hour":12,"minute":45,"enabled":true},
            {"meal":"dinner","hour":25,"minute":0,"enabled":true},
            {"meal":"snack","hour":16,"minute":0,"enabled":true}
        ]}"""

        val settings = decodeReminderSettings(stored)

        assertEquals(MealReminder(MealType.LUNCH, 12, 45, enabled = true), settings.reminderFor(MealType.LUNCH))
        assertEquals(ReminderSettings.DEFAULT.reminderFor(MealType.DINNER), settings.reminderFor(MealType.DINNER))
        assertEquals(ReminderSettings.DEFAULT.reminderFor(MealType.BREAKFAST), settings.reminderFor(MealType.BREAKFAST))
        assertEquals(REMINDER_MEALS, settings.reminders.map { it.meal })
    }

    @Test
    fun `next trigger is later today when the time is still ahead`() {
        assertEquals("2026-10-12T13:00", nextTriggerAt(at("2026-10-12T09:15"), 13, 0, madrid).local())
    }

    @Test
    fun `next trigger is tomorrow when the time has passed or is exactly now`() {
        assertEquals("2026-10-13T13:00", nextTriggerAt(at("2026-10-12T13:30"), 13, 0, madrid).local())
        assertEquals("2026-10-13T13:00", nextTriggerAt(at("2026-10-12T13:00"), 13, 0, madrid).local())
    }

    @Test
    fun `next trigger across the spring-forward day is still the local time`() {
        // Madrid jumps 02:00 -> 03:00 on 2026-03-29; the day is 23 hours long.
        val next = nextTriggerAt(at("2026-03-28T20:30"), 20, 0, madrid)
        assertEquals("2026-03-29T20:00", next.local())
        assertEquals(23.hours, next - at("2026-03-28T20:00"))
    }

    @Test
    fun `a time skipped by the spring-forward jump fires right after it`() {
        assertEquals("2026-03-29T03:30", nextTriggerAt(at("2026-03-29T00:00"), 2, 30, madrid).local())
    }

    @Test
    fun `next trigger across the fall-back day is still the local time`() {
        // Madrid goes back 03:00 -> 02:00 on 2026-10-25; the day is 25 hours long.
        val next = nextTriggerAt(at("2026-10-24T08:45"), 8, 30, madrid)
        assertEquals("2026-10-25T08:30", next.local())
        assertEquals(25.hours, next - at("2026-10-24T08:30"))
    }

    @Test
    fun `notification texts name the meal`() {
        assertEquals(Res.string.reminder_title_lunch, reminderTitle(MealType.LUNCH))
        assertEquals(Res.string.reminder_title_breakfast, reminderTitle(MealType.BREAKFAST))
        assertEquals(Res.string.reminder_title_dinner, reminderTitle(MealType.DINNER))
        assertEquals("07:05", timeLabel(7, 5))
    }

    private fun meal(type: MealType?) = MealEntry(
        id = 1,
        occurredAt = "2026-10-12T15:00:00.000Z",
        createdAt = "2026-10-12T15:00:00.000Z",
        notes = null,
        mealType = type,
        items = emptyList(),
        totalCalories = 0.0,
        totalProteinGrams = 0.0,
        totalFatGrams = 0.0,
        totalCarbsGrams = 0.0,
    )

    @Test
    fun `a reminder shows when today's meals are unknown or don't include that meal`() {
        assertTrue(shouldShowReminder(MealType.LUNCH, null))
        assertTrue(shouldShowReminder(MealType.LUNCH, emptyList()))
        assertTrue(shouldShowReminder(MealType.LUNCH, listOf(meal(MealType.BREAKFAST), meal(MealType.SNACK))))
    }

    @Test
    fun `a reminder is skipped when that meal is already logged`() {
        assertFalse(shouldShowReminder(MealType.LUNCH, listOf(meal(MealType.BREAKFAST), meal(MealType.LUNCH))))
    }

    @Test
    fun `an untagged meal doesn't count as any meal`() {
        assertTrue(shouldShowReminder(MealType.LUNCH, listOf(meal(null))))
    }
}
