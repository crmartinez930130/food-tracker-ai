# Tasks

## 1. Progress and date logic

- [x] 1.1 Add `ui/home/Progress.kt` with `progressOf(consumed, target)` (fraction clamped to 0..1, `isOver`, remaining/excess; target 0 → fraction 1 and over if consumed > 0, else 0) and `dayLabel(date, today)` ("Today" / "Yesterday" / ISO date); verify with `commonTest` `ProgressTest` covering under, exactly at, over, zero target with and without consumption, and the three label cases
- [x] 1.2 Add `isToday`/date helpers needed by the header (e.g. next-day disabled when `date == today`) to `util/Dates.kt`; verify with unit tests in the same test class or a `DatesTest`

## 2. Icons and navigation state

- [x] 2.1 Add Material Symbols Outlined 24px drawables `home`, `chevron_left`, `chevron_right`, `arrow_back`, `edit` to `composeResources/drawable/` (same source, license header and tint/fill adjustments as #1); verify the `Res.drawable.*` accessors compile
- [x] 2.2 Change `MainTab` to `HOME`/`SUMMARY`, add `Screen.Goals` with `openGoals()` to `AppState`, and move the Home date into `AppState.homeDate` (reset to today by `openMain` after a save); verify it compiles

## 3. Home screen

- [x] 3.1 Add `ui/home/HomeScreen.kt`: header (chevrons + `dayLabel` + edit-goal `IconButton`), calorie ring (`CircularProgressIndicator` with consumed/goal and left/over text, `error` color when over, clickable → Goals), three macro `LinearProgressIndicator` rows, default-goal `AssistChip`, and the day's meals grouped by type with delete (moved from `HistoryScreen`); loads `listMeals(date)` + `getGoals()`, totals summed from the meals; delete `HistoryScreen.kt`; verify it compiles
- [x] 3.2 Wire `MainScreen` to two tabs (Home with `home` icon, Summary) and `App` to the `Screen.Goals` route; verify `:composeApp:assembleDebug` and `:composeApp:testDebugUnitTest` pass

## 4. Goals and Summary screens

- [x] 4.1 Give `GoalsScreen` a `TopAppBar` with an `arrow_back` navigation icon and a `BackHandler`, both returning to Home; verify it compiles
- [x] 4.2 Make `SummaryScreen` weekly-only (remove the Daily/Weekly toggle and daily state, chevron `IconButton`s for the week); verify it compiles and the tests pass

## 5. End-to-end verification

- [x] 5.1 On the emulator against the real backend: Home on today with no meals (zero progress, empty message, default-goal chip); real capture → review → save → back on Home with the meal grouped under its type and the ring/bars updated; delete it → progress back to zero; go to yesterday (label "Yesterday"), confirm the next-day control is disabled on today
- [x] 5.2 On the emulator: open Goals from the header and from the ring; from a past day, save a lower goal so calories are over, go back (arrow and system back) → same day shown, ring in the over state; check Summary is weekly-only and the bottom bar has exactly Home and Summary; screenshot Home in light and dark. Clean up: delete test meals and restore the goal row to its pre-test state
