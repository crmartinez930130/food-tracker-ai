package com.foodtracker.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.DailyGoal
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.MealEntry
import com.foodtracker.app.data.LogWaterRequest
import com.foodtracker.app.data.MealType
import com.foodtracker.app.data.WaterDay
import com.foodtracker.app.ui.LoadingAware
import com.foodtracker.app.ui.displayLabelOrOther
import com.foodtracker.app.ui.loadingPresentation
import com.foodtracker.app.util.isBeforeDay
import com.foodtracker.app.util.plusDays
import com.foodtracker.app.util.todayDateString
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.ic_chevron_left
import food_tracker_frontend.composeapp.generated.resources.ic_chevron_right
import food_tracker_frontend.composeapp.generated.resources.ic_edit
import food_tracker_frontend.composeapp.generated.resources.ic_notifications
import food_tracker_frontend.composeapp.generated.resources.ic_star
import food_tracker_frontend.composeapp.generated.resources.ic_star_filled
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

/** Breakfast/lunch/dinner/snack first, in that order; untagged entries ("Other") last. */
private val MEAL_TYPE_SECTION_ORDER: List<MealType?> =
    listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK, null)

private data class DayTotals(val calories: Double, val protein: Double, val fat: Double, val carbs: Double)

/** Summed from the same meals the list shows, so the ring and the list can never disagree. */
private fun List<MealEntry>.dayTotals() = DayTotals(
    calories = sumOf { it.totalCalories },
    protein = sumOf { it.totalProteinGrams },
    fat = sumOf { it.totalFatGrams },
    carbs = sumOf { it.totalCarbsGrams },
)

@Composable
fun HomeScreen(
    apiClient: FoodTrackerApiClient,
    date: String,
    onDateChange: (String) -> Unit,
    onEditGoal: () -> Unit,
    onOpenReminders: () -> Unit,
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
    val today = todayDateString()

    LaunchedEffect(date, reloadKey) {
        isLoading = true
        errorMessage = null
        try {
            meals = apiClient.listMeals(date)
            goal = apiClient.getGoals()
            water = apiClient.getWater(date)
            waterError = null
        } catch (e: Exception) {
            errorMessage = "Couldn't load this day: ${e.message ?: "unknown error"}"
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
                errorMessage = "Couldn't update favorite: ${e.message ?: "unknown error"}"
            }
        }
    }

    fun deleteMeal(id: Int) {
        scope.launch {
            try {
                apiClient.deleteMeal(id)
                reloadKey++
            } catch (e: Exception) {
                errorMessage = "Couldn't delete meal: ${e.message ?: "unknown error"}"
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
                    // The user may have moved to another day meanwhile; don't show this one there.
                    if (currentDate == day) water = refreshed
                    waterError = null
                } catch (e: Exception) {
                    waterError = "Couldn't update water: ${e.message ?: "unknown error"}"
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        DayHeader(
            label = dayLabel(date, today),
            canGoForward = date.isBeforeDay(today),
            onPrevious = { onDateChange(date.plusDays(-1)) },
            onNext = { onDateChange(date.plusDays(1)) },
            onEditGoal = onEditGoal,
            onOpenReminders = onOpenReminders,
        )

        val currentGoal = goal
        when {
            errorMessage != null -> Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
            // Dims the previous day while the new one loads, instead of showing it unmarked.
            else -> LoadingAware(loadingPresentation(isLoading, hasContent = currentGoal != null), Modifier.weight(1f)) {
                if (currentGoal != null) {
                    val totals = meals.dayTotals()
                    val grouped = meals.groupBy { it.mealType }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item(key = "progress") {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                CalorieRing(
                                    consumed = totals.calories,
                                    target = currentGoal.targetCalories,
                                    onClick = onEditGoal,
                                )
                                if (currentGoal.isDefault) {
                                    AssistChip(
                                        onClick = onEditGoal,
                                        label = { Text("Using default goal — set yours") },
                                        leadingIcon = { Icon(painterResource(Res.drawable.ic_edit), contentDescription = null) },
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                MacroBar("Protein", totals.protein, currentGoal.targetProteinGrams)
                                MacroBar("Fat", totals.fat, currentGoal.targetFatGrams)
                                MacroBar("Carbs", totals.carbs, currentGoal.targetCarbsGrams)
                                Spacer(Modifier.height(8.dp))
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
                                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        if (meals.isEmpty()) {
                            item(key = "empty") {
                                Text(
                                    "Nothing logged this day yet.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 16.dp),
                                )
                            }
                        }
                        for (type in MEAL_TYPE_SECTION_ORDER) {
                            val mealsOfType = grouped[type] ?: continue
                            item(key = "header-${type?.name ?: "other"}") {
                                Text(
                                    type.displayLabelOrOther(),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                )
                            }
                            items(mealsOfType, key = { it.id }) { meal ->
                                MealRow(meal, onToggleFavorite = { toggleFavorite(meal) }, onDelete = { deleteMeal(meal.id) })
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(
    label: String,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onEditGoal: () -> Unit,
    onOpenReminders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(painterResource(Res.drawable.ic_chevron_left), contentDescription = "Previous day")
        }
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(painterResource(Res.drawable.ic_chevron_right), contentDescription = "Next day")
        }
        IconButton(onClick = onOpenReminders) {
            Icon(painterResource(Res.drawable.ic_notifications), contentDescription = "Meal reminders")
        }
        IconButton(onClick = onEditGoal) {
            Icon(painterResource(Res.drawable.ic_edit), contentDescription = "Edit daily goal")
        }
    }
}

@Composable
private fun CalorieRing(
    consumed: Double,
    target: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = progressOf(consumed, target)
    val color = if (progress.isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(vertical = 8.dp)
            .size(184.dp)
            .clip(CircleShape)
            .clickable(onClickLabel = "Edit daily goal", onClick = onClick),
    ) {
        CircularProgressIndicator(
            progress = { progress.fraction },
            modifier = Modifier.fillMaxSize().padding(4.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeWidth = 14.dp,
            strokeCap = StrokeCap.Round,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${consumed.roundToInt()}", style = MaterialTheme.typography.headlineLarge)
            Text(
                "of ${target.roundToInt()} kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (progress.isOver) "${progress.excess.roundToInt()} over" else "${progress.remaining.roundToInt()} left",
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
        }
    }
}

@Composable
private fun MacroBar(name: String, consumed: Double, target: Double, modifier: Modifier = Modifier) {
    val progress = progressOf(consumed, target)
    val color = if (progress.isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(name, style = MaterialTheme.typography.labelLarge)
            Text(
                "${consumed.roundToInt()} / ${target.roundToInt()} g",
                style = MaterialTheme.typography.labelLarge,
                color = if (progress.isOver) color else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress.fraction },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
private fun MealRow(
    meal: MealEntry,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(meal.items.joinToString(", ") { it.name }.ifBlank { "(no items)" })
            Text(
                "${meal.totalCalories.roundToInt()} kcal · P ${meal.totalProteinGrams.roundToInt()}g · " +
                    "F ${meal.totalFatGrams.roundToInt()}g · C ${meal.totalCarbsGrams.roundToInt()}g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                painterResource(if (meal.isFavorite) Res.drawable.ic_star_filled else Res.drawable.ic_star),
                contentDescription = if (meal.isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (meal.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onDelete) { Text("Delete") }
    }
}
