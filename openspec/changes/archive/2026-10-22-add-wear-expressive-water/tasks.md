## 1. Logic

- [x] 1.1 Add `logWatchGlass` and `WatchOutboxSync`, with its worker and `watchSyncResult` (D3), and `isNewTileClick` (D5). Test the two pure functions.

## 2. Look

- [x] 2.1 Add `WatchColors` with the Compose and tile color schemes (D1). Use it in `MaterialTheme` and in `materialScope` (`allowDynamicTheme = false`).
- [x] 2.2 Rebuild the "Today" screen (D2):
  - header and cards with `SurfaceTransformation` and `transformedHeight`;
  - the calories ring behind the list;
  - the `EdgeButton` "+ 250 ml", with a haptic (D4).
- [x] 2.3 Add the tile's edge button with `loadAction`, and handle `lastClickableId` (D5). Add the strings in Spanish and English.

## 3. Verification

- [x] 3.1 Run `./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest :wearApp:testDebugUnitTest :composeApp:assembleDebug :wearApp:assembleDebug`.
- [x] 3.2 On the Wear OS emulator, with a demo backend:
  - the cards transform while scrolling and the ring shows;
  - "+ 250 ml" in the app and in the tile, online, adds exactly one glass, which the backend has;
  - offline, the glass is pending and sent once;
  - the brand colors.

  Clear the emulator's app data afterwards.
- [x] 3.3 On the Galaxy Watch6 and the phone, with the user's OK to install: "+ 250 ml" from the app and the tile shows on the phone.
