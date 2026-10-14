## 1. Backups

- [x] 1.1 Add `data_extraction_rules.xml` and `backup_rules.xml`, excluding every domain, and the three manifest attributes (D1). Verify that `./gradlew :composeApp:assembleDebug` passes (aapt validates the XML) and that on the emulator `dumpsys package` no longer shows `ALLOW_BACKUP`, `bmgr` reports the app as not eligible for backup, and the app still works.

## 2. Warning and docs

- [x] 2.1 Add `isUnencrypted` with tests, the server card's note and the string in both languages (D2). Add the README "Security" section (D3). Verify with `./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug`.

## 3. Verification

- [x] 3.1 On the emulator, in light and dark and in Spanish and English:
  - the note shows for `http://` and updates while typing;
  - it disappears for `https://`;
  - saving still works.

  Also confirm the backup flags (1.1). Restore the settings afterwards.
