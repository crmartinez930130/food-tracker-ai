## Context

- **Storage:** `MainActivity`, `PreferencesStore` and `ReminderStore` keep everything in two SharedPreferences files: `app_preferences.xml` (one JSON with language, theme, server URL and access token) and `meal_reminders.xml`. There's no database or file storage on the phone, other than the export cache.
- **The manifest:** it sets `usesCleartextTraffic="true"` and no backup attributes. `targetSdk` is 36 and `minSdk` 26.
- **Android backup behavior:**
  - on Android 12+ (API 31+), with a targetSdk of 31 or higher, `allowBackup="false"` disables cloud backup but **not** device-to-device transfer, which is governed by `dataExtractionRules`;
  - on API 30 and lower, `allowBackup` and `fullBackupContent` govern.

## Goals / Non-Goals

**Goals:**
- None of the app's data leaves the phone through backup or transfer, on any supported Android version.
- The user is told when the server connection is unencrypted.
- The deployment's limits are documented.

**Non-Goals:**
- HTTPS on the backend.
- Encrypting the preferences at rest (EncryptedSharedPreferences is deprecated, and the sandbox already protects them).
- Restricting cleartext to private IP ranges: Android's network security config matches domains, not IP ranges, and the address is user-set.

## Decisions

### D1. Exclude everything, everywhere
- **Manifest attributes:**
  - `android:allowBackup="false"`;
  - `android:dataExtractionRules="@xml/data_extraction_rules"`;
  - `android:fullBackupContent="@xml/backup_rules"`.
- **`data_extraction_rules.xml`:** `<cloud-backup>` and `<device-transfer>`, each with `<exclude domain="root|file|database|sharedpref|external"/>` entries, so nothing is included.
- **`backup_rules.xml`:** the same exclusions for API ≤ 30.
- **Why everything and not only the token:** the token lives in the same JSON as the other preferences. Splitting it into its own file would change storage for a marginal benefit, since re-picking the language and theme on a new phone is trivial, and the issue asks to keep the preferences and reminders out.
- **Alternative:** moving the token to its own file and excluding only that. Rejected for the reason above.

### D2. Unencrypted warning
- **The helper:** `isUnencrypted(url)` is true when the address (trimmed, case-insensitive) starts with `http://`. It is pure and tested: `http`, `HTTP`, `https` and an invalid text.
- **Where:** the server card shows it as the address field's `supportingText`, with an `onSurfaceVariant` small text: "Conexión sin cifrar: usala solo en redes de confianza." / "Unencrypted connection: use it only on trusted networks." It follows the typed text live.
- **Not an error:** HTTP is the expected setup at home. The note informs and doesn't block.

### D3. README "Security" section
- **Where:** after "Reaching the backend from your phone".
- **What it covers:**
  - what is protected: the access token, the analysis rate limit and the upload checks from #60;
  - what isn't: HTTP traffic and a single shared token;
  - the do's and don'ts: never forward port 3000 on the router; use a VPN such as Tailscale away from home; rotate the token by changing it in both `.env` and the app;
  - a pointer to `SECURITY.md` once #62 adds it.

## Risks / Trade-offs

- **A phone migration loses the app's settings.** → They are few and quick to re-enter, and the data itself lives on the backend.
- **The warning may feel noisy at home.** → It's one small line, not an error state.

## Migration Plan

None. Existing installs keep their preferences: the change only affects future backups and transfers.
