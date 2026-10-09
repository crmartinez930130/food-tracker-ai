package com.foodtracker.app.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.ic_arrow_back
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private enum class CameraPermission { GRANTED, REQUESTING, DENIED, PERMANENTLY_DENIED }

/** The backend downsizes to 1568 px anyway, so ~2 MP keeps uploads small without losing detail. */
private val TARGET_CAPTURE_SIZE = Size(1920, 1440)

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
actual fun CameraCapture(
    onCaptured: (ByteArray) -> Unit,
    onCancel: () -> Unit,
    onUseGallery: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val alreadyGranted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    var permission by remember {
        mutableStateOf(if (alreadyGranted) CameraPermission.GRANTED else CameraPermission.REQUESTING)
    }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permission = when {
            granted -> CameraPermission.GRANTED
            // After a denial the system still lets us ask again only while a rationale is expected;
            // once it returns false the request would be auto-denied ("don't ask again").
            context.findActivity()?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == true ->
                CameraPermission.DENIED
            else -> CameraPermission.PERMANENTLY_DENIED
        }
    }

    LaunchedEffect(Unit) {
        if (permission == CameraPermission.REQUESTING) requestPermission.launch(Manifest.permission.CAMERA)
    }
    BackHandler(onBack = onCancel)

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        when (permission) {
            CameraPermission.GRANTED -> CameraViewfinder(onCaptured = onCaptured, onCancel = onCancel, onUseGallery = onUseGallery)
            CameraPermission.REQUESTING -> Unit
            CameraPermission.DENIED, CameraPermission.PERMANENTLY_DENIED -> CameraUnavailable(
                message = if (permission == CameraPermission.DENIED) {
                    "The camera is needed to take a photo of your meal."
                } else {
                    "The camera is needed to take a photo of your meal. Camera access is turned off for this " +
                        "app - you can enable it in the system settings."
                },
                onRetry = if (permission == CameraPermission.DENIED) {
                    { requestPermission.launch(Manifest.permission.CAMERA) }
                } else {
                    null
                },
                onUseGallery = onUseGallery,
                onCancel = onCancel,
            )
        }
    }
}

@Composable
private fun CameraViewfinder(
    onCaptured: (ByteArray) -> Unit,
    onCancel: () -> Unit,
    onUseGallery: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(TARGET_CAPTURE_SIZE, ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER),
                    )
                    .build(),
            )
            .build()
    }

    LaunchedEffect(lifecycleOwner) {
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { request -> surfaceRequest = request }
        }
        val provider = ProcessCameraProvider.awaitInstance(context)
        try {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            awaitCancellation()
        } catch (e: IllegalArgumentException) {
            // No back camera on this device.
            errorMessage = "No camera is available on this device."
        } finally {
            provider.unbindAll()
        }
    }

    val error = errorMessage
    if (error != null) {
        CameraUnavailable(message = error, onRetry = null, onUseGallery = onUseGallery, onCancel = onCancel)
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        surfaceRequest?.let { request ->
            CameraXViewfinder(surfaceRequest = request, modifier = Modifier.fillMaxSize())
        }

        Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp)) {
            IconButton(onClick = onCancel, modifier = Modifier.align(Alignment.TopStart)) {
                Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = "Cancel", tint = Color.White)
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .clickable(enabled = !isCapturing && surfaceRequest != null, role = Role.Button, onClickLabel = "Take photo") {
                        isCapturing = true
                        scope.launch {
                            try {
                                onCaptured(imageCapture.captureJpeg(context))
                            } catch (e: ImageCaptureException) {
                                errorMessage = "Couldn't take the photo: ${e.message ?: "unknown error"}"
                            } finally {
                                isCapturing = false
                            }
                        }
                    },
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                } else {
                    Box(Modifier.size(60.dp).clip(CircleShape).background(Color.White))
                }
            }
        }
    }
}

/** Saves to a temp file (CameraX writes the correct EXIF orientation there), reads it, deletes it. */
private suspend fun ImageCapture.captureJpeg(context: Context): ByteArray {
    val file = withContext(Dispatchers.IO) { File.createTempFile("meal_", ".jpg", context.cacheDir) }
    try {
        suspendCancellableCoroutine { continuation ->
            takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        continuation.resume(Unit)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                },
            )
        }
        return withContext(Dispatchers.IO) { file.readBytes() }
    } finally {
        withContext(Dispatchers.IO) { file.delete() }
    }
}

@Composable
private fun CameraUnavailable(
    message: String,
    onRetry: (() -> Unit)?,
    onUseGallery: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        if (onRetry != null) {
            Button(onClick = onRetry) { Text("Allow camera") }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(onClick = onUseGallery) { Text("Choose from gallery") }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}
