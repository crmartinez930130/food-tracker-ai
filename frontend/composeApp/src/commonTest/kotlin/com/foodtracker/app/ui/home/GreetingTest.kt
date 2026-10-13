package com.foodtracker.app.ui.home

import kotlin.test.Test
import kotlin.test.assertEquals

class GreetingTest {

    @Test
    fun `greeting changes at 5, 12 and 20`() {
        assertEquals(Greeting.EVENING, greetingFor(4))
        assertEquals(Greeting.MORNING, greetingFor(5))
        assertEquals(Greeting.MORNING, greetingFor(11))
        assertEquals(Greeting.AFTERNOON, greetingFor(12))
        assertEquals(Greeting.AFTERNOON, greetingFor(19))
        assertEquals(Greeting.EVENING, greetingFor(20))
    }

    @Test
    fun `evening wraps around midnight`() {
        assertEquals(Greeting.EVENING, greetingFor(23))
        assertEquals(Greeting.EVENING, greetingFor(0))
    }
}
