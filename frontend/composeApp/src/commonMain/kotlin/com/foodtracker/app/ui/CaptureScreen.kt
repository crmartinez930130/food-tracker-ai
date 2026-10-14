package com.foodtracker.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.MealAnalysis
import com.foodtracker.app.data.MealEntry
import com.foodtracker.app.data.Portion
import com.foodtracker.app.data.toReviewAnalysis
import com.foodtracker.app.ui.components.BigNumber
import com.foodtracker.app.ui.components.ConnectedButtonGroup
import com.foodtracker.app.ui.components.EmptyIllustration
import com.foodtracker.app.ui.components.ExpressiveLoadingIndicator
import com.foodtracker.app.ui.components.IllustrationKind
import com.foodtracker.app.ui.components.pressBounce
import com.foodtracker.app.ui.settings.LocalAppLocale
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.capture_analyze
import food_tracker_frontend.composeapp.generated.resources.capture_analyzing
import food_tracker_frontend.composeapp.generated.resources.capture_choose_again
import food_tracker_frontend.composeapp.generated.resources.capture_choose_gallery
import food_tracker_frontend.composeapp.generated.resources.capture_close
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
import food_tracker_frontend.composeapp.generated.resources.ic_close
import food_tracker_frontend.composeapp.generated.resources.ic_mic
import food_tracker_frontend.composeapp.generated.resources.ic_photo_camera
import food_tracker_frontend.composeapp.generated.resources.ic_photo_library
import food_tracker_frontend.composeapp.generated.resources.ic_star_filled
import food_tracker_frontend.composeapp.generated.resources.no_items
import food_tracker_frontend.composeapp.generated.resources.portion_large
import food_tracker_frontend.composeapp.generated.resources.portion_medium
import food_tracker_frontend.composeapp.generated.resources.portion_small
import food_tracker_frontend.composeapp.generated.resources.remove_from_favorites
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private enum class PhotoSource { CAMERA, GALLERY }

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
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

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.capture_title)) },
                navigationIcon = {
                    // Hidden while analyzing, like back (see captureBackAction).
                    if (!isAnalyzing) {
                        IconButton(onClick = onCancel) {
                            Icon(painterResource(Res.drawable.ic_close), contentDescription = stringResource(Res.string.capture_close))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            val currentPhoto = photo
            if (currentPhoto == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    PhotoTile(
                        label = stringResource(Res.string.capture_take_photo),
                        icon = Res.drawable.ic_photo_camera,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        onClick = { showCamera = true },
                        modifier = Modifier.weight(1f),
                    )
                    PhotoTile(
                        label = stringResource(Res.string.capture_choose_gallery),
                        icon = Res.drawable.ic_photo_library,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        onClick = { pickerLauncher.launch() },
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                val bitmap = preview
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = stringResource(Res.string.capture_selected_photo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).clip(MaterialTheme.shapes.extraLarge),
                    )
                } else {
                    Text(stringResource(Res.string.capture_photo_size, currentPhoto.size / 1024))
                }
                if (!isAnalyzing) {
                    val retake = photoSource == PhotoSource.CAMERA
                    FilledTonalButton(
                        onClick = {
                            if (retake) {
                                photo = null
                                showCamera = true
                            } else {
                                pickerLauncher.launch()
                            }
                        },
                        modifier = Modifier.padding(top = 12.dp).align(Alignment.CenterHorizontally),
                    ) {
                        Icon(
                            painterResource(if (retake) Res.drawable.ic_photo_camera else Res.drawable.ic_photo_library),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(if (retake) Res.string.capture_retake else Res.string.capture_choose_again))
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
            Spacer(Modifier.height(16.dp))
            PortionButtons(selected = portion, onSelect = { portion = toggledPortion(portion, it) }, enabled = !isAnalyzing)
            Spacer(Modifier.height(24.dp))
            if (isAnalyzing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    ExpressiveLoadingIndicator(
                        size = 72.dp,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        stringResource(Res.string.capture_analyzing),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            } else {
                val analyzeInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = { analyze() },
                    enabled = canAnalyze(hasPhoto = currentPhoto != null, description = description),
                    shape = CircleShape,
                    interactionSource = analyzeInteraction,
                    modifier = Modifier.fillMaxWidth().height(56.dp).pressBounce(analyzeInteraction),
                ) { Text(stringResource(Res.string.capture_analyze), style = MaterialTheme.typography.titleMedium) }
            }

            errorMessage?.let { message ->
                Spacer(Modifier.height(16.dp))
                Text(message, color = MaterialTheme.colorScheme.error)
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
        }
    }
}

/** A large camera/gallery choice: an icon over its label on a tinted card. */
@Composable
private fun PhotoTile(
    label: String,
    icon: DrawableResource,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier.height(132.dp).pressBounce(interaction),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(12.dp),
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(32.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
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
        Spacer(Modifier.height(12.dp))
        when {
            error != null -> Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            favorites == null -> ExpressiveLoadingIndicator(size = 32.dp)
            favorites.isEmpty() -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                EmptyIllustration(IllustrationKind.STAR)
                Text(
                    stringResource(Res.string.capture_favorites_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(favorites, key = { it.id }) { meal ->
                    FavoriteCard(meal, onRelog = { onRelog(meal) }, onRemove = { onRemove(meal) })
                }
            }
        }
    }
}

@Composable
private fun FavoriteCard(meal: MealEntry, onRelog: () -> Unit, onRemove: () -> Unit) {
    val relogLabel = stringResource(Res.string.capture_relog)
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(140.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClickLabel = relogLabel, onClick = onRelog),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.fillMaxSize().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    meal.items.joinToString(", ") { it.name }.ifBlank { stringResource(Res.string.no_items) },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(top = 4.dp),
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        painterResource(Res.drawable.ic_star_filled),
                        contentDescription = stringResource(Res.string.remove_from_favorites),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            BigNumber(
                value = "${meal.totalCalories.roundToInt()}",
                unit = "kcal",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                unitStyle = MaterialTheme.typography.labelMedium,
            )
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

/** Optional portion size: none selected by default, and tapping the selected one clears it (see toggledPortion). */
@Composable
private fun PortionButtons(selected: Portion?, onSelect: (Portion) -> Unit, enabled: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.capture_portion), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
        ConnectedButtonGroup(
            options = PORTION_LABELS.map { (_, label) -> stringResource(label) },
            selectedIndex = PORTION_LABELS.indexOfFirst { (value, _) -> value == selected },
            onSelect = { onSelect(PORTION_LABELS[it].first) },
            enabled = enabled,
        )
    }
}
