package com.foodtracker.app.ui

import com.foodtracker.app.data.ApiException
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.error_unauthorized
import food_tracker_frontend.composeapp.generated.resources.unknown_error
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * A localized "Couldn't …: <detail>" message. The detail is the backend's (English) error text as
 * received, except a rejected access token, which gets the app's own explanation of where to fix it.
 */
suspend fun errorText(message: StringResource, error: Exception): String {
    val detail = when {
        error is ApiException && error.status == HTTP_UNAUTHORIZED -> getString(Res.string.error_unauthorized)
        else -> error.message ?: getString(Res.string.unknown_error)
    }
    return getString(message, detail)
}

private const val HTTP_UNAUTHORIZED = 401
