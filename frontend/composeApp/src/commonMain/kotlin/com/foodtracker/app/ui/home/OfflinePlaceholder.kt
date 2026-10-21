package com.foodtracker.app.ui.home

import com.foodtracker.app.data.cache.CachedDay

/**
 * The empty day to show for a date that has no copy and can't be loaded (offline-cache), or null when a
 * copy of that date got shown while it was being built: [build] suspends, and the copy must win.
 */
suspend fun placeholderIfStillMissing(isShown: () -> Boolean, build: suspend () -> CachedDay): CachedDay? {
    if (isShown()) return null
    val placeholder = build()
    return placeholder.takeUnless { isShown() }
}
