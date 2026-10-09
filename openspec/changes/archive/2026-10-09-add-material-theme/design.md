# Design

## Context

`App.kt` wraps everything in a bare `MaterialTheme { }`, which gives the default baseline palette and only a light scheme. Only the tabs in `MainScreen` sit inside a `Scaffold`. `CaptureScreen` and `ReviewScreen` are drawn directly by `App`, with no `Surface`. Their background is therefore the Android window background, and their default text color (`LocalContentColor`) isn't tied to the theme, so those two screens would show dark text on a dark background once a dark scheme exists.

The app targets SDK 36, so Android 15+ already forces edge-to-edge. The status bar icons come from the static `Theme.Material.Light.NoActionBar` window theme, which means dark icons in every mode. Changing the system mode recreates `MainActivity`, and `AppState` lives in a plain `remember`, so a recreation would drop an analysis that is under review.

Compose Multiplatform is at 1.9.3. Since 1.8 the Material icons artifacts are no longer pulled in transitively, and JetBrains recommends Material Symbols vector resources over the frozen `material-icons-*` 1.7.3 artifacts.

## Goals / Non-Goals

**Goals:**
- One theme entry point (`FoodTrackerTheme`) that all current and future screens inherit from.
- Live light/dark switching that keeps the on-screen state, on Android first. The common code also works on iOS.
- Contrast that is checked by a test, not only by eye.

**Non-Goals:**
- Custom typography or shapes. The Material 3 defaults (Roboto, baseline shapes) are kept.
- An in-app theme toggle (light/dark/system setting). The app follows the system only.
- Redesigning screens or replacing other ad-hoc widgets, such as the `[Breakfast]` text-button selector or the "<" / ">" date arrows. That belongs to #3 and later UI issues.
- Dynamic color (Material You wallpaper colors). See Decision 2.

## Decisions

**1. Schemes generated from one seed with Material's own algorithm, committed as Kotlin constants.**
The seed is a fresh green, `#4CAF50`, which fits the food and health domain and keeps clear of the error red. I generate every color role for light and dark with `@material/material-color-utilities`, using the `SchemeTonalSpot` variant that Material Theme Builder also uses by default. The npm package runs in a throwaway script in the scratchpad and is not added to the project. The output is pasted into `ui/theme/Color.kt` as `lightColorScheme(...)` and `darkColorScheme(...)`, with all roles filled, including the `surfaceContainer*` roles.
- *Alternative:* hand-pick a few colors and let Compose fill in the rest. The missing roles would fall back to the baseline purple, which defeats the purpose.
- *Alternative:* the Theme Builder website export. Same algorithm, but a manual step that can't be reproduced. The script, with its seed and variant, is recorded in a comment in `Color.kt`.

**2. Static brand schemes, without dynamic color.**
On Android 12+, Material You would replace the palette with the user's wallpaper colors. This is a nutrition app: upcoming features (#3's progress ring, #7's charts) will lean on stable meanings for colors, and it should look the same on every device. Dynamic color could be added later as an opt-in setting.
- *Alternative:* dynamic color when available, falling back to the brand palette. This is the Material default, but it makes the app's look depend on the wallpaper and removes its identity.

**3. A `Surface` at the root, plus safe-drawing insets on the screens outside the `Scaffold`.**
`FoodTrackerTheme` wraps the content in `Surface(color = colorScheme.background)`. Every screen then gets a themed background and the matching `onBackground` content color. `CaptureScreen` and `ReviewScreen` get `windowInsetsPadding(WindowInsets.safeDrawing)` so that, under the enforced edge-to-edge, they don't draw under the status bar.

**4. Live mode switching through `configChanges="uiMode"`, not activity recreation.**
`MainActivity` declares `android:configChanges="uiMode"`. Compose's `isSystemInDarkTheme()` reads the current `Configuration`, so it recomposes with the new scheme while `AppState` and the screen state stay in memory. This is the most direct way to meet "updates without restarting" and "keeps the analysis under review".
- *Alternative:* let the activity recreate and make `AppState` saveable. That needs a `Saver` for `MealAnalysis` and touches every screen's state. It's worth doing eventually for process death, but it is more than this issue needs.

**5. System bars styled from Compose, plus day/night window themes.**
- In `setContent`, a `DisposableEffect(darkTheme)` calls `enableEdgeToEdge(...)` with `SystemBarStyle.auto(...) { darkTheme }`. It is called again on every mode switch, because with `configChanges` the activity is no longer recreated and the bars wouldn't update otherwise.
- New `res/values/themes.xml` and `res/values-night/themes.xml` define `Theme.FoodTracker` on top of the light and dark platform `Material.NoActionBar` themes. Their `windowBackground` matches each scheme's `background` color, which avoids a white flash when launching in dark mode. The two background hex values are copied into XML. The cost is a small duplication, documented next to both definitions.

**6. Material Symbols as vector resources.**
I add the needed icons as Android-XML vector drawables in `commonMain/composeResources/drawable/` and load them with `painterResource(Res.drawable.…)`, following JetBrains' recommendation for CMP 1.8+. The icons are: history list (`list_alt`), summary (`bar_chart`), goals (`flag`) and capture (`add`). The 24px "Outlined" XML files come from Google's `material-design-icons` repository on GitHub, which is under the Apache 2.0 license.
- *Alternative:* `org.jetbrains.compose.material:material-icons-extended:1.7.3`. It is frozen at 1.7.3, built with K1, and pulls in thousands of icons to use four.

**7. Contrast is checked in a unit test.**
A `commonTest` test computes WCAG contrast ratios for the text/background role pairs the app uses in both schemes: `onSurface`/`surface`, `onSurfaceVariant`/`surface`, `onPrimary`/`primary`, `onPrimaryContainer`/`primaryContainer`, `onSecondaryContainer`/`secondaryContainer` (the selected nav indicator), `onSurfaceVariant`/`surfaceContainer` (unselected nav items), `primary`/`surface` (success text), `error`/`surface` and `onBackground`/`background`. It requires at least 4.5:1 for each pair. The emulator screenshots then cover the parts a ratio can't, such as components using unexpected roles.

## Risks / Trade-offs

- **[With `configChanges="uiMode"`, any uiMode-dependent Android resource won't reload]** → The UI is pure Compose and reads colors from `MaterialTheme`. The only uiMode-dependent resource is the window background, which is just the launch backdrop.
- **[Generated colors may look off in practice]** → They are reviewed in the emulator screenshots of the five screens in both modes. If the seed doesn't look right, it is changed and the schemes regenerated with the same recorded script.
- **[Icon XML comes from an external repo]** → The files are small, have a pinned source and license, and are reviewed before committing. No build-time download is involved.
