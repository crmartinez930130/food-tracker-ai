package com.foodtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.MealAnalysis
import kotlinx.coroutines.launch

@Composable
fun CaptureScreen(
    apiClient: FoodTrackerApiClient,
    onAnalyzed: (MealAnalysis) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickedPhoto by remember { mutableStateOf<ByteArray?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val pickerLauncher = rememberImagePickerLauncher { bytes ->
        pickedPhoto = bytes
        errorMessage = null
    }

    fun analyze() {
        val photo = pickedPhoto ?: return
        isAnalyzing = true
        errorMessage = null
        scope.launch {
            try {
                val analysis = apiClient.analyzeMealPhoto(photo)
                onAnalyzed(analysis)
            } catch (e: Exception) {
                errorMessage = "Couldn't analyze the photo: ${e.message ?: "unknown error"}"
            } finally {
                isAnalyzing = false
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Log a meal", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        val photo = pickedPhoto
        if (photo == null) {
            Button(onClick = { pickerLauncher.launch() }) { Text("Choose a photo") }
        } else {
            Text("Photo selected (${photo.size / 1024} KB)")
            Spacer(Modifier.height(16.dp))
            if (isAnalyzing) {
                CircularProgressIndicator()
            } else {
                Row {
                    OutlinedButton(onClick = { pickedPhoto = null }) { Text("Choose again") }
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
