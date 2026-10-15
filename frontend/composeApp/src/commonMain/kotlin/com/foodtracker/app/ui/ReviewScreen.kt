package com.foodtracker.app.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.MealAnalysis
import com.foodtracker.app.data.MealItem
import com.foodtracker.app.data.MealType
import com.foodtracker.app.data.SaveMealRequest
import com.foodtracker.app.data.cache.OutboxRepository
import com.foodtracker.app.data.cache.SaveOutcome
import com.foodtracker.app.ui.components.BarSegment
import com.foodtracker.app.ui.components.BigNumber
import com.foodtracker.app.ui.components.ConnectedButtonGroup
import com.foodtracker.app.ui.components.SegmentedBar
import com.foodtracker.app.ui.components.pressBounce
import com.foodtracker.app.util.currentInstantIso
import com.foodtracker.app.util.currentLocalHour
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.action_cancel
import food_tracker_frontend.composeapp.generated.resources.error_save_meal
import food_tracker_frontend.composeapp.generated.resources.ic_close
import food_tracker_frontend.composeapp.generated.resources.ic_delete
import food_tracker_frontend.composeapp.generated.resources.macro_carbs
import food_tracker_frontend.composeapp.generated.resources.macro_fat
import food_tracker_frontend.composeapp.generated.resources.macro_protein
import food_tracker_frontend.composeapp.generated.resources.review_carbs_g
import food_tracker_frontend.composeapp.generated.resources.review_confidence
import food_tracker_frontend.composeapp.generated.resources.review_discard
import food_tracker_frontend.composeapp.generated.resources.review_discard_text
import food_tracker_frontend.composeapp.generated.resources.review_discard_title
import food_tracker_frontend.composeapp.generated.resources.review_fat_g
import food_tracker_frontend.composeapp.generated.resources.review_food
import food_tracker_frontend.composeapp.generated.resources.review_grams
import food_tracker_frontend.composeapp.generated.resources.review_kcal
import food_tracker_frontend.composeapp.generated.resources.review_keep_editing
import food_tracker_frontend.composeapp.generated.resources.review_no_food
import food_tracker_frontend.composeapp.generated.resources.review_notes
import food_tracker_frontend.composeapp.generated.resources.review_protein_g
import food_tracker_frontend.composeapp.generated.resources.review_remove
import food_tracker_frontend.composeapp.generated.resources.review_save_total
import food_tracker_frontend.composeapp.generated.resources.review_title
import food_tracker_frontend.composeapp.generated.resources.review_total_title
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The local hour of an ISO instant, e.g. a kept capture's time. */
@OptIn(ExperimentalTime::class)
private fun localHourOf(isoInstant: String): Int = Instant.parse(isoInstant).toLocalDateTime(TimeZone.currentSystemDefault()).hour

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
        estimatedGrams = editableNumber(estimatedGrams),
        confidence = confidence,
        calories = editableNumber(calories),
        proteinGrams = editableNumber(proteinGrams),
        fatGrams = editableNumber(fatGrams),
        carbsGrams = editableNumber(carbsGrams),
    )

    private companion object {
        var counter = 0
        fun nextId(): Int = counter++
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    initialAnalysis: MealAnalysis,
    outbox: OutboxRepository,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    /** A kept capture's time and id (offline-queue); by default the meal takes the Save tap's time. */
    occurredAt: String? = null,
    clientId: String? = null,
) {
    val items = remember(initialAnalysis) {
        mutableStateListOf(*initialAnalysis.items.map { EditableItemState(it) }.toTypedArray())
    }
    var notes by remember { mutableStateOf(initialAnalysis.notes ?: "") }
    // A kept capture suggests the type of its own time (a lunch photo reviewed at night is still lunch).
    var mealType by remember { mutableStateOf(suggestedMealType(occurredAt?.let { localHourOf(it) } ?: currentLocalHour())) }
    var isSaving by remember { mutableStateOf(false) }
    var showDiscardPrompt by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val totals = mealTotals(items.map { it.toMealItem() })

    fun save() {
        isSaving = true
        errorMessage = null
        scope.launch {
            try {
                // Stamped with this tap (or the capture's time): sent now, or queued with it when the
                // backend can't be reached.
                val outcome = outbox.saveMeal(
                    SaveMealRequest(
                        items = items.map { it.toMealItem() },
                        notes = notes.ifBlank { null },
                        occurredAt = occurredAt ?: currentInstantIso(),
                        mealType = mealType,
                        clientId = clientId,
                    ),
                )
                when (outcome) {
                    is SaveOutcome.Saved, SaveOutcome.Queued -> onSaved()
                    is SaveOutcome.Rejected -> errorMessage = errorText(Res.string.error_save_meal, outcome.error)
                }
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
            title = { Text(stringResource(Res.string.review_discard_title)) },
            text = { Text(stringResource(Res.string.review_discard_text)) },
            confirmButton = {
                TextButton(onClick = onCancel) {
                    Text(stringResource(Res.string.review_discard), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardPrompt = false }) { Text(stringResource(Res.string.review_keep_editing)) }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.review_title)) },
                navigationIcon = {
                    // The cancel action: asks before discarding, like back (see reviewBackAction).
                    IconButton(onClick = { showDiscardPrompt = true }, enabled = !isSaving) {
                        Icon(painterResource(Res.drawable.ic_close), contentDescription = stringResource(Res.string.action_cancel))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Room under the last card for the fixed save pill.
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "type") {
                    ConnectedButtonGroup(
                        options = MealType.entries.map { it.displayLabel() },
                        selectedIndex = mealType.ordinal,
                        onSelect = { mealType = MealType.entries[it] },
                        showCheck = false,
                    )
                }
                if (items.isEmpty()) {
                    item(key = "empty") { Text(stringResource(Res.string.review_no_food)) }
                }
                items(items, key = { it.id }) { item ->
                    EditableItemCard(item, onRemove = { items.remove(item) })
                }
                item(key = "notes") {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(Res.string.review_notes)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item(key = "total") { TotalCard(totals) }
                errorMessage?.let { message ->
                    item(key = "error") { Text(message, color = MaterialTheme.colorScheme.error) }
                }
            }

            // Fixed over the list, so saving (and the total) is always one tap away.
            val saveInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = { save() },
                enabled = !isSaving,
                shape = CircleShape,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                interactionSource = saveInteraction,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(56.dp)
                    .pressBounce(saveInteraction),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                    Spacer(Modifier.width(12.dp))
                }
                Text(stringResource(Res.string.review_save_total, totals.calories.roundToInt()), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** The meal's total as a big figure with its macro composition. */
@Composable
private fun TotalCard(totals: MealTotals, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(Res.string.review_total_title), style = MaterialTheme.typography.titleMedium)
            BigNumber(
                value = "${totals.calories.roundToInt()}",
                unit = "kcal",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                unitColor = colors.onPrimaryContainer,
            )
            SegmentedBar(
                segments = listOf(
                    BarSegment(stringResource(Res.string.macro_protein), totals.proteinGrams, colors.primary, "${totals.proteinGrams.roundToInt()} g"),
                    BarSegment(stringResource(Res.string.macro_fat), totals.fatGrams, colors.tertiary, "${totals.fatGrams.roundToInt()} g"),
                    BarSegment(stringResource(Res.string.macro_carbs), totals.carbsGrams, colors.secondary, "${totals.carbsGrams.roundToInt()} g"),
                ),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun EditableItemCard(
    item: EditableItemState,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow, contentColor = colors.onSurface),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { item.name = it },
                    label = { Text(stringResource(Res.string.review_food)) },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(painterResource(Res.drawable.ic_delete), contentDescription = stringResource(Res.string.review_remove), tint = colors.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(end = 8.dp)) {
                NumberField(item.estimatedGrams, { item.estimatedGrams = it }, stringResource(Res.string.review_grams), Modifier.weight(1f))
                NumberField(item.calories, { item.calories = it }, stringResource(Res.string.review_kcal), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(end = 8.dp)) {
                NumberField(item.proteinGrams, { item.proteinGrams = it }, stringResource(Res.string.review_protein_g), Modifier.weight(1f))
                NumberField(item.fatGrams, { item.fatGrams = it }, stringResource(Res.string.review_fat_g), Modifier.weight(1f))
                NumberField(item.carbsGrams, { item.carbsGrams = it }, stringResource(Res.string.review_carbs_g), Modifier.weight(1f))
            }
            Surface(shape = CircleShape, color = colors.secondaryContainer) {
                Text(
                    stringResource(Res.string.review_confidence, stringResource(item.confidence.labelRes)),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}
