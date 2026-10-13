package com.foodtracker.app.ui.home

/** Which greeting Home's header shows. */
enum class Greeting { MORNING, AFTERNOON, EVENING }

/** Morning from 05:00, afternoon from 12:00, evening from 20:00 until 04:59. */
fun greetingFor(hour: Int): Greeting = when (hour) {
    in 5..11 -> Greeting.MORNING
    in 12..19 -> Greeting.AFTERNOON
    else -> Greeting.EVENING
}
