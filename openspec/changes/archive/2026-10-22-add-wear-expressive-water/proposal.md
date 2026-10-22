## Why

The watch app (#104) uses Compose for Wear OS Material 3 and `protolayout-material3`, but takes little from Material 3 Expressive, the design of Wear OS 6:
- **The "Today" screen:** its blocks are plain columns, with none of the list motion that traces the round display.
- **Colors:** the app uses the default theme and the tile the watch's dynamic colors, not Food Tracker's green.
- **Water:** a glass can't be logged from the watch.

Tracked as GitHub issue #106: https://github.com/crmartinez930130/food-tracker-ai/issues/106

## What Changes

- **"Today" screen, Expressive:**
  - calories, macros and water in cards that shrink and curve at the edges while scrolling (`SurfaceTransformation` + `transformedHeight`);
  - a calories progress ring around the screen, as on the tile.
- **"+ 250 ml" on the app,** as an `EdgeButton` hugging the bottom edge:
  - logs one glass of the size set in Goals;
  - goes through the offline queue as on the phone: sent at once, or kept on the watch and sent once when the backend answers;
  - shows at once on the screen and the tile.
- **"+ glass" on the tile,** an edge button that logs the glass without opening the app (`loadAction` with a clickable id), and redraws the tile.
- **Food Tracker's green** (the app's dark scheme) on the app and the tile, instead of the watch's dynamic colors.
- Texts in Spanish and English.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `wear-app`:
  - water can be logged from the watch app and its tile;
  - the watch app and tile use Food Tracker's colors;
  - the "Today" screen follows Material 3 Expressive list motion.

## Impact

- **`frontend/wearApp`:**
  - the screen rebuilt with cards, the ring and the `EdgeButton`;
  - the tile with an edge button and its click handling;
  - glass logging through the watch's own offline queue, with a WorkManager sender;
  - a Wear color scheme (Compose and tiles) from the app's dark palette;
  - strings.
- **`:shared` and `composeApp`:** no change. The watch reuses `storedSettingsOutbox`, `OutboxRepository` and `todayWidgetStates`.
- **Backend:** no change.
