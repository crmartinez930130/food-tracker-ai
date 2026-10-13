## Context

- **Home since #43:**
  - shows only today;
  - its content lives in `DayContent(apiClient, date, today, onEditGoal)`, which already supports past days (water logged on a past day goes to local noon through `loggedAtFor`).
- **Backend:**
  - `GET /api/summary/range?startDate&endDate` returns one zero-filled entry per day with the calorie and macro totals;
  - `dailyTotalsBetween` aggregates meals per local day in SQL;
  - water has only a per-day endpoint (`GET /api/water?date`).
- **Navigation:**
  - `AppState.screen` is a `Screen` sealed interface;
  - `Main` holds the tab, but its selected tab is local to `MainScreen` and resets when Main is left;
  - Goals always returns to `Main(HOME)`.
- **Components from #42 and #43:**
  - `SegmentedBar` (with `showLegend = false` for a mini bar);
  - `ShapeBadge`;
  - the `weekday_names` and `month_names` arrays;
  - the `weekday_short_*` strings (used by the Summary chart).

## Goals / Non-Goals

**Goals:**
- See a month's days at a glance against the calorie goal, and open any past day with Home's full content and actions.
- One request per month for the calendar and the list.
- Pure, tested logic for the month grid, the day status and the navigation return targets.

**Non-Goals:**
- Tapping Summary's bars (#44). The detail screen is built so #44 only has to call it.
- Logging meals on a past date. Meals are still logged at the current time.
- Searching meals.
- A per-day goal history. Every day is judged against the current goal, since the app keeps only one goal.
- Animations (#47).

## Decisions

### D1. Extend the range summary instead of adding an endpoint
- **What changes:** each `GET /api/summary/range` entry gains:
  - `mealCount`: the number of meals that local day;
  - `waterMl`: the water total that local day.
- **How:**
  - `dailyTotalsBetween` adds `count(*)`;
  - a new `waterTotalsBetween(db, start, endExclusive)` sums `water_logs` per `date(logged_at, 'localtime')`, the same day rule as everywhere else;
  - `rangeSummary` merges both into the zero-filled days.
- **Compatibility:** it is additive. The app's JSON parser ignores unknown keys, so Summary keeps working unchanged.
- **Alternative:** a new `/api/history` endpoint. Rejected: it would duplicate the range logic, its validation and its tests for two extra fields.

### D2. Day status against the calorie goal
- **The rule:** `dayStatus(day, targetCalories)` returns:
  - `NONE` when `mealCount == 0`. Water alone doesn't color the calendar, but it does list the day;
  - `UNDER` below 90% of the goal;
  - `ON_TARGET` from 90% to 100% inclusive;
  - `OVER` above 100%.
- **The goal:** a non-positive target makes every day with meals `OVER`, matching `progressOf`.
- **Why these bounds:**
  - "over" is the same rule as Home's red badge and Summary's highlighted bars, so the three screens agree;
  - the 90% floor keeps a nearly-complete day from reading as "under".
- **Colors:**

  | Status | Container | Text |
  |---|---|---|
  | `NONE` | none | `onSurfaceVariant` |
  | `UNDER` | `secondaryContainer` | `onSecondaryContainer` |
  | `ON_TARGET` | `primary` | `onPrimary` |
  | `OVER` | `errorContainer` | `onErrorContainer` |

  A legend under the calendar names the four.
- **Tests:** each boundary (89.9%, 90%, 100%, 100.1%), no meals with water, and a zero target.

### D3. Month grid as a pure function
- **`monthGrid(year, month)`** returns the weeks of the month as lists of 7 `LocalDate?`. Monday comes first, matching ISO days and the existing `weekday_short_*` labels, and `null` pads before the 1st and after the last day.
- **Tests:**
  - a month starting on a Monday;
  - one starting on a Sunday, which needs 6 rows;
  - February in a leap year and in a common year.
- **Month navigation:**
  - `YearMonth` arithmetic through `LocalDate.plus(DatePeriod(months = ±1))` on the 1st;
  - the next arrow is disabled when the shown month is the current one;
  - the previous arrow has no limit.

### D4. History screen layout
- **The screen:** a `LazyColumn` with:
  - the month header: the month name, capitalized, and the year, between the arrows;
  - the weekday initials;
  - the calendar grid, as rows of 7 equal-weight circular cells, each with the day number on its status color;
  - the legend;
  - the day cards.
- **Today** gets a 2 dp `primary` outline.
- **Future days** are drawn at 38% alpha and aren't clickable.
- **Day cards:**
  - only days with `mealCount > 0` or `waterMl > 0`, newest first;
  - each is a `surfaceContainerLow` card with:
    - a status dot;
    - the date ("Hoy", "Ayer", or "Lunes 12" from `weekday_names`);
    - "1705 / 2000 kcal";
    - a mini `SegmentedBar` of the macro grams (no legend);
    - the water as text ("500 ml de agua").
- **Empty month:** the calendar plus a "No registraste nada este mes" message.
- **Loading:** `LoadingAware` with the existing STALE dimming when the month changes, and one load per shown month: `rangeSummary(first, last)` plus `getGoals()`. A future month can't be shown, but the current month's days after today come back zero-filled and are drawn as future.

### D5. Day detail screen
- **`DayDetailScreen(apiClient, date, onBack, onEditGoal)`:**
  - a `TopAppBar` with a back arrow and the long date title ("Lunes, 12 de octubre"), built by the `longDate` helper that moves from `HomeScreen` to a shared file;
  - `DayContent(apiClient, date, today, onEditGoal)` below it;
  - a `BackHandler` for system back.
- **Today:** the detail of today is allowed, with no special case. It shows the same content as Home.

### D6. Navigation state and return targets
- **`AppState` changes:**
  - `MainTab` gains `HISTORY`, placed between `HOME` and `SUMMARY`;
  - `Screen.DayDetail(date: String, returnTo: Screen)`, with `openDayDetail(date)` recording the current screen. From History that is `Main(HISTORY)`; #44 will open it from `Main(SUMMARY)`;
  - `historyMonth: String` (the first day of the shown month, as an ISO date) lives in `AppState`, so returning from the detail shows the same month. It resets to the current month only on a fresh `AppState`.
- **The selected tab** is kept in `Screen.Main(tab)`. `MainScreen` reports tab changes back (`onTabChange`), so `openDayDetail` records the right tab.
- **Goals:** `openGoals()` stores the current screen, and Goals' back calls `closeGoals()`, which returns to it: Home, or the day detail. Settings and Reminders keep their current behavior.
- **Reminders:** `openCaptureFromReminder` also treats `DayDetail` as a screen it can leave for Capture.
- **Tests:** `AppStateTest` covers:
  - History → detail → back returns to `Main(HISTORY)`;
  - detail → goals → back returns to the same detail;
  - Home → goals → back returns to `Main(HOME)`;
  - a reminder from the detail opens Capture.

### D7. Resources
- **Icon:** Material Symbols `calendar_month` (Outlined, Apache-2.0), cleaned like the other icons.
- **New strings, both languages:**
  - `nav_history`;
  - the legend's four labels;
  - `history_month_title` ("Octubre de 2026" / "October 2026") and `history_day_label` ("Lunes 12");
  - `history_empty_month`;
  - `history_day_water` ("%1$d ml de agua");
  - `history_day_kcal` ("%1$d / %2$d kcal");
  - `error_load_history`;
  - `day_calories_title` ("Calorías del día"), so a past day's detail doesn't say "Calorías de hoy";
  - `day_today` and `day_yesterday`, back for the list.
- **Shared date labels:** `longDate`, `monthTitle` and `weekdayName` move to `ui/DateLabels.kt`, used by Home, History and the detail.
- **Reused strings:**
  - `summary_previous_month` and `summary_next_month` as the arrows' descriptions;
  - `action_back`.

## Risks / Trade-offs

- **Every day is judged against today's goal.** → Changing the goal recolors past days. It's documented as a non-goal; a goal history would need backend storage.
- **No lower bound on month navigation.** → Months before the first record just show empty. It's harmless, and cheaper than an extra request for the first date.
- **The calendar uses calories only.** → Macros and water are on each day card and in the detail.

## Migration Plan

None. The range response change is additive, and older app builds ignore the new fields.
