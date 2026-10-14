package com.foodtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.Confidence
import com.foodtracker.app.data.MealType
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.confidence_high
import food_tracker_frontend.composeapp.generated.resources.confidence_low
import food_tracker_frontend.composeapp.generated.resources.confidence_medium
import food_tracker_frontend.composeapp.generated.resources.ic_bakery_dining
import food_tracker_frontend.composeapp.generated.resources.ic_cookie
import food_tracker_frontend.composeapp.generated.resources.ic_dinner_dining
import food_tracker_frontend.composeapp.generated.resources.ic_lunch_dining
import food_tracker_frontend.composeapp.generated.resources.ic_restaurant
import food_tracker_frontend.composeapp.generated.resources.meal_type_breakfast
import food_tracker_frontend.composeapp.generated.resources.meal_type_dinner
import food_tracker_frontend.composeapp.generated.resources.meal_type_lunch
import food_tracker_frontend.composeapp.generated.resources.meal_type_other
import food_tracker_frontend.composeapp.generated.resources.meal_type_snack
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
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

/** A meal type's icon and its square's colors; snack and "Other" share the neutral container. */
data class MealTypeIconStyle(val icon: DrawableResource, val container: Color, val content: Color)

@Composable
fun MealType?.iconStyle(): MealTypeIconStyle {
    val colors = MaterialTheme.colorScheme
    return when (this) {
        MealType.BREAKFAST -> MealTypeIconStyle(Res.drawable.ic_bakery_dining, colors.tertiaryContainer, colors.onTertiaryContainer)
        MealType.LUNCH -> MealTypeIconStyle(Res.drawable.ic_lunch_dining, colors.primaryContainer, colors.onPrimaryContainer)
        MealType.DINNER -> MealTypeIconStyle(Res.drawable.ic_dinner_dining, colors.secondaryContainer, colors.onSecondaryContainer)
        MealType.SNACK -> MealTypeIconStyle(Res.drawable.ic_cookie, colors.surfaceContainerHighest, colors.onSurfaceVariant)
        null -> MealTypeIconStyle(Res.drawable.ic_restaurant, colors.surfaceContainerHighest, colors.onSurfaceVariant)
    }
}

/** The meal type's icon in its tinted rounded square, as on Home's groups and the reminders. */
@Composable
fun MealTypeIcon(type: MealType?, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val style = type.iconStyle()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).background(style.container, MaterialTheme.shapes.small),
    ) {
        Icon(painterResource(style.icon), contentDescription = null, tint = style.content, modifier = Modifier.size(size * 0.55f))
    }
}
