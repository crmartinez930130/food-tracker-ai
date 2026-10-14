package com.foodtracker.app.ui

import android.content.ContentResolver
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.foodtracker.app.ui.components.animationsDisabled

private fun readAnimationsDisabled(resolver: ContentResolver): Boolean =
    animationsDisabled(Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f))

/** Read on entry and on every resume, since the option is changed in system settings. */
@Composable
actual fun systemAnimationsDisabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    var disabled by remember { mutableStateOf(readAnimationsDisabled(resolver)) }
    LifecycleResumeEffect(Unit) {
        disabled = readAnimationsDisabled(resolver)
        onPauseOrDispose {}
    }
    return disabled
}
