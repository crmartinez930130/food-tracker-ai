## 1. Preferences and locale plumbing

- [x] 1.1 Add `ui/settings/AppPreferences.kt`:
  - the `LanguagePreference`/`ThemePreference`/`AppLanguage` enums;
  - `AppPreferences` with JSON encode and a decode that falls back to the defaults;
  - `resolveLanguage(pref, deviceLanguage)` and `resolveDarkTheme(pref, systemDark)`.

  Verify with `AppPreferencesTest`: defaults; round-trip; corrupt and partial input; system resolving `en` → en, and `es`/`fr`/`pt` → es; fixed choices ignore the device; the theme matrix.
- [x] 1.2 Add the `expect`/`actual` `LocalAppLocale` and `deviceLanguage()`, plus the common `AppEnvironment(tag)` (JetBrains pattern), with the Android actual and iOS stubs. Also add the Android `PreferencesStore` (`SharedPreferences`). Then wire them in:
  - `MainActivity` holds the preferences state and drives `enableEdgeToEdge` from the resolved theme;
  - `App(preferences, onPreferencesChange)` keeps `AppState` outside the locale key and applies `FoodTrackerTheme(darkTheme)`.

  Verify with `./gradlew :composeApp:assembleDebug`.

## 2. String resources

- [x] 2.1 Create `composeResources/values/strings.xml` (Spanish) and `values-en/strings.xml` (English) with every UI string, including weekday initials, meal types, notification texts and the channel name. Verify with `StringResourcesTest`, which parses both XML files and asserts the same keys and the same format placeholders per key.
- [x] 2.2 Change the pure helpers to typed results: `dayLabel` → `DayLabel`, `xAxisLabel` → `DayOfWeek?`, `exportRangeError` → `ExportRangeProblem?`, and `reminderTitle` → `StringResource`. Update `ProgressTest`, `ChartLayoutTest`, `ExportRangeTest` and `ReminderSettingsTest` to assert the typed values.
- [x] 2.3 Replace every literal in the commonMain UI with `stringResource`/`pluralStringResource`. That covers Home, WaterCard, Capture, Review, Goals, Summary (charts, weight, export), Reminders, MainScreen and the meal type labels. Verify with `./gradlew :composeApp:testDebugUnitTest assembleDebug` and a grep showing no `Text("`/`contentDescription = "` literals remain in commonMain UI.

## 3. Options screen and notifications

- [x] 3.1 Build `SettingsScreen`: language and theme radio groups, a Reminders row, a back arrow and `BackHandler`. Add `Screen.Settings`, then make Reminders' back return to Settings. Put a gear icon (Material Symbols `settings`) in Home's header and remove the bell. Verify with `AppStateTest` (Settings ↔ Reminders ↔ Home navigation, plus the reminder-tap guard still covering Settings) and `assembleDebug`.
- [x] 3.2 `ReminderReceiver`/`ReminderNotifications`: resolve the app language from the stored preferences and `deviceLanguage()`, call `Locale.setDefault`, and build the title, text and channel name with `getString`. Verify with `assembleDebug` and the emulator check in 4.1.

## 4. End-to-end verification

- [x] 4.1 On the emulator against the real backend:
  - **First run** (cleared app data): with the phone in English the app is in English; in Spanish it is in Spanish; in French it is in Spanish.
  - **Fixed language:** choosing Español with the phone in English switches everything instantly, and it persists after a force-stop and after changing the phone's language.
  - **Walk-through:** every screen in both languages, checked against the UI dump for leftover English or Spanish.
  - **Theme:** Dark on a light phone and Light on a dark phone, checking the system bar icons; System follows the phone.
  - **Navigation:** changing the language or the theme in Options keeps the user on Options.
  - **Notification:** a reminder notification in Spanish while the phone is in English.

  Clean up: reset the preferences to the defaults, the phone language back to English, reminders off, and the clock automatic.
