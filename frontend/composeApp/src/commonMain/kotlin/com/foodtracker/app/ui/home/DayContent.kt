package com.foodtracker.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.DailyGoal
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.LogWaterRequest
import com.foodtracker.app.data.MealEntry
import com.foodtracker.app.data.MealType
import com.foodtracker.app.data.WaterDay
import com.foodtracker.app.ui.LoadingAware
import com.foodtracker.app.ui.components.BarSegment
import com.foodtracker.app.ui.components.BigNumber
import com.foodtracker.app.ui.components.SegmentedBar
import com.foodtracker.app.ui.components.ShapeBadge
import com.foodtracker.app.ui.displayLabelOrOther
import com.foodtracker.app.ui.errorText
import com.foodtracker.app.ui.loadingPresentation
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.action_delete
import food_tracker_frontend.composeapp.generated.resources.add_to_favorites
import food_tracker_frontend.composeapp.generated.resources.calorie_ring_left
import food_tracker_frontend.composeapp.generated.resources.calorie_ring_of_target
import food_tracker_frontend.composeapp.generated.resources.calorie_ring_over
import food_tracker_frontend.composeapp.generated.resources.default_goal_hint
import food_tracker_frontend.composeapp.generated.resources.edit_daily_goal
import food_tracker_frontend.composeapp.generated.resources.error_delete_meal
import food_tracker_frontend.composeapp.generated.resources.error_load_day
import food_tracker_frontend.composeapp.generated.resources.error_update_favorite
import food_tracker_frontend.composeapp.generated.resources.error_update_water
import food_tracker_frontend.composeapp.generated.resources.home_calories_title
import food_tracker_frontend.composeapp.generated.resources.home_empty_day
import food_tracker_frontend.composeapp.generated.resources.ic_bakery_dining
import food_tracker_frontend.composeapp.generated.resources.ic_cookie
import food_tracker_frontend.composeapp.generated.resources.ic_delete
import food_tracker_frontend.composeapp.generated.resources.ic_dinner_dining
import food_tracker_frontend.composeapp.generated.resources.ic_edit
import food_tracker_frontend.composeapp.generated.resources.ic_lunch_dining
import food_tracker_frontend.composeapp.generated.resources.ic_restaurant
import food_tracker_frontend.composeapp.generated.resources.ic_star
import food_tracker_frontend.composeapp.generated.resources.ic_star_filled
import food_tracker_frontend.composeapp.generated.resources.macro_carbs
import food_tracker_frontend.composeapp.generated.resources.macro_fat
import food_tracker_frontend.composeapp.generated.resources.macro_progress_grams
import food_tracker_frontend.composeapp.generated.resources.macro_protein
import food_tracker_frontend.composeapp.generated.resources.macros_short
import food_tracker_frontend.composeapp.generated.resources.no_items
import food_tracker_frontend.composeapp.generated.resources.remove_from_favorites
import food_tracker_frontend.composeapp.generated.resources.unit_kcal
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Breakfast/lunch/dinner/snack first, in that order; untagged entries ("Other") last. */
private val MEAL_TYPE_SECTION_ORDER: List<MealType?> =
    listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK, null)

private data class DayTotals(val calories: Double, val protein: Double, val fat: Double, val carbs: Double)

/** Summed from the same meals the list shows, so the totals and the list can never disagree. */
private fun List<MealEntry>.dayTotals() = DayTotals(
    calories = sumOf { it.totalCalories },
    protein = sumOf { it.totalProteinGrams },
    fat = sumOf { it.totalFatGrams },
    carbs = sumOf { it.totalCarbsGrams },
)

/**
 * One day's calories, macros, water and meals, with every action on them (favorite, delete, log
 * or undo water). Home shows it for today; it takes the day so other screens can show past days.
 */
