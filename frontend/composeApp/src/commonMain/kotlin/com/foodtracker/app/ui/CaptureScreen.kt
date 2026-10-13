package com.foodtracker.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.MealAnalysis
import com.foodtracker.app.data.MealEntry
import com.foodtracker.app.data.Portion
import com.foodtracker.app.data.toReviewAnalysis
import com.foodtracker.app.ui.components.ExpressiveLoadingIndicator
import com.foodtracker.app.ui.settings.LocalAppLocale
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.action_cancel
import food_tracker_frontend.composeapp.generated.resources.capture_analyze
import food_tracker_frontend.composeapp.generated.resources.capture_choose_again
import food_tracker_frontend.composeapp.generated.resources.capture_choose_gallery
import food_tracker_frontend.composeapp.generated.resources.capture_description_counter
import food_tracker_frontend.composeapp.generated.resources.capture_description_label
import food_tracker_frontend.composeapp.generated.resources.capture_description_placeholder
import food_tracker_frontend.composeapp.generated.resources.capture_dictate
import food_tracker_frontend.composeapp.generated.resources.capture_dictation_prompt
import food_tracker_frontend.composeapp.generated.resources.capture_favorites
import food_tracker_frontend.composeapp.generated.resources.capture_favorites_empty
import food_tracker_frontend.composeapp.generated.resources.capture_or_describe
import food_tracker_frontend.composeapp.generated.resources.capture_photo_size
import food_tracker_frontend.composeapp.generated.resources.capture_portion
import food_tracker_frontend.composeapp.generated.resources.capture_relog
import food_tracker_frontend.composeapp.generated.resources.capture_retake
import food_tracker_frontend.composeapp.generated.resources.capture_selected_photo
import food_tracker_frontend.composeapp.generated.resources.capture_take_photo
import food_tracker_frontend.composeapp.generated.resources.capture_title
import food_tracker_frontend.composeapp.generated.resources.error_analyze_photo
import food_tracker_frontend.composeapp.generated.resources.error_load_favorites
import food_tracker_frontend.composeapp.generated.resources.error_update_favorite
import food_tracker_frontend.composeapp.generated.resources.ic_mic
import food_tracker_frontend.composeapp.generated.resources.ic_star_filled
import food_tracker_frontend.composeapp.generated.resources.no_items
import food_tracker_frontend.composeapp.generated.resources.portion_large
import food_tracker_frontend.composeapp.generated.resources.portion_medium
import food_tracker_frontend.composeapp.generated.resources.portion_small
import food_tracker_frontend.composeapp.generated.resources.remove_from_favorites
import food_tracker_frontend.composeapp.generated.resources.unit_kcal
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private enum class PhotoSource { CAMERA, GALLERY }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CaptureScreen(
    apiClient: FoodTrackerApiClient,
    onAnalyzed: (MealAnalysis) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var photo by remember { mutableStateOf<ByteArray?>(null) }
    var photoSource by remember { mutableStateOf<PhotoSource?>(null) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    // The camera is an in-place mode of this screen, so a captured photo lands in the same state the
    // gallery picker writes to.
    var showCamera by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    // Optional context for the analysis (and the only input when logging without a photo).
    var description by remember { mutableStateOf("") }
    var portion by remember { mutableStateOf<Portion?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var favorites by remember { mutableStateOf<List<MealEntry>?>(null) }
    var favoritesError by remember { mutableStateOf<String?>(null) }
    var favoritesReloadKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(favoritesReloadKey) {
        favoritesError = null
        try {
            favorites = apiClient.listFavorites()
        } catch (e: Exception) {
            // Non-blocking: the photo options keep working without the list.
            favoritesError = errorText(Res.string.error_load_favorites, e)
        }
    }

    fun removeFavorite(meal: MealEntry) {
        scope.launch {
            try {
                apiClient.setFavorite(meal.id, false)
                favoritesReloadKey++
            } catch (e: Exception) {
                favoritesError = errorText(Res.string.error_update_favorite, e)
            }
        }
    }

    fun usePhoto(bytes: ByteArray, source: PhotoSource) {
        photo = bytes
        photoSource = source
        errorMessage = null
    }

    val pickerLauncher = rememberImagePickerLauncher { bytes -> usePhoto(bytes, PhotoSource.GALLERY) }

    LaunchedEffect(photo) {
        val bytes = photo
        preview = null
        if (bytes != null) {
            preview = withContext(Dispatchers.Default) { bytes.decodePreviewBitmap() }
        }
    }

    // The language the app is showing, so the analysis names and notes come back in it.
    val language = LocalAppLocale.current

    val speechInput = rememberSpeechInput(
        language = language,
        prompt = stringResource(Res.string.capture_dictation_prompt),
        onResult = { dictated -> description = appendDictation(description, dictated) },
    )

    fun analyze() {
        if (!canAnalyze(hasPhoto = photo != null, description = description)) return
        val bytes = photo
        isAnalyzing = true
        errorMessage = null
        scope.launch {
            try {
                onAnalyzed(apiClient.analyzeMeal(bytes, description, portion, language))
            } catch (e: Exception) {
                errorMessage = errorText(Res.string.error_analyze_photo, e)
            } finally {
                isAnalyzing = false
            }
        }
    }

    if (showCamera) {
        CameraCapture(
            onCaptured = { bytes ->
                usePhoto(bytes, PhotoSource.CAMERA)
                showCamera = false
            },
            onCancel = { showCamera = false },
            onUseGallery = {
                showCamera = false
                pickerLauncher.launch()
            },
        )
        return
    }

    // Composed only outside the camera, which handles its own back (CameraCapture).
    BackHandler {
        when (captureBackAction(hasPhotoToConfirm = photo != null, isAnalyzing = isAnalyzing)) {
            CaptureBack.IGNORE -> Unit
            CaptureBack.DISCARD_PHOTO -> photo = null
            CaptureBack.EXIT -> onCancel()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(Res.string.capture_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        val currentPhoto = photo
        if (currentPhoto == null) {
            Button(onClick = { showCamera = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.capture_take_photo)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { pickerLauncher.launch() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.capture_choose_gallery))
            }
        } else {
            val bitmap = preview
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(Res.string.capture_selected_photo),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Text(stringResource(Res.string.capture_photo_size, currentPhoto.size / 1024))
            }
            if (!isAnalyzing) {
                Spacer(Modifier.height(8.dp))
                if (photoSource == PhotoSource.CAMERA) {
                    OutlinedButton(onClick = { photo = null; showCamera = true }) { Text(stringResource(Res.string.capture_retake)) }
                } else {
                    OutlinedButton(onClick = { pickerLauncher.launch() }) { Text(stringResource(Res.string.capture_choose_again)) }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        if (currentPhoto == null) {
            Text(
                stringResource(Res.string.capture_or_describe),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }
        DescriptionField(
            value = description,
            onValueChange = { description = it.take(MAX_DESCRIPTION_LENGTH) },
            speechInput = speechInput,
            enabled = !isAnalyzing,
        )
        Spacer(Modifier.height(12.dp))
        PortionChips(selected = portion, onSelect = { portion = it }, enabled = !isAnalyzing)
        Spacer(Modifier.height(16.dp))
        if (isAnalyzing) {
            ExpressiveLoadingIndicator(size = 56.dp)
        } else {
            Button(
                onClick = { analyze() },
                enabled = canAnalyze(hasPhoto = currentPhoto != null, description = description),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.capture_analyze)) }
        }

        // Favorites are the "nothing chosen yet" shortcut; once a photo or description exists they'd compete.
        if (currentPhoto == null && description.isBlank()) {
            Spacer(Modifier.height(32.dp))
            FavoritesSection(
                favorites = favorites,
                error = favoritesError,
                // Re-logging skips analysis entirely: the saved foods go straight to review.
                onRelog = { meal -> onAnalyzed(meal.toReviewAnalysis()) },
                onRemove = ::removeFavorite,
            )
        }

        errorMessage?.let { message ->
            Spacer(Modifier.height(16.dp))
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onCancel) { Text(stringResource(Res.string.action_cancel)) }
    }
}

@Composable
private fun FavoritesSection(
    favorites: List<MealEntry>?,
    error: String?,
    onRelog: (MealEntry) -> Unit,
    onRemove: (MealEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.capture_favorites), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        when {
            error != null -> Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            favorites == null -> ExpressiveLoadingIndicator(size = 32.dp)
            favorites.isEmpty() -> Text(
                stringResource(Res.string.capture_favorites_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> favorites.forEach { meal ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClickLabel = stringResource(Res.string.capture_relog)) { onRelog(meal) }
                        .padding(vertical = 8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(meal.items.joinToString(", ") { it.name }.ifBlank { stringResource(Res.string.no_items) }, maxLines = 2)
                        Text(
                            stringResource(Res.string.unit_kcal, meal.totalCalories.roundToInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onRemove(meal) }) {
                        Icon(
                            painterResource(Res.drawable.ic_star_filled),
                            contentDescription = stringResource(Res.string.remove_from_favorites),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

/** Shown under the field only near the limit, so the counter doesn't clutter normal use. */
private const val DESCRIPTION_COUNTER_FROM = 400

@Composable
private fun DescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    speechInput: SpeechInput?,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(Res.string.capture_description_label)) },
        placeholder = { Text(stringResource(Res.string.capture_description_placeholder)) },
        minLines = 2,
        maxLines = 5,
        enabled = enabled,
        trailingIcon = speechInput?.let { input ->
            {
                IconButton(onClick = { input.start() }, enabled = enabled) {
                    Icon(painterResource(Res.drawable.ic_mic), contentDescription = stringResource(Res.string.capture_dictate))
                }
            }
        },
        supportingText = if (value.length > DESCRIPTION_COUNTER_FROM) {
            { Text(stringResource(Res.string.capture_description_counter, value.length, MAX_DESCRIPTION_LENGTH)) }
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

private val PORTION_LABELS = listOf(
    Portion.SMALL to Res.string.portion_small,
    Portion.MEDIUM to Res.string.portion_medium,
    Portion.LARGE to Res.string.portion_large,
)

/** Optional portion size: none selected by default, and tapping the selected one clears it. */
@Composable
private fun PortionChips(selected: Portion?, onSelect: (Portion?) -> Unit, enabled: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.capture_portion), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((value, label) in PORTION_LABELS) {
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(if (selected == value) null else value) },
                    label = { Text(stringResource(label)) },
                    enabled = enabled,
                )
            }
        }
    }
}
