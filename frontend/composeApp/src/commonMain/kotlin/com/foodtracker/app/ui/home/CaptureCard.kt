package com.foodtracker.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.ApiException
import com.foodtracker.app.data.cache.CaptureRow
import com.foodtracker.app.data.cache.CaptureStatus
import com.foodtracker.app.data.cache.MISSING_PHOTO_ERROR
import com.foodtracker.app.ui.decodePreviewBitmap
import com.foodtracker.app.ui.errorText
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.capture_failed
import food_tracker_frontend.composeapp.generated.resources.capture_photo_missing
import food_tracker_frontend.composeapp.generated.resources.capture_photo_only
import food_tracker_frontend.composeapp.generated.resources.capture_ready
import food_tracker_frontend.composeapp.generated.resources.capture_review
import food_tracker_frontend.composeapp.generated.resources.capture_waiting
import food_tracker_frontend.composeapp.generated.resources.outbox_discard
import food_tracker_frontend.composeapp.generated.resources.outbox_retry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * A meal captured without a connection (offline-queue): its thumbnail or description, its time and
 * state, and what the user can do in that state. It never counts in the day's totals.
 */
@Composable
fun CaptureCard(
    capture: CaptureRow,
    loadPhoto: suspend () -> ByteArray?,
    onReview: () -> Unit,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var thumbnail by remember(capture.photoFile) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(capture.photoFile) {
        thumbnail = loadPhoto()?.let { bytes -> withContext(Dispatchers.Default) { bytes.decodePreviewBitmap(maxDimension = 240) } }
    }
    val time = localClockTime(capture.capturedAt)
    val state = when (capture.status) {
        CaptureStatus.WAITING -> stringResource(Res.string.capture_waiting, time)
        CaptureStatus.READY -> stringResource(Res.string.capture_ready, time)
        CaptureStatus.FAILED -> failedText(capture)
    }
    val failed = capture.status == CaptureStatus.FAILED
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = if (failed) {
            CardDefaults.cardColors(containerColor = colors.errorContainer, contentColor = colors.onErrorContainer)
        } else {
            CardDefaults.cardColors(containerColor = colors.surfaceContainerLow, contentColor = colors.onSurface)
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)) {
            thumbnail?.let {
                Image(
                    it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.medium),
                )
            }
            Column(Modifier.weight(1f).padding(start = if (thumbnail != null) 12.dp else 0.dp)) {
                Text(
                    capture.description ?: stringResource(Res.string.capture_photo_only),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                )
                Text(
                    state,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (failed) colors.onErrorContainer else colors.onSurfaceVariant,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            TextButton(onClick = onDiscard) { Text(stringResource(Res.string.outbox_discard)) }
            when (capture.status) {
                CaptureStatus.WAITING -> Unit
                CaptureStatus.READY -> TextButton(onClick = onReview) { Text(stringResource(Res.string.capture_review)) }
                CaptureStatus.FAILED -> TextButton(onClick = onRetry) { Text(stringResource(Res.string.outbox_retry)) }
            }
        }
    }
}

/** "Couldn't analyze it: …", with the app's own text for a rejected token or a missing photo. */
@Composable
private fun failedText(capture: CaptureRow): String {
    var text by remember(capture.error, capture.errorStatus) { mutableStateOf("") }
    LaunchedEffect(capture.error, capture.errorStatus) {
        text = if (capture.error == MISSING_PHOTO_ERROR) {
            getString(Res.string.capture_failed, getString(Res.string.capture_photo_missing))
        } else {
            errorText(Res.string.capture_failed, ApiException(capture.error.orEmpty(), capture.errorStatus ?: 0))
        }
    }
    return text
}
