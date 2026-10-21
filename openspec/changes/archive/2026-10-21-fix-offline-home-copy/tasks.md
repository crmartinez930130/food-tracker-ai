## 1. Fix

- [x] 1.1 Add `placeholderIfStillMissing` with tests (D2):
  - a copy that arrives while the placeholder is built wins;
  - with no copy, the placeholder is used;
  - with the copy already shown, there's no placeholder.

  Use it in `DayContent`'s offline branch (D1).

## 2. Verification

- [x] 2.1 Run `./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest :composeApp:assembleDebug`.
- [x] 2.2 On the emulator, with a demo backend:
  - open with a connection;
  - turn on airplane mode;
  - force-stop and reopen several times: Home shows the copy with "No connection · data from HH:mm" every time;
  - a day never loaded still shows the empty day with the queued entries;
  - the day detail behaves the same.

  Restore the emulator afterwards.
