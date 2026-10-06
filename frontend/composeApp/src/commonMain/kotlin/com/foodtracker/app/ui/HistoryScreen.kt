package com.foodtracker.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.MealEntry
import com.foodtracker.app.data.MealType
import com.foodtracker.app.util.plusDays
import com.foodtracker.app.util.todayDateString
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Breakfast/lunch/dinner/snack first, in that order; untagged entries ("Other") last. */
private val MEAL_TYPE_SECTION_ORDER: List<MealType?> =
    listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK, null)

@Composable
fun HistoryScreen(apiClient: FoodTrackerApiClient, modifier: Modifier = Modifier) {
    var date by rememberSaveable { mutableStateOf(todayDateString()) }
    var meals by remember { mutableStateOf<List<MealEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(date, reloadKey) {
        isLoading = true
        errorMessage = null
        try {
            meals = apiClient.listMeals(date)
        } catch (e: Exception) {
            errorMessage = "Couldn't load meals: ${e.message ?: "unknown error"}"
        } finally {
            isLoading = false
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

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { date = date.plusDays(-1) }) { Text("<") }
            Text(date, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            TextButton(onClick = { date = date.plusDays(1) }) { Text(">") }
        }

        when {
            isLoading -> CircularProgressIndicator()
            errorMessage != null -> Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
            meals.isEmpty() -> Text("No meals logged for this day yet.")
            else -> {
                val grouped = meals.groupBy { it.mealType }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    for (type in MEAL_TYPE_SECTION_ORDER) {
                        val mealsOfType = grouped[type] ?: continue
                        item(key = "header-${type?.name ?: "other"}") {
                            Text(
                                type.displayLabelOrOther(),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            )
                        }
                        items(mealsOfType, key = { it.id }) { meal ->
                            MealRow(meal, onDelete = { deleteMeal(meal.id) })
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealRow(meal: MealEntry, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(meal.items.joinToString(", ") { it.name }.ifBlank { "(no items)" })
            Text(
                "${meal.totalCalories.roundToInt()} kcal · P ${meal.totalProteinGrams.roundToInt()}g · " +
                    "F ${meal.totalFatGrams.roundToInt()}g · C ${meal.totalCarbsGrams.roundToInt()}g",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        TextButton(onClick = onDelete) { Text("Delete") }
    }
}
