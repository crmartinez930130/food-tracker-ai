package com.foodtracker.app

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.foodtracker.app.data.cache.IosCapturePhotoStore
import com.foodtracker.app.data.cache.cacheDatabase
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App(database = remember { cacheDatabase() }, capturePhotos = remember { IosCapturePhotoStore() }, requestSync = {}) }
