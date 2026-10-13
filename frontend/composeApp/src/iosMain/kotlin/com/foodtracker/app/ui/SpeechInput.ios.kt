package com.foodtracker.app.ui

import androidx.compose.runtime.Composable

// iOS dictation (SFSpeechRecognizer) is deferred like the other iOS actuals: no microphone shown.
@Composable
actual fun rememberSpeechInput(language: String, prompt: String, onResult: (String) -> Unit): SpeechInput? = null
