package com.foodtracker.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.MealAnalysis
import com.foodtracker.app.data.MealItem
import com.foodtracker.app.data.MealType
import com.foodtracker.app.data.SaveMealRequest
import com.foodtracker.app.util.currentLocalHour
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** UI-only default (see design.md of the add-meal-type change) - the backend never derives a type from a timestamp. */
private fun suggestedMealType(hourOfDay: Int): MealType = when {
    hourOfDay < 11 -> MealType.BREAKFAST
    hourOfDay < 15 -> MealType.LUNCH
    hourOfDay < 19 -> MealType.SNACK
    else -> MealType.DINNER
}

/** Mutable, text-field-friendly mirror of [MealItem] used while the user edits an analysis. */
private class EditableItemState(item: MealItem) {
    // A stable, Bundle-storable identity for LazyColumn's `key` - the instance itself isn't
    // a valid key on Android (SaveableStateHolder requires primitives/Parcelable/String).
    val id: Int = nextId()
    var name by mutableStateOf(item.name)
    var estimatedGrams by mutableStateOf(item.estimatedGrams.toString())
    var confidence by mutableStateOf(item.confidence)
    var calories by mutableStateOf(item.calories.toString())
    var proteinGrams by mutableStateOf(item.proteinGrams.toString())
    var fatGrams by mutableStateOf(item.fatGrams.toString())
    var carbsGrams by mutableStateOf(item.carbsGrams.toString())

    fun toMealItem(): MealItem = MealItem(
        name = name,
        estimatedGrams = estimatedGrams.toDoubleOrNull() ?: 0.0,
        confidence = confidence,
        calories = calories.toDoubleOrNull() ?: 0.0,
        proteinGrams = proteinGrams.toDoubleOrNull() ?: 0.0,
        fatGrams = fatGrams.toDoubleOrNull() ?: 0.0,
        carbsGrams = carbsGrams.toDoubleOrNull() ?: 0.0,
    )

    private companion object {
        var counter = 0
        fun nextId(): Int = counter++
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ReviewScreen(
    initialAnalysis: MealAnalysis,
    apiClient: FoodTrackerApiClient,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember(initialAnalysis) {
        mutableStateListOf(*initialAnalysis.items.map { EditableItemState(it) }.toTypedArray())
    }
    var notes by remember { mutableStateOf(initialAnalysis.notes ?: "") }
    var mealType by remember { mutableStateOf(suggestedMealType(currentLocalHour())) }
    var isSaving by remember { mutableStateOf(false) }
    var showDiscardPrompt by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val totalCalories = items.sumOf { it.calories.toDoubleOrNull() ?: 0.0 }
    val totalProtein = items.sumOf { it.proteinGrams.toDoubleOrNull() ?: 0.0 }
    val totalFat = items.sumOf { it.fatGrams.toDoubleOrNull() ?: 0.0 }
    val totalCarbs = items.sumOf { it.carbsGrams.toDoubleOrNull() ?: 0.0 }

    fun save() {
        isSaving = true
        errorMessage = null
        scope.launch {
            try {
                apiClient.saveMeal(
                    SaveMealRequest(
                        items = items.map { it.toMealItem() },
                        notes = notes.ifBlank { null },
                        mealType = mealType,
                    ),
                )
                onSaved()
            } catch (e: Exception) {
                errorMessage = "Couldn't save the meal: ${e.message ?: "unknown error"}"
            } finally {
                isSaving = false
            }
        }
    }

    // Always enabled while this screen is shown, so back never falls through to the activity
    // (which would close the app and lose the unsaved analysis).
    BackHandler {
        when (reviewBackAction(isSaving = isSaving, isDiscardPromptShown = showDiscardPrompt)) {
            ReviewBack.IGNORE -> Unit
            ReviewBack.DISMISS_PROMPT -> showDiscardPrompt = false
            ReviewBack.SHOW_PROMPT -> showDiscardPrompt = true
        }
    }

    if (showDiscardPrompt) {
        AlertDialog(
            onDismissRequest = { showDiscardPrompt = false },
            title = { Text("Discard this meal?") },
            text = { Text("The analysis and your edits will be lost.") },
            confirmButton = {
                TextButton(onClick = onCancel) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardPrompt = false }) { Text("Keep editing") }
            },
        )
    }

    Column(modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp)) {
        Text("Review analysis", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        MealTypeSelector(selected = mealType, onSelect = { mealType = it })
        Spacer(Modifier.height(8.dp))

        if (items.isEmpty()) {
            Text("No food was detected in this photo. You can add items manually if needed.")
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(items, key = { it.id }) { item ->
                EditableItemRow(item, onRemove = { items.remove(item) })
                HorizontalDivider()
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
        Text(
            "Total: ${totalCalories.roundToInt()} kcal · P ${totalProtein.roundToInt()}g · " +
                "F ${totalFat.roundToInt()}g · C ${totalCarbs.roundToInt()}g",
            style = MaterialTheme.typography.titleSmall,
        )

        errorMessage?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedButton(onClick = { showDiscardPrompt = true }, enabled = !isSaving) { Text("Cancel") }
            Spacer(Modifier.width(12.dp))
            Button(onClick = { save() }, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Confirm & Save")
                }
            }
        }
    }
}

@Composable
private fun MealTypeSelector(
    selected: MealType,
    onSelect: (MealType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        MealType.entries.forEach { type ->
            TextButton(onClick = { onSelect(type) }) {
                Text(if (type == selected) "[${type.displayLabel()}]" else type.displayLabel())
            }
        }
    }
}

@Composable
private fun EditableItemRow(
    item: EditableItemState,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = item.name,
                onValueChange = { item.name = it },
                label = { Text("Food") },
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRemove) { Text("Remove") }
        }
        Row {
            OutlinedTextField(
                value = item.estimatedGrams,
                onValueChange = { item.estimatedGrams = it },
                label = { Text("Grams") },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = item.calories,
                onValueChange = { item.calories = it },
                label = { Text("kcal") },
                modifier = Modifier.weight(1f),
            )
        }
        Row {
            OutlinedTextField(
                value = item.proteinGrams,
                onValueChange = { item.proteinGrams = it },
                label = { Text("Protein g") },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = item.fatGrams,
                onValueChange = { item.fatGrams = it },
                label = { Text("Fat g") },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = item.carbsGrams,
                onValueChange = { item.carbsGrams = it },
                label = { Text("Carbs g") },
                modifier = Modifier.weight(1f),
            )
        }
        Text("Confidence: ${item.confidence.name.lowercase()}", style = MaterialTheme.typography.bodySmall)
    }
}
