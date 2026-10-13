## Why

This is the second of the redesign issues inspired by the Material 3 Expressive samples at https://m3e.c5inco.dev/ (Greenwave palette, already the app's theme since #42). Home is the main screen. Today it shows a calorie ring, three macro bars and a plain list of meals, and it lets the user step through past days with arrows. With the redesign it shows **only today**: past days move to their own History screen (#48). Tracked as GitHub issue #43: https://github.com/crmartinez930130/food-tracker-ai/issues/43

## What Changes

- **Header:**
  - a greeting that depends on the time of day ("Buenos días", "Buenas tardes", "Buenas noches");
  - today's date, written out ("lunes, 13 de octubre"; "Monday, October 13");
  - actions for Options and the goal.
- **Today only:**
  - the previous-day and next-day arrows are removed;
  - Home always shows the current calendar day;
  - if the app stays open past midnight, or comes back to the foreground on a new day, Home switches to the new day by itself.
- **Calories:**
  - the ring is replaced by a big figure ("1240 kcal") with a **shape badge** next to it showing the percentage of the goal;
  - tapping the badge opens the goal, as tapping the ring did;
  - a line under the figure keeps "of N kcal" and "N left" or "N over", with the over state in the error color.
- **Macros:** a **segmented bar** (protein, fat, carbohydrates) with a dot legend. Each entry shows the grams consumed against that macro's target. An entry over its target is shown in the error color.
- **Water:** the same data and actions (add a glass, undo) on a tinted card in the new style.
- **Meals:**
  - grouped by type, each group on a card headed by the type's icon in a small colored square and the group's calories;
  - the icons are breakfast, lunch, dinner, snack and other;
  - favorite and delete stay on each meal, with delete becoming an icon button.
- The default-goal hint stays.
- The day's content (calories, macros, water, meals) becomes one reusable piece that takes the day as a parameter, so #48's day detail can show any past day with the same content and actions.

Until #48 lands, past days are not reachable from the app. Their data stays intact in the backend, and Summary keeps showing them in its charts.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `daily-dashboard`: Home shows only today, with a greeting header, a big calorie figure with a goal-percentage badge, a segmented macro bar and meals grouped on cards with type icons. It follows the day change at midnight, and day navigation is removed.
- `water-tracking`: water on Home is always today's; logging into a past day from the app moves to the day detail (#48).
- `nutrition-goals`: the goal is opened from Home's header or the percentage badge, and going back returns to today's Home.

## Impact

- **Frontend common:**
  - `ui/home/HomeScreen.kt` is rewritten as a header plus a reusable day-content composable;
  - `WaterCard.kt` is restyled;
  - new pure helpers for the greeting, the written-out date, the goal percentage and the time until the next day, with tests;
  - `AppState`, `MainScreen` and `App` drop `homeDate` and the date callbacks;
  - the `dayLabel` and `DayLabel` helpers and their tests are removed.
- **Resources:**
  - Material Symbols icons for the meal types and delete (Apache-2.0, same source as the existing icons);
  - new strings and string arrays (weekday and month names) in Spanish and English;
  - the unused day-navigation strings are removed.
- **Tests:** `StringResourcesTest` also checks string arrays. `ColorContrastTest` adds the container pairs the new cards use.
- **Backend:** no change.
