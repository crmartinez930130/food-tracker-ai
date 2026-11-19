package com.foodtracker.app

/**
 * The Android version code of a MAJOR.MINOR.PATCH version: MAJOR * 10000 + MINOR * 100 + PATCH, so a later
 * version always has a higher code. The Gradle build computes the apps' code the same way (root
 * build.gradle.kts); a test checks that the generated [AppVersion.CODE] agrees. Null for anything else.
 */
fun versionCodeOf(version: String): Int? {
    val parts = Regex("""^(\d+)\.(\d+)\.(\d+)$""").matchEntire(version)?.groupValues?.drop(1)?.map { it.toIntOrNull() ?: return null }
        ?: return null
    val (major, minor, patch) = parts
    if (minor > 99 || patch > 99) return null
    return major * 10000 + minor * 100 + patch
}