@Composable
fun DayContent(
    apiClient: FoodTrackerApiClient,
    date: String,
    today: String,
    onEditGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var meals by remember { mutableStateOf<List<MealEntry>>(emptyList()) }
    var goal by remember { mutableStateOf<DailyGoal?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var water by remember { mutableStateOf<WaterDay?>(null) }
    var waterError by remember { mutableStateOf<String?>(null) }
    // Serializes water taps, so each refetch reflects every earlier log/undo (taps can be rapid).
    val waterMutex = remember { Mutex() }
    val currentDate by rememberUpdatedState(date)
    val scope = rememberCoroutineScope()

    LaunchedEffect(date, reloadKey) {
        isLoading = true
        errorMessage = null
        try {
            meals = apiClient.listMeals(date)
            goal = apiClient.getGoals()
            water = apiClient.getWater(date)
            waterError = null
        } catch (e: Exception) {
            errorMessage = errorText(Res.string.error_load_day, e)
        } finally {
            isLoading = false
        }
    }

    fun toggleFavorite(meal: MealEntry) {
        scope.launch {
            try {
                apiClient.setFavorite(meal.id, !meal.isFavorite)
                reloadKey++
            } catch (e: Exception) {
                errorMessage = errorText(Res.string.error_update_favorite, e)
            }
        }
    }

    fun deleteMeal(id: Int) {
        scope.launch {
            try {
                apiClient.deleteMeal(id)
                reloadKey++
            } catch (e: Exception) {
                errorMessage = errorText(Res.string.error_delete_meal, e)
            }
        }
    }

    // Only water is refetched afterwards, not the day's meals.
    fun changeWater(action: suspend (day: String) -> Unit) {
        val day = date
        scope.launch {
            waterMutex.withLock {
                try {
                    action(day)
                    val refreshed = apiClient.getWater(day)
                    // The shown day may have changed meanwhile (e.g. at midnight); don't show this one there.
                    if (currentDate == day) water = refreshed
                    waterError = null
                } catch (e: Exception) {
                    waterError = errorText(Res.string.error_update_water, e)
                }
            }
        }
    }

    val currentGoal = goal
    when {
        errorMessage != null -> Text(
            errorMessage.orEmpty(),
            color = MaterialTheme.colorScheme.error,
            modifier = modifier.padding(horizontal = 16.dp),
        )
        // Dims the previous day while the new one loads, instead of showing it unmarked.
        else -> LoadingAware(loadingPresentation(isLoading, hasContent = currentGoal != null), modifier) {
            if (currentGoal != null) {
                val totals = meals.dayTotals()
                val grouped = meals.groupBy { it.mealType }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "calories") {
                        CaloriesCard(totals = totals, goal = currentGoal, onEditGoal = onEditGoal)
                    }
                    item(key = "water") {
                        Column {
                            WaterCard(
                                water = water,
                                targetMl = currentGoal.targetWaterMl,
                                glassMl = currentGoal.waterGlassMl,
                                onAddGlass = {
                                    changeWater { day ->
                                        apiClient.logWater(LogWaterRequest(currentGoal.waterGlassMl, loggedAtFor(day, today)))
                                    }
                                },
                                onUndo = {
                                    // Picked under the lock from fresh data, so rapid undos each remove a different entry.
                                    changeWater { day ->
                                        apiClient.getWater(day).entries.lastOrNull()?.let { apiClient.deleteWater(it.id) }
                                    }
                                },
                            )
                            waterError?.let {
                                Text(
                                    it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                    if (meals.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                stringResource(Res.string.home_empty_day),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                    for (type in MEAL_TYPE_SECTION_ORDER) {
                        val mealsOfType = grouped[type] ?: continue
                        item(key = "group-${type?.name ?: "other"}") {
                            MealGroupCard(
                                type = type,
                                meals = mealsOfType,
                                onToggleFavorite = ::toggleFavorite,
                                onDelete = { deleteMeal(it.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The day's calories as a big figure with the goal percentage badge, plus the macro bar. */
@Composable
private fun CaloriesCard(totals: DayTotals, goal: DailyGoal, onEditGoal: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val progress = progressOf(totals.calories, goal.targetCalories)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer, contentColor = colors.onSurface),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(Res.string.home_calories_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    BigNumber(value = "${totals.calories.roundToInt()}", unit = "kcal")
                    Row {
                        Text(
                            stringResource(Res.string.calorie_ring_of_target, goal.targetCalories.roundToInt()) + " · ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                        Text(
                            if (progress.isOver) {
                                stringResource(Res.string.calorie_ring_over, progress.excess.roundToInt())
                            } else {
                                stringResource(Res.string.calorie_ring_left, progress.remaining.roundToInt())
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (progress.isOver) colors.error else colors.primary,
                        )
                    }
                }
                val percent = goalPercent(totals.calories, goal.targetCalories)
                ShapeBadge(
                    text = percent?.let { "$it%" } ?: "–",
                    size = 80.dp,
                    containerColor = if (progress.isOver) colors.errorContainer else colors.primaryContainer,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clip(CircleShape)
                        .clickable(onClickLabel = stringResource(Res.string.edit_daily_goal), onClick = onEditGoal),
                )
            }
            if (goal.isDefault) {
                AssistChip(
                    onClick = onEditGoal,
                    label = { Text(stringResource(Res.string.default_goal_hint)) },
                    leadingIcon = { Icon(painterResource(Res.drawable.ic_edit), contentDescription = null) },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            MacroBar(totals, goal, Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
private fun MacroBar(totals: DayTotals, goal: DailyGoal, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    @Composable
    fun segment(label: String, consumed: Double, target: Double, color: Color) = BarSegment(
        label = label,
        value = consumed,
        color = color,
        detail = stringResource(Res.string.macro_progress_grams, consumed.roundToInt(), target.roundToInt()),
        detailColor = if (progressOf(consumed, target).isOver) colors.error else null,
    )
    SegmentedBar(
        segments = listOf(
            segment(stringResource(Res.string.macro_protein), totals.protein, goal.targetProteinGrams, colors.primary),
            segment(stringResource(Res.string.macro_fat), totals.fat, goal.targetFatGrams, colors.tertiary),
            segment(stringResource(Res.string.macro_carbs), totals.carbs, goal.targetCarbsGrams, colors.secondary),
        ),
        modifier = modifier,
    )
}

/** Icon and square color for each meal type; snack and "Other" share the neutral container. */
@Composable
private fun MealType?.iconStyle(): Triple<DrawableResource, Color, Color> {
    val colors = MaterialTheme.colorScheme
    return when (this) {
        MealType.BREAKFAST -> Triple(Res.drawable.ic_bakery_dining, colors.tertiaryContainer, colors.onTertiaryContainer)
        MealType.LUNCH -> Triple(Res.drawable.ic_lunch_dining, colors.primaryContainer, colors.onPrimaryContainer)
        MealType.DINNER -> Triple(Res.drawable.ic_dinner_dining, colors.secondaryContainer, colors.onSecondaryContainer)
        MealType.SNACK -> Triple(Res.drawable.ic_cookie, colors.surfaceContainerHighest, colors.onSurfaceVariant)
        null -> Triple(Res.drawable.ic_restaurant, colors.surfaceContainerHighest, colors.onSurfaceVariant)
    }
}

@Composable
private fun MealGroupCard(
    type: MealType?,
    meals: List<MealEntry>,
    onToggleFavorite: (MealEntry) -> Unit,
    onDelete: (MealEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (icon, squareColor, iconColor) = type.iconStyle()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow, contentColor = colors.onSurface),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp).background(squareColor, MaterialTheme.shapes.small),
                ) {
                    Icon(painterResource(icon), contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                }
                Text(
                    type.displayLabelOrOther(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
                Text(
                    stringResource(Res.string.unit_kcal, meals.sumOf { it.totalCalories }.roundToInt()),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                )
            }
            meals.forEachIndexed { index, meal ->
                if (index > 0) HorizontalDivider(color = colors.outlineVariant)
                MealRow(meal, onToggleFavorite = { onToggleFavorite(meal) }, onDelete = { onDelete(meal) })
            }
        }
    }
}

@Composable
private fun MealRow(
    meal: MealEntry,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                meal.items.joinToString(", ") { it.name }.ifBlank { stringResource(Res.string.no_items) },
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(
                    Res.string.macros_short,
                    meal.totalCalories.roundToInt(),
                    meal.totalProteinGrams.roundToInt(),
                    meal.totalFatGrams.roundToInt(),
                    meal.totalCarbsGrams.roundToInt(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                painterResource(if (meal.isFavorite) Res.drawable.ic_star_filled else Res.drawable.ic_star),
                contentDescription = stringResource(if (meal.isFavorite) Res.string.remove_from_favorites else Res.string.add_to_favorites),
                tint = if (meal.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                painterResource(Res.drawable.ic_delete),
                contentDescription = stringResource(Res.string.action_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
