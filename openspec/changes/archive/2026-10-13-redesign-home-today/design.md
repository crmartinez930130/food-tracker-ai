## Context

- **Home today:**
  - `HomeScreen` receives a `date` and an `onDateChange`, and `AppState.homeDate` keeps that date across leaving Main.
  - The header has previous and next arrows, a "Hoy"/"Ayer"/ISO label, and the Options and goal actions.
  - The body is one `LazyColumn` with:
    - a `CircularProgressIndicator` ring (clickable to the goal);
    - three `LinearProgressIndicator` macro bars;
    - the `WaterCard`;
    - the meals grouped by type under plain text headers.
- **Base from #42 (`ui/components/`):**
  - `BigNumber` (figure plus unit, baseline-aligned);
  - `ShapeBadge` (text in a `RoundedPolygon`, `cookie9` by default);
  - `SegmentedBar` (proportional segments with a dot legend; `BarSegment(label, value, color, detail)`);
  - `StatCard`;
  - `ExpressiveLoadingIndicator`, already used by `LoadingAware`.
- **History (#48)** will need the same day content (calories, macros, water and meals, with favorite, delete and water log or undo) for any past day.
- **Android timing:** the main-thread coroutine `delay` is based on uptime, which stops while the device is in deep sleep. A long delay started in the background can fire late, so the day change can't rely on a single delay alone.

## Goals / Non-Goals

**Goals:**
- Home shows today, and only today, in the Greenwave expressive style, with every current action kept.
- The day's content is one piece that takes the day as a parameter, ready for #48.
- The new pure logic (greeting, percentage, time to the next minute) is tested.

**Non-Goals:**
- The History screen, the bottom navigation change and reaching past days (#48).
- The Summary redesign (#44).
- Animations such as count-up or morphing the badge (#47).
- Backend changes.
- Thousands separators. The app shows plain figures everywhere ("1240"), and Spanish typography doesn't separate four-digit numbers.

## Decisions

### D1. A minute clock drives "today"
- **`rememberCurrentMinute()`** returns the local date-time as Compose state:
  - a `LaunchedEffect` loop waits `millisUntilNextMinute(now)`, then re-reads the clock;
  - a `LifecycleResumeEffect` re-reads it on resume, which covers the uptime-based delay drifting while the app was in the background.
- **Home derives from it:**
  - `today` (date string) and the greeting hour come from the clock;
  - the day's data loads keyed on `today`, so the midnight tick reloads it, dimmed while loading by the existing `LoadingAware` (STALE);
  - recomposing the header once a minute is negligible.
- **Pure helper:** `millisUntilNextMinute(now: LocalDateTime)` returns 1..60000 ms and is tested.
- **Alternatives:**
  - A single delay until midnight. Rejected: deep-sleep drift, and the greeting would not follow the hour.
  - An `ACTION_DATE_CHANGED` broadcast receiver. Rejected: platform-specific for no gain.

### D2. Greeting by time of day
- **Hour ranges:** `greetingFor(hour)` maps 5–11 to morning, 12–19 to afternoon and 20–4 to night, as an enum.
- **Strings:** "Buenos días", "Buenas tardes", "Buenas noches" and "Good morning", "Good afternoon", "Good evening".
- **Tests:** the boundaries 4/5, 11/12, 19/20 and 23/0.

### D3. Written-out date from string arrays
- **Name arrays:** `weekday_names` (Monday first, matching ISO day numbers) and `month_names` as `<string-array>` in both languages.
- **Template:** `home_date` is "%1$s, %2$d de %3$s" in Spanish and "%1$s, %3$s %2$d" in English, so each language orders the parts its own way.
- **Capitalization:** the first letter is capitalized for display, since Spanish names are lowercase ("Lunes, 13 de octubre").
- **Tests:** `StringResourcesTest` also checks string arrays, with the same names in both languages and the same item count (7 and 12).
- **Alternative:** platform date formatting through expect/actual. Rejected: more code than two arrays, and the in-app language override would need care on each platform.

### D4. Calories: big figure plus percentage badge
- **The row:** `BigNumber("1240", "kcal")`, with a `ShapeBadge` at 80 dp, `cookie9`, showing `goalPercent(consumed, target)`.
- **Percentage:** rounded and not clamped, so 112% shows as such. It is `null` when the target is not positive, and the badge then shows "–".
- **Badge colors:** `primaryContainer` normally, `errorContainer` when over the goal.
- **Badge action:** it is clickable with the "edit daily goal" click label, replacing the ring's role.
- **The line below:** "de N kcal · quedan M" (or "M de más" in `error`) keeps the existing strings.

### D5. Macros: segmented bar, legend against targets
- **Segment widths:** proportional to the **grams consumed** of each macro (protein `primary`, fat `tertiary`, carbs `secondary`). They show the day's composition in the same unit as the legend. With nothing eaten the bar is an empty track (`segmentFractions` already returns zeros).
- **Legend:** each entry's detail is "consumed / target g". It turns `error` when over target, which keeps the spec's "over target is visually distinguished", including the zero-target case through `progressOf`.
- **Component change:** `BarSegment` gains an optional `detailColor`.
- **Alternative:** segments sized by calories (4/9/4 kcal per gram). Rejected: the bar and legend would use different units.

### D6. Cards and meal-type icons
- **Summary card:** calories, the default-goal hint and macros share one card on `surfaceContainer`.
- **Water card:** `WaterCard` becomes a card on `tertiaryContainer` with:
  - a `BigNumber` total over the target ("750 / 2000 ml");
  - the `tertiary` progress bar;
  - the same undo and add-glass buttons.

  Going over the water target still never turns red.
- **Meal groups:**
  - one card per meal type, on `surfaceContainerLow`;
  - the header has a 40 dp rounded square with the type's icon, then the type label and the group's kcal;
  - the meals inside are separated by dividers.
- **Type icons** (Material Symbols Outlined, Apache-2.0, same source as the existing icons):

  | Type | Icon | Square color |
  |---|---|---|
  | Breakfast | `bakery_dining` | `tertiaryContainer` |
  | Lunch | `lunch_dining` | `primaryContainer` |
  | Dinner | `dinner_dining` | `secondaryContainer` |
  | Snack | `cookie` | `surfaceContainerHighest` |
  | Other | `restaurant` | `surfaceContainerHighest` |

  The scheme has only three accent containers, so snack and other share the neutral one; their icons tell them apart.
- **Delete** becomes an icon button (`delete` icon, with the existing "Eliminar" / "Delete" string as its content description).
- **Contrast:** `ColorContrastTest` adds the new pairs:
  - `onTertiaryContainer/tertiaryContainer` and `onTertiary/tertiary` (the water card and its button);
  - `onErrorContainer/errorContainer` (the over-goal badge);
  - `primary/surfaceContainer` and `error/surfaceContainer` (the "left" and "over" text);
  - `onSurface/surfaceContainerLow`;
  - `onSurfaceVariant/surfaceContainerLow`;
  - `onSurfaceVariant/surfaceContainerHighest`.

### D7. Day content as a reusable composable
- **`DayContent(apiClient, date, today, onEditGoal)`** in `ui/home/` holds all the loading, state and actions that live in `HomeScreen` today:
  - meals, goal and water loading;
  - favorite and delete;
  - water log and undo under the mutex, with `loggedAtFor(date, today)`;
  - the cards from D4 to D6.
- **`HomeScreen`** becomes the header plus `DayContent(date = today, today = today)`.
- **For #48:** the day detail will wrap the same composable with a past date.
- **Stale water:** the current-date guard on water refreshes stays, so a refresh that finishes after the midnight switch is not shown on the new day.

### D8. Navigation state cleanup
- **`AppState`:**
  - `homeDate` is removed;
  - `openHomeAfterSave()` becomes `openMain(HOME)`, because Home is always today;
  - `MainScreen` and `App` drop the date parameters.
- **Removed as unused:** `dayLabel`/`DayLabel` and their tests, and the `day_today`, `day_yesterday`, `previous_day` and `next_day` strings. #48 can bring back what its list needs.
- **Kept:** `plusDays` and `isBeforeDay`, which Summary still uses.

## Risks / Trade-offs

- **Past days are unreachable between this PR and #48.** → The data is intact in the backend and Summary still charts it. The two issues are planned back to back.
- **A once-a-minute recomposition of Home's header.** → Only the header reads the minute. The day's content is keyed on the date string and stays put.
- **"112%" in the cookie.** → At 80 dp, three digits plus "%" fit on the emulator; 72 dp was too tight.

## Migration Plan

None. This is a frontend-only change. Reverting the commit restores day navigation on Home.
