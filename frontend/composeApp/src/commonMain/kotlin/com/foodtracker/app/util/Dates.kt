package com.foodtracker.app.util

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun todayDateString(): String = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

fun String.plusDays(days: Int): String = LocalDate.parse(this).plus(DatePeriod(days = days)).toString()

@OptIn(ExperimentalTime::class)
fun currentLocalHour(): Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
