package com.foodtracker.app.ui

import androidx.compose.runtime.Composable
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.history_month_title
import food_tracker_frontend.composeapp.generated.resources.home_date
import food_tracker_frontend.composeapp.generated.resources.month_names
import food_tracker_frontend.composeapp.generated.resources.month_short_names
import food_tracker_frontend.composeapp.generated.resources.weekday_names
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

/** The weekday's name in the app language, as stored (lowercase in Spanish). */
@Composable
fun weekdayName(date: LocalDate): String = stringArrayResource(Res.array.weekday_names)[date.dayOfWeek.isoDayNumber - 1]

@Composable
private fun monthName(date: LocalDate): String = stringArrayResource(Res.array.month_names)[date.month.number - 1]

/** "Lunes, 13 de octubre" / "Monday, October 13", in the app language. */
@Composable
fun longDate(date: LocalDate): String =
    stringResource(Res.string.home_date, weekdayName(date), date.day, monthName(date)).replaceFirstChar { it.titlecase() }

/** "Octubre de 2026" / "October 2026". */
@Composable
fun monthTitle(date: LocalDate): String =
    stringResource(Res.string.history_month_title, monthName(date), date.year).replaceFirstChar { it.titlecase() }

/** "13 oct" / "13 Oct". */
@Composable
fun shortDate(date: LocalDate): String = "${date.day} ${stringArrayResource(Res.array.month_short_names)[date.month.number - 1]}"
