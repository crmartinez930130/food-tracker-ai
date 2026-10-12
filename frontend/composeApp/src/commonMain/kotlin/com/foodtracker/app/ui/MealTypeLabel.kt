package com.foodtracker.app.ui

import androidx.compose.runtime.Composable
import com.foodtracker.app.data.Confidence
import com.foodtracker.app.data.MealType
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.confidence_high
import food_tracker_frontend.composeapp.generated.resources.confidence_low
import food_tracker_frontend.composeapp.generated.resources.confidence_medium
import food_tracker_frontend.composeapp.generated.resources.meal_type_breakfast
import food_tracker_frontend.composeapp.generated.resources.meal_type_dinner
import food_tracker_frontend.composeapp.generated.resources.meal_type_lunch
import food_tracker_frontend.composeapp.generated.resources.meal_type_other
import food_tracker_frontend.composeapp.generated.resources.meal_type_snack
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val MealType.labelRes: StringResource
    get() = when (this) {
        MealType.BREAKFAST -> Res.string.meal_type_breakfast
        MealType.LUNCH -> Res.string.meal_type_lunch
        MealType.DINNER -> Res.string.meal_type_dinner
        MealType.SNACK -> Res.string.meal_type_snack
    }

@Composable
fun MealType.displayLabel(): String = stringResource(labelRes)

/** For grouping entries that predate this feature or were never tagged. */
@Composable
fun MealType?.displayLabelOrOther(): String = stringResource(this?.labelRes ?: Res.string.meal_type_other)

val Confidence.labelRes: StringResource
    get() = when (this) {
        Confidence.LOW -> Res.string.confidence_low
        Confidence.MEDIUM -> Res.string.confidence_medium
        Confidence.HIGH -> Res.string.confidence_high
    }
