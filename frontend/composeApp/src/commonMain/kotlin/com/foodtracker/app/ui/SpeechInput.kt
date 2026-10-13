package com.foodtracker.app.ui

import androidx.compose.runtime.Composable

/** Starts the platform's speech recognizer; the recognized text arrives in the callback it was created with. */
interface SpeechInput {
    fun start()
}

/**
 * Speech-to-text in [language] (e.g. "es"), showing [prompt] while listening. Returns null when the
 * device has no speech recognizer, so the UI can hide the microphone.
 */
@Composable
expect fun rememberSpeechInput(language: String, prompt: String, onResult: (String) -> Unit): SpeechInput?
