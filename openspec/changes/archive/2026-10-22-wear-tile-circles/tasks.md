## 1. Tile

- [x] 1.1 Add `ic_tile_water` and register it as a tile resource (version "2") (D2).
- [x] 1.2 Rebuild the tile layout by hand (D1, D3):
  - the title and a pill panel with two circles: a green or red calories ring, and the water ring that logs a glass (`loadAction`/`glassClickId`);
  - the stale note under it;
  - the open-app click on the panel.

## 2. Leftovers

- [x] 2.1 Remove the dead import in `WatchWater.kt`. Make the `EdgeButton` fall back to `DEFAULT_GOAL.waterGlassMl` (D4).

## 3. Verification

- [x] 3.1 Run `./gradlew :wearApp:testDebugUnitTest :wearApp:assembleDebug` and the full CI command.
- [x] 3.2 On the Galaxy Watch6, with the user's OK to install:
  - the two circles read at a glance;
  - the calories ring turns red over the goal;
  - a tap on the water circle adds one glass and the phone shows it;
  - a tap elsewhere on the panel opens the app.
