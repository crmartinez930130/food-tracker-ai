package com.foodtracker.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

/** How a screen section should present itself while (re)loading. */
enum class LoadingPresentation {
    /** Loading with nothing to show yet: a spinner. */
    INITIAL,

    /** Reloading over previously shown data (e.g. another day or period): dimmed content + progress bar. */
    STALE,

    /** Data is current: shown normally. */
    READY,
}

fun loadingPresentation(isLoading: Boolean, hasContent: Boolean): LoadingPresentation = when {
    !isLoading -> LoadingPresentation.READY
    hasContent -> LoadingPresentation.STALE
    else -> LoadingPresentation.INITIAL
}

/**
 * Shows [content] according to [presentation]: a spinner instead of it when INITIAL, and dimmed
 * under a thin progress bar when STALE - so data from the previous day/period is never shown
 * unmarked, without blanking the screen (and flickering) on every navigation.
 */
@Composable
fun LoadingAware(
    presentation: LoadingPresentation,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when (presentation) {
        LoadingPresentation.INITIAL -> Box(modifier.padding(16.dp)) { CircularProgressIndicator() }
        LoadingPresentation.STALE -> Column(modifier) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
            Box(Modifier.alpha(0.4f)) { content() }
        }
        LoadingPresentation.READY -> Box(modifier) { content() }
    }
}
