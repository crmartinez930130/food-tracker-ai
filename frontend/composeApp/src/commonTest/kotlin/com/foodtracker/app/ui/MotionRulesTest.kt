package com.foodtracker.app.ui

import com.foodtracker.app.ui.components.BadgeShape
import com.foodtracker.app.ui.components.animationsDisabled
import com.foodtracker.app.ui.components.badgeShapeFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MotionRulesTest {

    @Test
    fun `only a zero animator scale removes animations`() {
        assertTrue(animationsDisabled(0f))
        assertFalse(animationsDisabled(1f))
        assertFalse(animationsDisabled(0.5f))
    }

    @Test
    fun `the badge changes shape at 100 percent`() {
        assertEquals(BadgeShape.IN_PROGRESS, badgeShapeFor(99))
        assertEquals(BadgeShape.GOAL_REACHED, badgeShapeFor(100))
        assertEquals(BadgeShape.GOAL_REACHED, badgeShapeFor(112))
        assertEquals(BadgeShape.IN_PROGRESS, badgeShapeFor(null))
    }

    @Test
    fun `deeper screens go forward, shallower go back, same level fades`() {
        assertEquals(TransitionDirection.FORWARD, transitionDirection(Screen.Main(), Screen.Goals))
        assertEquals(TransitionDirection.FORWARD, transitionDirection(Screen.Capture, Screen.Review))
        assertEquals(TransitionDirection.BACK, transitionDirection(Screen.Review, Screen.Main()))
        assertEquals(TransitionDirection.BACK, transitionDirection(Screen.Reminders, Screen.Settings))
        assertEquals(TransitionDirection.FADE, transitionDirection(Screen.Capture, Screen.Goals))
        assertEquals(TransitionDirection.FORWARD, transitionDirection(Screen.Main(MainTab.HISTORY), Screen.DayDetail("2026-10-12", Screen.Main(MainTab.HISTORY))))
    }
}
