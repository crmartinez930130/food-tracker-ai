package com.foodtracker.app.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Uses the system recognizer (RecognizerIntent): it owns the microphone, so the app needs no
 * RECORD_AUDIO permission. Visibility of the recognizer requires the manifest's <queries> entry.
 */
@Composable
actual fun rememberSpeechInput(language: String, prompt: String, onResult: (String) -> Unit): SpeechInput? {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(currentOnResult)
        }
    }
    val available = remember(context) {
        context.packageManager.queryIntentActivities(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0).isNotEmpty()
    }
    return remember(launcher, language, prompt, available) {
        if (!available) {
            null
        } else {
            object : SpeechInput {
                override fun start() {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                        .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
                    try {
                        launcher.launch(intent)
                    } catch (e: ActivityNotFoundException) {
                        // The recognizer went away since the availability check: nothing to dictate with.
                    }
                }
            }
        }
    }
}
