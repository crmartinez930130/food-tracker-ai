# Tasks

## 1. Color schemes

- [x] 1.1 Generate the light and dark `SchemeTonalSpot` schemes from seed `#4CAF50` with a throwaway `@material/material-color-utilities` script in the session scratchpad (not added to the repo); verify the output includes every Compose `ColorScheme` role, including the `surfaceContainer*` roles
- [x] 1.2 Add `frontend/composeApp/src/commonMain/kotlin/com/foodtracker/app/ui/theme/Color.kt` with `LightColors` / `DarkColors` built from those values (with a comment recording seed, variant and generator); verify `:composeApp:compileDebugKotlinAndroid` compiles
- [x] 1.3 Add `commonTest` contrast tests (`ui/theme/ColorContrastTest.kt`) that compute WCAG ratios for the role pairs listed in design.md Decision 7 in both schemes and require ≥ 4.5:1; verify `:composeApp:testDebugUnitTest` passes

## 2. Theme wiring

- [x] 2.1 Add `FoodTrackerTheme` (`ui/theme/Theme.kt`) choosing the scheme from `isSystemInDarkTheme()` and wrapping content in a `Surface(color = background)`; use it in `App.kt` instead of the bare `MaterialTheme`; verify it compiles
- [x] 2.2 Add `windowInsetsPadding(WindowInsets.safeDrawing)` to `CaptureScreen` and `ReviewScreen`; verify on the emulator that their content no longer sits under the status bar

## 3. Android integration

- [x] 3.1 Add `res/values/themes.xml` and `res/values-night/themes.xml` defining `Theme.FoodTracker` (light/dark `Material.NoActionBar` parents, `windowBackground` = each scheme's `background`), point the manifest's application and activity at it, and add `android:configChanges="uiMode"` to `MainActivity`; verify `:composeApp:assembleDebug` builds
- [x] 3.2 In `MainActivity`, re-apply `enableEdgeToEdge` with `SystemBarStyle.auto(...)` from a `DisposableEffect(darkTheme)` inside `setContent`; verify on the emulator that status/navigation bar icons are dark in light mode and light in dark mode

## 4. Icons

- [x] 4.1 Add the Material Symbols Outlined 24px vector drawables `list_alt`, `bar_chart`, `flag` and `add` to `commonMain/composeResources/drawable/` (from google/material-design-icons, Apache 2.0); verify the generated `Res.drawable.*` accessors compile
- [x] 4.2 Replace the emoji icons in `MainScreen`'s `NavigationBar` and the "+" text in the FAB with `Icon(painterResource(...))`, keeping each tab's label and giving the FAB a `contentDescription`; verify it compiles and the tests pass

## 5. End-to-end verification

- [x] 5.1 On the emulator, in light and dark mode, screenshot Capture, Review (one real photo analysis), History, Summary and Goals; check that every text and icon is legible and that nothing renders in default purple. Clean up any meal saved during the check
- [x] 5.2 On the emulator, switch the system mode (`adb shell cmd uimode night yes|no`) while on a tab and while on an unsaved Review screen; verify the theme changes immediately, the tab stays selected and the review keeps its analysis and edits. Launch the app cold in dark mode and verify there is no white flash (screenshot right after launch)
