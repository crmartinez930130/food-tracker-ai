package com.foodtracker.app.ui

import com.foodtracker.app.data.MealItem
import com.foodtracker.app.data.Portion

/** Portion after tapping [tapped]: tapping the selected one clears it (none is a valid choice). */
fun toggledPortion(selected: Portion?, tapped: Portion): Portion? = if (selected == tapped) null else tapped

/** A Review number field's value: a decimal comma is accepted, and anything else counts as 0. */
fun editableNumber(text: String): Double = text.trim().replace(',', '.').toDoubleOrNull() ?: 0.0

data class MealTotals(val calories: Double, val proteinGrams: Double, val fatGrams: Double, val carbsGrams: Double)

/** The meal being reviewed, summed: what the total card and the save pill show. */
fun mealTotals(items: List<MealItem>): MealTotals = MealTotals(
    calories = items.sumOf { it.calories },
    proteinGrams = items.sumOf { it.proteinGrams },
    fatGrams = items.sumOf { it.fatGrams },
    carbsGrams = items.sumOf { it.carbsGrams },
)
