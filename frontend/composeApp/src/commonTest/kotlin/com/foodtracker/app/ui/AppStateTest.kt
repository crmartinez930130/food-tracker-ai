package com.foodtracker.app.ui

import com.foodtracker.app.data.MealAnalysis
import kotlin.test.Test
import kotlin.test.assertEquals

class AppStateTest {

    private val analysis = MealAnalysis(
        items = emptyList(),
        totalCalories = 0.0,
        totalProteinGrams = 0.0,
        totalFatGrams = 0.0,
        totalCarbsGrams = 0.0,
    )

    @Test
    fun `a reminder tap opens capture from Main, Goals and Reminders`() {
        for (open in listOf<AppState.() -> Unit>({ openMain(MainTab.SUMMARY) }, { openGoals() }, { openSettings() }, { openReminders() }, { openDesignSample() }, { openDayDetail("2026-10-12") })) {
            val state = AppState().apply(open)

            state.openCaptureFromReminder()

            assertEquals(Screen.Capture, state.screen)
        }
    }

    @Test
    fun `a reminder tap while capturing stays on capture`() {
        val state = AppState().apply { openCapture() }

        state.openCaptureFromReminder()

        assertEquals(Screen.Capture, state.screen)
    }

    @Test
    fun `a reminder tap while reviewing keeps the review and its analysis`() {
        val state = AppState().apply { openReview(analysis) }

        state.openCaptureFromReminder()

        assertEquals(Screen.Review, state.screen)
        assertEquals(analysis, state.pendingAnalysis)
    }

    @Test
    fun `options leads to reminders and back to options`() {
        val state = AppState()

        state.openSettings()
        assertEquals(Screen.Settings, state.screen)
        state.openReminders()
        assertEquals(Screen.Reminders, state.screen)
        state.openSettings()
        assertEquals(Screen.Settings, state.screen)
    }

    @Test
    fun `a day opened from History goes back to History`() {
        val state = AppState().apply { openMain(MainTab.HOME); selectMainTab(MainTab.HISTORY) }

        state.openDayDetail("2026-10-12")
        assertEquals(Screen.DayDetail("2026-10-12", returnTo = Screen.Main(MainTab.HISTORY)), state.screen)
        state.closeDayDetail()

        assertEquals(Screen.Main(MainTab.HISTORY), state.screen)
    }

    @Test
    fun `goals opened from a day's detail goes back to that detail`() {
        val state = AppState().apply { openMain(MainTab.HISTORY); openDayDetail("2026-10-12") }
        val detail = state.screen

        state.openGoals()
        state.closeGoals()

        assertEquals(detail, state.screen)
        state.closeDayDetail()
        assertEquals(Screen.Main(MainTab.HISTORY), state.screen)
    }

    @Test
    fun `goals opened from Home goes back to Home`() {
        val state = AppState()

        state.openGoals()
        state.closeGoals()

        assertEquals(Screen.Main(MainTab.HOME), state.screen)
    }
}
