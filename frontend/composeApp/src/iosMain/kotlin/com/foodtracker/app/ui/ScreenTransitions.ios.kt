package com.foodtracker.app.ui

import androidx.compose.runtime.Composable

// iOS is a secondary target; its reduce-motion setting isn't wired yet.
@Composable
actual fun systemAnimationsDisabled(): Boolean = false
