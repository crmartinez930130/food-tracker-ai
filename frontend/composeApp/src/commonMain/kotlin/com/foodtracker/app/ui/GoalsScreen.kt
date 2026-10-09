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
import food_tracker_frontend.composeapp.generated.resources.ic_arrow_back
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/** Shows whole numbers without a trailing ".0" so the fields read like what the user typed. */
private fun Double.toFieldText(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun GoalsScreen(apiClient: FoodTrackerApiClient, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
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
        isDefault = goal.isDefault
    }

    LaunchedEffect(Unit) {
        try {
            showGoal(apiClient.getGoals())
        } catch (e: Exception) {
            errorMessage = "Couldn't load your goal: ${e.message ?: "unknown error"}"
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
        )
        // Same rules the backend enforces, checked here first for a friendlier message.
        val clientError = when {
            listOf(request.targetCalories, request.targetProteinGrams, request.targetFatGrams, request.targetCarbsGrams)
                .any { it.isNaN() } -> "Enter a number in every field."
            request.targetCalories <= 0 -> "Calories must be greater than 0."
            listOf(request.targetProteinGrams, request.targetFatGrams, request.targetCarbsGrams).any { it < 0 } ->
                "Protein, fat and carbs can't be negative."
            else -> null
        }
        if (clientError != null) {
            errorMessage = clientError
            return
        }
        isSaving = true
        errorMessage = null
        scope.launch {
            try {
                showGoal(apiClient.updateGoals(request))
                savedMessage = "Goal saved."
            } catch (e: Exception) {
                // Keep the user's edits on screen so they can correct them.
                errorMessage = "Couldn't save your goal: ${e.message ?: "unknown error"}"
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
                title = { Text("Daily goal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = "Back")
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
                    "You're using the default goal. Adjust it to your needs and save.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
            }

            GoalField("Calories (kcal)", calories, onValueChange = { calories = it })
            GoalField("Protein (g)", protein, onValueChange = { protein = it })
            GoalField("Fat (g)", fat, onValueChange = { fat = it })
            GoalField("Carbs (g)", carbs, onValueChange = { carbs = it })

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
                    Text("Save goal")
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
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}
