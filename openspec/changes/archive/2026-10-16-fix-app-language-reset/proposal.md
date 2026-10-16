## Why

After the in-app camera is used, screens opened afterwards show the phone's language instead of the one chosen in Options, and the analysis can be requested in that language (`language=en` with the app in Spanish). It lasts until the app is restarted.

Cause, confirmed on the emulator (Android 17, phone in English, app in Spanish): when the camera closes, Android re-applies its configuration to the app's resources without calling `onConfigurationChanged` (neither the activity's nor the application's). That re-application puts the phone's locale back in the activity's resources and in the process default (`Locale.getDefault()`), and undoes what `LocalAppLocale.provides` set. Compose resources resolve `stringResource` with the process default, so screens composed from then on (Home, after closing Capture) come out in English. The screens already on display keep their language. With the language set as Android's per-app language (`cmd locale set-app-locales`), the same 4 rounds stayed in Spanish; without it, 3 of 4 switched to English.

Tracked as GitHub issue #76: https://github.com/crmartinez930130/food-tracker-ai/issues/76

## What Changes

- **Android 13 and later:** the app's language becomes Android's per-app language (`LocaleManager.setApplicationLocales`), set from the resolved language whenever it changes. Android itself then applies it to every configuration it re-applies, so nothing can reset it.
- **Changing the language in Options:** the activity handles `locale|layoutDirection` changes itself (manifest `configChanges`), so it isn't recreated and the user stays on Options, as today.
- **The phone's language:** with Android's per-app language set, `Resources.getSystem()` no longer reports the phone's language. "Phone language" reads `LocaleManager.getSystemLocales()` instead, and the app re-reads it whenever it comes back to the foreground, so changing the phone's language still applies.
- **Android 8 to 12:** unchanged. The current override stays as it is, so the bug may persist there. It couldn't be reproduced on those versions, because no emulator image for them is installed. The user accepted this scope on 2026-10-16, in PR #81 (#76's criterion now says Android 13+), and the older versions go to a follow-up issue.
- iOS is unchanged.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `app-settings`: the chosen language survives the camera and any other configuration Android re-applies.

## Impact

- **App (Android only):**
  - `AppLocale.android.kt` (sets the per-app language, `deviceLanguage()`);
  - `deviceLanguage()` becomes `@Composable` (re-read on resume), with `deviceLanguage(context)` for notifications;
  - `AndroidManifest.xml` (`configChanges`);
  - `PreferencesStore.applyAppLocale` (notifications) keeps working as is.
- No new dependency (`LocaleManager` is part of the platform from API 33).
- No backend change, so no redeploy.
