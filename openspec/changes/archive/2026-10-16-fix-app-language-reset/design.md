# Design

## Context

- **`AppEnvironment(language)`** provides `LocalAppLocale provides language.tag` and keys the content by language. The Android `provides`:
  - calls `Locale.setDefault`;
  - mutates `LocalConfiguration` with `setLocale`;
  - calls the deprecated `Resources.updateConfiguration`.

  It only runs again when it is recomposed.
- **Compose resources' `stringResource`** resolves with Compose's `Locale.current`, which follows the process default locale. `getString` outside composition (notifications) uses `Locale.getDefault()`, set by `applyAppLocale`.
- **Language resolution:** `resolveLanguage(preference, deviceLanguage())` in `App`. `deviceLanguage()` reads `Resources.getSystem().configuration.locales[0]`.
- **Manifest:** `MainActivity` has `configChanges="uiMode"`; any other change recreates it.
- **What breaks:** on Android 17 the emulator shows a configuration re-application when the camera closes, with no callback. It sets both the activity's resources and `Locale.getDefault()` back to the phone's locale.

## Goals / Non-Goals

**Goals:**
- The chosen language holds through the camera and any configuration Android re-applies, on Android 13 and later.
- Same behavior as today for switching in Options (immediate, stays on Options), "Phone language", and notifications.

**Non-Goals:**
- Android 8–12: the bug may persist there; see the alternative below.
- Showing the app in Android's "App languages" settings (`localeConfig`).
- iOS.

## Decisions

1. **Per-app language on API 33+.** In `LocalAppLocale.provides`, when the device is on API 33 or later and `LocaleManager.applicationLocales` differs from the resolved tag, call `setApplicationLocales(LocaleList.forLanguageTags(tag))`.
   - The stored preference stays the source of truth, and the per-app language mirrors the resolved language (always explicit, also with "Phone language", since French must give Spanish).
   - The existing `Locale.setDefault`/`updateConfiguration` override stays for the first frame and for API < 33.
   - Android keeps the per-app language across restarts and updates. `applyAppLocale` still sets the default for notifications.
2. **No recreation when it changes.** Add `locale|layoutDirection` to `MainActivity`'s `configChanges`. Setting the per-app language then only updates `LocalConfiguration`, and the `key(language)` already re-resolves every string, so Options stays on screen.
3. **The phone's language.** On API 33+, `deviceLanguage()` reads `LocaleManager.systemLocales[0]`, which ignores the per-app language. `Resources.getSystem()` reflects it, as the emulator showed (`sys=[es,en_US]`).
   - With an app language set, Android doesn't notify the app when the phone's language changes. The common `deviceLanguage()` becomes `@Composable`: on Android it re-reads the phone's language in a `LifecycleResumeEffect`, and on iOS it is unchanged. Changing the phone's language means leaving the app, so it applies on return.
   - Notifications (`applyAppLocale`) use a non-composable `deviceLanguage(context)` with the same rule.
   - The pure resolution (`resolveLanguage`) is unchanged.

## Alternative considered

**AppCompat's `AppCompatDelegate.setApplicationLocales`.** It covers API < 33 too, with a backport. It needs:
- the `androidx.appcompat` dependency;
- `MainActivity` extending `AppCompatActivity`;
- an AppCompat theme;
- and, on API < 33, it still relies on overriding the activity's configuration, so it isn't clear it survives this reset there.

It's left as a follow-up if the bug shows up on an older phone.

## Risks / Trade-offs

- **API < 33 untested and unfixed.** Stated in the proposal; the code path there is unchanged.
- **Handling `locale` in `configChanges`:** a system language change no longer recreates the activity. D3's re-read on resume covers it.
