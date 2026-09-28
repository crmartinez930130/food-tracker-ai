package com.foodtracker.app.data

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android

actual fun platformHttpClientEngine(): HttpClientEngine = Android.create()
