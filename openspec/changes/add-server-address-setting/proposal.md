## Why

On Android the app always calls `http://10.0.2.2:3000`, the emulator's alias for the host machine. On a real phone such as the S24, that address goes nowhere and nothing lets the user change it. The backend already listens on `0.0.0.0`, so the phone only needs to be told the PC's LAN address. Tracked as GitHub issue #36: https://github.com/crmartinez930130/food-tracker-ai/issues/36

## What Changes

- **Options gains a "Server" section:**
  - It shows the current backend address, which can be edited (for example `http://192.168.1.50:3000`) and saved.
  - **Validation before saving:**
    - accepts `http` or `https`, a host, an optional port from 1 to 65535, and no path;
    - a trailing `/` is dropped;
    - an invalid entry shows a message and is not saved.
  - **"Test connection"** calls the backend with a short timeout and reports "Connected" or the error, in the app's language.
  - **"Restore default"** goes back to the built-in address (`http://10.0.2.2:3000` on Android).
- **Persistence:** the address is saved with the other Options preferences and applies immediately, without a restart.
- **Every backend call uses the saved address.** That includes the reminder receiver's meal check, which runs outside the app.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `app-settings`: Options gains the server address, with its validation, connection test, restore-default and persistence.

## Impact

- **Frontend common:**
  - `AppPreferences` gains `serverUrl: String?`, where null means the platform default.
  - New pure `normalizeServerUrl(input): String?` and `effectiveServerUrl(preferences)`.
  - `SettingsScreen` gains the Server section.
  - New strings in both languages.
- **Frontend Android:**
  - `MainActivity` applies the effective address to `BackendConfig.baseUrl` on start and on every change.
  - `ReminderReceiver` builds its client with the stored address.
- **Backend:** no change.
- **Dependencies:** none.
