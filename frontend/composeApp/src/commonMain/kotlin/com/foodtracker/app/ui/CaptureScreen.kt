package com.foodtracker.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.MealAnalysis
import com.foodtracker.app.data.MealEntry
import com.foodtracker.app.data.toReviewAnalysis
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.ic_star_filled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

private enum class PhotoSource { CAMERA, GALLERY }

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
            favoritesError = "Couldn't load favorites: ${e.message ?: "unknown error"}"
        }
    }

    fun removeFavorite(meal: MealEntry) {
        scope.launch {
            try {
                apiClient.setFavorite(meal.id, false)
                favoritesReloadKey++
            } catch (e: Exception) {
                favoritesError = "Couldn't update favorite: ${e.message ?: "unknown error"}"
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

    fun analyze() {
        val bytes = photo ?: return
        isAnalyzing = true
        errorMessage = null
        scope.launch {
            try {
                onAnalyzed(apiClient.analyzeMealPhoto(bytes))
            } catch (e: Exception) {
                errorMessage = "Couldn't analyze the photo: ${e.message ?: "unknown error"}"
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Log a meal", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        val currentPhoto = photo
        if (currentPhoto == null) {
            Button(onClick = { showCamera = true }, modifier = Modifier.fillMaxWidth()) { Text("Take photo") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { pickerLauncher.launch() }, modifier = Modifier.fillMaxWidth()) {
                Text("Choose from gallery")
            }
            Spacer(Modifier.height(32.dp))
            FavoritesSection(
                favorites = favorites,
                error = favoritesError,
                // Re-logging skips analysis entirely: the saved foods go straight to review.
                onRelog = { meal -> onAnalyzed(meal.toReviewAnalysis()) },
                onRemove = ::removeFavorite,
            )
        } else {
            val bitmap = preview
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Selected meal photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Text("Photo selected (${currentPhoto.size / 1024} KB)")
            }
            Spacer(Modifier.height(16.dp))
            if (isAnalyzing) {
                CircularProgressIndicator()
            } else {
                Row {
                    if (photoSource == PhotoSource.CAMERA) {
                        OutlinedButton(onClick = { photo = null; showCamera = true }) { Text("Retake") }
                    } else {
                        OutlinedButton(onClick = { pickerLauncher.launch() }) { Text("Choose again") }
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = { analyze() }) { Text("Analyze") }
                }
            }
        }

        errorMessage?.let { message ->
            Spacer(Modifier.height(16.dp))
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onCancel) { Text("Cancel") }
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
        Text("Favorites", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        when {
            error != null -> Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            favorites == null -> CircularProgressIndicator(Modifier.size(24.dp))
            favorites.isEmpty() -> Text(
                "Star a meal on Home to log it again here in one tap.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> favorites.forEach { meal ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClickLabel = "Log this meal again") { onRelog(meal) }
                        .padding(vertical = 8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(meal.items.joinToString(", ") { it.name }.ifBlank { "(no items)" }, maxLines = 2)
                        Text(
                            "${meal.totalCalories.roundToInt()} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onRemove(meal) }) {
                        Icon(
                            painterResource(Res.drawable.ic_star_filled),
                            contentDescription = "Remove from favorites",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

