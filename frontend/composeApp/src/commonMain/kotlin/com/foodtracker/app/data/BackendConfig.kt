package com.foodtracker.app.data

/**
 * Per-platform default backend URL. Android emulators reach the host machine's
 * localhost via the special 10.0.2.2 alias; a real phone on the home network needs
 * the backend machine's actual LAN IP, set at runtime via [BackendConfig.baseUrl].
 */
expect fun defaultBackendBaseUrl(): String

object BackendConfig {
    var baseUrl: String = defaultBackendBaseUrl()
}
