## Why

The app still runs on Compose's default `MaterialTheme`: the generic purple palette, no dark scheme of its own, and a light-only Android window theme. Dark mode is expected in every app we compared (MyFitnessPal, Cronometer, Lifesum, Cal AI). The emoji tab icons (📋 📊 🎯) also look out of place next to Material components. This change is the visual base the next UI issues build on, starting with the home redesign in #3. Tracked as GitHub issue #1: https://github.com/crmartinez930130/food-tracker-ai/issues/1

## What Changes

- Add an app theme with its own Material 3 color schemes, light and dark, generated from a single brand seed color. It replaces the default `MaterialTheme` in `App.kt`.
- The app follows the system light/dark setting, and switching it while the app is open updates the UI in place, without restarting the screen or losing in-progress work such as an analysis under review.
- Make the Android system bars and the window background follow the active mode: no white flash on launch in dark mode, and readable status/navigation bar icons in both modes.
- Replace the emoji icons in the bottom navigation (History, Summary, Goals) and the "+" text on the capture button with Material Symbols icons.
- Verify contrast and legibility in both modes on all five existing screens: Capture, Review, History, Summary and Goals.

## Capabilities

### New Capabilities
- `app-appearance`: the app's visual theme: following the system light/dark mode live, keeping on-screen state across the switch, and keeping system bars legible in both modes.

### Modified Capabilities
<!-- None: no backend behavior or existing requirement changes. -->

## Impact

- **Frontend only.**
  - New theme files under `frontend/composeApp/src/commonMain/kotlin/com/foodtracker/app/ui/theme/`.
  - Material Symbols vector drawables under `composeResources/drawable/`.
  - Changes to `App.kt`, `MainScreen.kt`, `MainActivity.kt`, `AndroidManifest.xml`, and new `res/values{,-night}/themes.xml`.
- **No new Gradle dependencies.** The icons ship as vector resources through the existing `compose.components.resources` dependency.
- **Backend/API:** none.
- **Unblocks:** #3 (home redesign), which will use these color roles for the progress ring.
