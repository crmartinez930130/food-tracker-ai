## 1. App

- [x] 1.1 `LocalAppLocale.provides` sets Android's per-app language on API 33+ when it differs from the resolved tag; `MainActivity` handles `locale|layoutDirection` (D1, D2). Verify with the build and on the emulator.
- [x] 1.2 `deviceLanguage()` reads `LocaleManager.systemLocales` on API 33+, becomes `@Composable` and re-reads it on resume; notifications use `deviceLanguage(context)` (D3). Verify with the existing `resolveLanguage` tests and on the emulator.

## 2. Verification

- [x] 2.1 On the emulator (Android 17), with the phone in English:
  - with the app in Spanish, open the in-app camera, cancel it and go back to Home, 4 times; then take a photo and check that the analysis request carries `language=es`;
  - the same with the phone in Spanish and the app in English;
  - switch the language in Options: it applies at once and Options stays on screen;
  - with "Phone language", change the phone's language and return: the app follows;
  - a reminder comes out in the app's language, with the app's process started by the alarm. The "meals ready to review" notification uses the same path (`applyAppLocale`) and wasn't triggered separately.

  Restore the app's settings, its per-app language and the phone's language afterwards.
