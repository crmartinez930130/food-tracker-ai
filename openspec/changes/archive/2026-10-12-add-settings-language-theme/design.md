## Context

- **UI text:** about 115 user-visible strings are literals in commonMain. Several pure, unit-tested helpers return English text: `dayLabel` ("Today"/"Yesterday"), `xAxisLabel` ("Mo"…"Su"), `exportRangeError` and `reminderTitle`. Number and unit helpers (`waterLabel`, `formatKg`) are language-neutral.
- **Compose Multiplatform 1.9.3 resources:**
  - In composables, the resource environment comes from `LocalConfiguration`.
  - Outside composables, on Android, `getString` builds it from `Locale.getDefault()`, as `getSystemEnvironment` does.
  - JetBrains documents a runtime locale override: an `expect object LocalAppLocale` whose Android actual calls `Locale.setDefault`, updates the configuration and provides `LocalConfiguration`, wrapped in `key(locale)`. We follow it.
- **Theme:** `MainActivity` computes `isSystemInDarkTheme()` for `enableEdgeToEdge` and `FoodTrackerTheme`, and the manifest declares `configChanges="uiMode"`.
- **Other local preferences:** reminder settings already live in `SharedPreferences` as JSON, with a corrupt-safe decode.

## Goals / Non-Goals

**Goals:**
- One place decides the language, and every string, both in Compose and in the notification receiver, follows it.
- Switching language or theme keeps the user on the same screen (Options), with navigation state intact.

**Non-Goals:**
- More languages.
- Translating backend error details, the CSV headers or the food names returned by photo analysis.
- Localized number formatting. Decimals keep a `.`, the weight input already accepts `,`, and units (kcal, g, ml, kg) are the same in both languages.
- Changing the pre-Compose launch window to a forced theme, which would need AppCompat night mode.
- iOS.

## Decisions

### D1. Spanish is the base resource set
- `values/strings.xml` holds Spanish and `values-en/strings.xml` holds English.
- The resolved language is always `es` or `en` (D2), so the fallback set only matters for missing keys.
- A unit test asserts that both files define exactly the same keys and format arguments.
- **Alternative:** English as the base. Rejected: Spanish is the product default.

### D2. Preferences and resolution are pure common code
- **The model:** `AppPreferences(language: LanguagePreference = SYSTEM, theme: ThemePreference = SYSTEM)`, serialized as JSON. Missing or corrupt values decode to the defaults, as `ReminderSettings` does.
- **`resolveLanguage(pref, deviceLanguage): AppLanguage`:**
  - `SPANISH` → es, `ENGLISH` → en;
  - `SYSTEM` → en if the device language is `en`, and es otherwise, including any third language.
- **`resolveDarkTheme(pref, systemDark): Boolean`.**
- **`deviceLanguage()`** is an `expect`. The Android actual reads `Resources.getSystem().configuration.locales[0].language`, which our own `Locale.setDefault` override never changes. So "Phone language" keeps tracking the phone.

### D3. The platform entry point owns the preference state
- **`MainActivity`:**
  - loads `AppPreferences` from `SharedPreferences` into a `mutableStateOf`;
  - resolves the dark theme for `enableEdgeToEdge`;
  - passes `preferences` and `onPreferencesChange` (save, then update the state) into `App`.
- **`App`:**
  - keeps `AppState` *outside* the locale `key`, so switching language recomposes the screens but keeps the user on Options;
  - wraps the rest in `AppEnvironment(languageTag)` with `FoodTrackerTheme(darkTheme)`.
- **iOS:** `MainViewController` keeps the preferences in memory (stub).

### D4. Runtime locale override follows the JetBrains pattern
- `expect object LocalAppLocale { val current; infix fun provides(tag: String) }`.
- **Android actual:** `Locale.setDefault(Locale(tag))`, `configuration.setLocale`, `resources.updateConfiguration`, then provides `LocalConfiguration`.
- `AppEnvironment` applies it with `key(tag) { content() }`.
- **Side effects of `Locale.setDefault`:** Material's date and time pickers, which read `LocalConfiguration` and the default locale, show Spanish month and day names. Non-composable `getString` calls in this process also resolve Spanish.

### D5. Typed results instead of English from pure helpers
- `dayLabel` → `DayLabel` (`Today`, `Yesterday`, `Date(iso)`).
- `xAxisLabel` → `DayOfWeek?`.
- `exportRangeError` → `ExportRangeProblem?` (`Incomplete`, `Reversed`, `TooLong(days)`).
- `reminderTitle(meal)` → `StringResource`.
- Composables map these through `stringResource`, so the existing tests keep testing logic, not wording.
- The chart's weekday initials are resources (`Lu Ma Mi Ju Vi Sá Do` / `Mo Tu We Th Fr Sa Su`).

### D6. Notifications resolve the language themselves
- **In `ReminderReceiver`** (no Compose, possibly a fresh process):
  - load `AppPreferences`;
  - resolve the language with `deviceLanguage()`;
  - `Locale.setDefault` to it;
  - build the title, text and channel name with `getString(Res.string.…)`. The receiver already runs in a coroutine for the meals check.
- **Channel name:** `ensureChannel` re-applies it, so the name follows the latest language.

### D7. Options screen and navigation
- **Entry:** `Screen.Settings`, opened from a gear (Material Symbols `settings`) in Home's header. The bell is removed.
- **Layout:** two sections with radio-button groups ("Language", "Theme"), plus a "Reminders" row that opens `Screen.Reminders`.
- **Back from Reminders** returns to Settings, and back from Settings returns to Home.
- **Choices** apply on tap. There is no Save button.

## Risks / Trade-offs

- **`Locale.setDefault` is process-global.** Any default-locale formatting changes with it. → The app formats numbers with Kotlin's locale-independent `toString`/`roundToInt`, and dates with kotlinx-datetime ISO strings. This is covered by the walk-through in both languages.
- **`updateConfiguration` is deprecated.** → It is the documented Compose Multiplatform approach for this version. It is isolated in one actual, so a future migration to per-app locales (`LocaleManager` on API 33+) only touches that file.
- **A forced theme flashes the system theme at launch** (for example Dark on a light phone) before the first frame. → This is accepted; the existing launch requirement still holds for the device mode.
- **The extraction misses a string.** → The key-parity test catches mismatches between the two files. A manual walk-through of every screen in both languages catches leftover literals, plus a grep for `Text("` with a literal in commonMain.

## Migration Plan

None. The new preferences default to Phone language and System theme. For an English phone that is the current behavior, and a Spanish phone switches to Spanish.
