## Why

Two minor points from the security review, both more relevant now that #60 stores the backend's access token on the phone:
- **Backups:** `android:allowBackup` isn't set, so it defaults to true. The app's preferences (language, theme, server address and, since #60, the **access token**) and the reminder settings go into Android's cloud backup and device-to-device transfer. The token shouldn't leave the phone.
- **Cleartext:** `usesCleartextTraffic="true"` is needed to reach the backend over `http://` on the home network, so meals, weight and water travel unencrypted over the Wi-Fi. That's fine at home, but it isn't documented or signaled anywhere.

Tracked as GitHub issue #61: https://github.com/crmartinez930130/food-tracker-ai/issues/61

## What Changes

- **No backups:**
  - `android:allowBackup="false"` for Android ≤ 11;
  - `android:dataExtractionRules`, which excludes every data domain from both cloud backup and device-to-device transfer, for Android 12+, where `allowBackup="false"` no longer stops device transfers;
  - `android:fullBackupContent`, which excludes everything for older key/value-less full backups.

  On a new phone the user sets the app up again (server, token, reminders).
- **Options → Server:** when the address uses `http://`, a short note under the field says the connection isn't encrypted and is meant for trusted networks only.
- **README "Security" section:**
  - the backend is meant for the home network, protected by the access token (#60), without HTTPS;
  - never expose it to the internet or forward the port on the router;
  - on public networks the data travels unencrypted;
  - away from home, use a VPN such as Tailscale.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `app-settings`: the settings stay on the device (excluded from backups and transfers), and the server setting warns about unencrypted addresses.

## Impact

- **Android:**
  - `AndroidManifest.xml`;
  - new `res/xml/data_extraction_rules.xml` and `res/xml/backup_rules.xml`.
- **Common:**
  - a pure `isUnencrypted(url)` helper next to `normalizeServerUrl`, with tests;
  - the warning in `SettingsScreen`'s server card;
  - one string in both languages.
- **Docs:** README.
- **Backend:** no change.
