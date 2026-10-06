package com.foodtracker.app.ui

import com.foodtracker.app.data.MealType

fun MealType.displayLabel(): String = when (this) {
    MealType.BREAKFAST -> "Breakfast"
    MealType.LUNCH -> "Lunch"
    MealType.DINNER -> "Dinner"
    MealType.SNACK -> "Snack"
}

/** For grouping entries that predate this feature or were never tagged. */
fun MealType?.displayLabelOrOther(): String = this?.displayLabel() ?: "Other"
