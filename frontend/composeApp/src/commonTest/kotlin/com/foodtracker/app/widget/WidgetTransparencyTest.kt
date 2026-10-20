package com.foodtracker.app.widget

import kotlin.test.Test
import kotlin.test.assertEquals

class WidgetTransparencyTest {
    @Test
    fun `transparency stays between 0 and 100`() {
        assertEquals(0, clampTransparency(-20))
        assertEquals(40, clampTransparency(40))
        assertEquals(100, clampTransparency(250))
    }

    @Test
    fun `the slider snaps to steps of 5`() {
        assertEquals(0, snapTransparency(2.4f))
        assertEquals(5, snapTransparency(2.6f))
        assertEquals(60, snapTransparency(61f))
        assertEquals(100, snapTransparency(99f))
        assertEquals(100, snapTransparency(130f))
        assertEquals(0, snapTransparency(-3f))
    }

    @Test
    fun `alpha is the opaque share of the background`() {
        assertEquals(1f, backgroundAlpha(0))
        assertEquals(0.4f, backgroundAlpha(60))
        assertEquals(0f, backgroundAlpha(100))
        assertEquals(1f, backgroundAlpha(-5))
    }
}
