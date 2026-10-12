package com.foodtracker.app.ui

import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.unknown_error
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * A localized "Couldn't …: <detail>" message. The detail is the backend's (English) error text as
 * received; only the app's own wording is translated.
 */
suspend fun errorText(message: StringResource, error: Exception): String =
    getString(message, error.message ?: getString(Res.string.unknown_error))
