package com.foodtracker.app.ui

import androidx.compose.runtime.Composable

/** Whether this is a development (debuggable) build - gates tools like the design sample screen. */
@Composable
expect fun isDebugBuild(): Boolean
