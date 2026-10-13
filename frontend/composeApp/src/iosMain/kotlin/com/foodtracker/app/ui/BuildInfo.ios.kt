package com.foodtracker.app.ui

import androidx.compose.runtime.Composable

// No debug tools on iOS for now (secondary target).
@Composable
actual fun isDebugBuild(): Boolean = false
