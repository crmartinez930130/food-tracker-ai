rootProject.name = "food-tracker-frontend"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":composeApp")
// The app's logic without UI (API client, local cache, offline queue, day calculations), shared with
// other apps such as a future Wear OS one.
include(":shared")
