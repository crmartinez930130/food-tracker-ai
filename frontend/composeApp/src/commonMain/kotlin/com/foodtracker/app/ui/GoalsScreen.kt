package com.foodtracker.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.DailyGoal
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.UpdateGoalsRequest
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.action_back
import food_tracker_frontend.composeapp.generated.resources.error_load_goal
import food_tracker_frontend.composeapp.generated.resources.error_save_goal
import food_tracker_frontend.composeapp.generated.resources.goals_calories
import food_tracker_frontend.composeapp.generated.resources.goals_carbs
import food_tracker_frontend.composeapp.generated.resources.goals_default_hint
import food_tracker_frontend.composeapp.generated.resources.goals_error_calories
import food_tracker_frontend.composeapp.generated.resources.goals_error_glass
import food_tracker_frontend.composeapp.generated.resources.goals_error_macros
import food_tracker_frontend.composeapp.generated.resources.goals_error_numbers
import food_tracker_frontend.composeapp.generated.resources.goals_error_water
import food_tracker_frontend.composeapp.generated.resources.goals_fat
import food_tracker_frontend.composeapp.generated.resources.goals_glass_hint
import food_tracker_frontend.composeapp.generated.resources.goals_glass_size
import food_tracker_frontend.composeapp.generated.resources.goals_protein
import food_tracker_frontend.composeapp.generated.resources.goals_save
import food_tracker_frontend.composeapp.generated.resources.goals_saved
import food_tracker_frontend.composeapp.generated.resources.goals_title
import food_tracker_frontend.composeapp.generated.resources.goals_water_section
import food_tracker_frontend.composeapp.generated.resources.goals_water_target
import food_tracker_frontend.composeapp.generated.resources.ic_arrow_back
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Shows whole numbers without a trailing ".0" so the fields read like what the user typed. */
private fun Double.toFieldText(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun GoalsScreen(apiClient: FoodTrackerApiClient, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var water by remember { mutableStateOf("") }
    var glass by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun showGoal(goal: DailyGoal) {
        calories = goal.targetCalories.toFieldText()
        protein = goal.targetProteinGrams.toFieldText()
        fat = goal.targetFatGrams.toFieldText()
        carbs = goal.targetCarbsGrams.toFieldText()
        water = goal.targetWaterMl.toString()
        glass = goal.waterGlassMl.toString()
        isDefault = goal.isDefault
    }

    LaunchedEffect(Unit) {
        try {
            showGoal(apiClient.getGoals())
        } catch (e: Exception) {
            errorMessage = errorText(Res.string.error_load_goal, e)
        } finally {
            isLoading = false
        }
    }

    fun save() {
        savedMessage = null
        val request = UpdateGoalsRequest(
            targetCalories = calories.toDoubleOrNull() ?: Double.NaN,
            targetProteinGrams = protein.toDoubleOrNull() ?: Double.NaN,
            targetFatGrams = fat.toDoubleOrNull() ?: Double.NaN,
            targetCarbsGrams = carbs.toDoubleOrNull() ?: Double.NaN,
            targetWaterMl = water.trim().toIntOrNull() ?: -1,
            waterGlassMl = glass.trim().toIntOrNull() ?: -1,
        )
        // Same rules the backend enforces, checked here first for a friendlier message.
        val clientError = when {
            listOf(request.targetCalories, request.targetProteinGrams, request.targetFatGrams, request.targetCarbsGrams)
                .any { it.isNaN() } -> Res.string.goals_error_numbers
            request.targetCalories <= 0 -> Res.string.goals_error_calories
            listOf(request.targetProteinGrams, request.targetFatGrams, request.targetCarbsGrams).any { it < 0 } ->
                Res.string.goals_error_macros
            request.targetWaterMl !in 1..20000 -> Res.string.goals_error_water
            request.waterGlassMl !in 1..5000 -> Res.string.goals_error_glass
            else -> null
        }
        if (clientError != null) {
            scope.launch { errorMessage = getString(clientError) }
            return
        }
        isSaving = true
        errorMessage = null
        scope.launch {
            try {
                showGoal(apiClient.updateGoals(request))
                savedMessage = getString(Res.string.goals_saved)
            } catch (e: Exception) {
                // Keep the user's edits on screen so they can correct them.
                errorMessage = errorText(Res.string.error_save_goal, e)
            } finally {
                isSaving = false
            }
        }
    }

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.goals_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = stringResource(Res.string.action_back))
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator()
                return@Column
            }

            if (isDefault) {
                Text(
                    stringResource(Res.string.goals_default_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
            }

            GoalField(stringResource(Res.string.goals_calories), calories, onValueChange = { calories = it })
            GoalField(stringResource(Res.string.goals_protein), protein, onValueChange = { protein = it })
            GoalField(stringResource(Res.string.goals_fat), fat, onValueChange = { fat = it })
            GoalField(stringResource(Res.string.goals_carbs), carbs, onValueChange = { carbs = it })

            Spacer(Modifier.height(16.dp))
            Text(stringResource(Res.string.goals_water_section), style = MaterialTheme.typography.titleMedium)
            GoalField(stringResource(Res.string.goals_water_target), water, onValueChange = { water = it }, keyboardType = KeyboardType.Number)
            GoalField(stringResource(Res.string.goals_glass_size), glass, onValueChange = { glass = it }, keyboardType = KeyboardType.Number)
            Text(
                stringResource(Res.string.goals_glass_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            errorMessage?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(message, color = MaterialTheme.colorScheme.error)
            }
            savedMessage?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(message, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = { save() }, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text(stringResource(Res.string.goals_save))
                }
            }
        }
    }
}

@Composable
private fun GoalField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}
