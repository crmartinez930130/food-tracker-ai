## 1. Model and validation

- [x] 1.1 Add `serverUrl: String? = null` to `AppPreferences`, plus the pure `normalizeServerUrl(input)` and `effectiveServerUrl(prefs)` helpers. Verify with `AppPreferencesTest`/`ServerUrlTest`:
  - canonical forms: trimming, scheme case, the trailing slash;
  - a valid IPv4 address with a port, `localhost`, a hostname, and `https`;
  - rejection of: no scheme, an unsupported scheme, an empty host, a port of 0 or above 65535 or non-numeric, a path, a query, spaces;
  - old JSON without the field decodes to null;
  - the effective URL falls back to the default.

## 2. Applying the address

- [x] 2.1 `MainActivity` sets `BackendConfig.baseUrl` from the preferences before `setContent` and on every preferences change. `ReminderReceiver` builds its client with `effectiveServerUrl(PreferencesStore.load(context))`. Verify with `./gradlew :composeApp:assembleDebug` and on the device (4.1).

## 3. Options UI

- [x] 3.1 Add the Server section to `SettingsScreen`: a field with the effective URL, Save (validate and store), "Test connection" (validate, then `getGoals` on a client bound to the typed URL, with a 5 s timeout and a localized result) and "Restore default". Add the strings in both languages. Verify with `StringResourcesTest`, `./gradlew :composeApp:testDebugUnitTest assembleDebug`, and on the device (4.1).

## 4. Verification on the S24

- [x] 4.1 On the real phone (S24) on the same Wi-Fi as the PC, against the real backend:
  - The default address fails "Test connection" with a readable error.
  - An invalid entry is rejected.
  - Entering the PC's LAN IP passes "Test connection". Saving makes Home load, water logging work and a photo analysis succeed.
  - The address persists after a force-stop.
  - "Restore default" brings back `10.0.2.2`.

  Clean up any test data created.
