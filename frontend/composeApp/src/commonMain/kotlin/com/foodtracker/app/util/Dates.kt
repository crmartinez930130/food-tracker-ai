package com.foodtracker.app.util

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun todayDateString(): String = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

fun String.plusDays(days: Int): String = LocalDate.parse(this).plus(DatePeriod(days = days)).toString()
