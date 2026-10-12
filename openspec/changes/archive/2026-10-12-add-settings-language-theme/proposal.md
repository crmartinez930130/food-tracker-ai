## Why

Every UI text is hard-coded in English, and the theme always follows the system. The app should speak Spanish and English, start in the phone's language, and let the user pick the language and the theme on a new options screen. Tracked as GitHub issue #34: https://github.com/crmartinez930130/food-tracker-ai/issues/34

## What Changes

- **New Options screen**, opened from a gear icon in Home's header.
  - **Language:** "Phone language" (default), "Español" or "English".
    - "Phone language" uses Spanish or English as the phone says, and **Spanish** for any other phone language.
    - A fixed choice sticks even if the phone's language changes.
  - **Theme:** "System" (default), "Light" or "Dark".
  - **Reminders moves inside Options**, and the bell leaves Home's header, which keeps the gear and the edit-goal pencil.
  - Changes apply immediately, without restarting and without losing the current screen. They persist on the device.
- **The whole app is translated:**
  - Every UI text moves to Compose Multiplatform string resources, with Spanish as the base (`values/`) and English in `values-en/`. This covers screens, dialogs, the app's own error messages, chart labels and accessibility descriptions.
  - Reminder notifications and their channel name use the chosen language.
  - "Today"/"Yesterday", the chart's weekday initials and the date and time pickers follow the chosen language.
- **Theme preference:** `FoodTrackerTheme` and the system bar icon colors follow the preference instead of always using the system mode.

## Capabilities

### New Capabilities
- `app-settings`: the Options screen, the language and theme preferences and their persistence, and the requirement that every UI text exists in Spanish and English and follows the chosen language.

### Modified Capabilities
- `app-appearance`: the app follows the user's theme preference, and follows the system mode only when that preference is "System".
- `meal-reminders`: the reminders screen is reached from Options instead of Home's header, and notifications use the app's chosen language.

## Impact

- **Frontend common:**
  - `composeResources/values/strings.xml` (Spanish) and `values-en/strings.xml` (English).
  - Every screen switches to `stringResource`.
  - Pure helpers that returned English text now return typed values that the UI maps to resources: `dayLabel`, `xAxisLabel`, `exportRangeError` and `reminderTitle`.
  - New `ui/settings/` with `AppPreferences`, language and theme resolution, and `SettingsScreen`.
  - `LocalAppLocale` `expect`, following the JetBrains resource-environment pattern.
  - `Screen.Settings`; Reminders opens from Settings, and its back returns there.
- **Frontend Android:**
  - `LocalAppLocale` actual.
  - `deviceLanguage()`.
  - Preferences in `SharedPreferences`.
  - `MainActivity` owns the preference state and applies the theme to the edge-to-edge bars.
  - The reminder receiver resolves the app language before building the notification with `getString`.
- **Frontend iOS:** stubs.
- **Backend:** no change.
- **Dependencies:** none new.
