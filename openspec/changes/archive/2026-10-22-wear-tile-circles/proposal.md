## Why

The "Today" tile (#104, #106) crowds a ring, the number, "left/over" and the water into one circle in small type, and it doesn't flag going over the goal: the ring doesn't turn red. Samsung's own tiles, such as "Salud", read at a glance: a title and a rounded panel with circles, each one figure. The user chose that style.

Tracked as GitHub issue #108: https://github.com/crmartinez930130/food-tracker-ai/issues/108

## What Changes

- **The "Today" tile:** the title and a rounded pill-shaped panel with two circles, after the user's review on the watch (four were tried first; the user kept calories and water):
  - **Calories:** a progress ring, green within the goal and red once over, with the number inside and "kcal" below.
  - **Water:** a light blue ring with the ml and a "+ (drop)" hint. Tapping it logs the configured glass, exactly once per tap (#106).
- **A tap elsewhere on the panel** opens the app.
- **Old or missing data:** a short line says so.
- **Two leftovers from the #107 review:**
  - a dead import in `WatchWater.kt`;
  - the app's `EdgeButton` falls back to `DEFAULT_GOAL.waterGlassMl` instead of a hard-coded 250.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `wear-app`: the "Today" tile's content and layout (the four circles, the red ring over the goal).

## Impact

- **`frontend/wearApp`:**
  - `TodayTileService` layout;
  - a water-drop drawable registered as a tile image resource;
  - strings;
  - the two small fixes.
- **Others:** no change to `:shared`, the phone app or the backend.
