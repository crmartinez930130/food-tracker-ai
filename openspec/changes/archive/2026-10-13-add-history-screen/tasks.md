## 1. Backend

- [x] 1.1 Add `mealCount` to `dailyTotalsBetween`. Add `waterTotalsBetween` to the water repository. Merge both in `rangeSummary`, as zero-filled `mealCount` and `waterMl` (D1). Verify with backend tests:
  - the repository: count and water per local day, including a water-only day;
  - the service: zero-filled days carry `mealCount: 0` and `waterMl: 0`;
  - the route: the new fields are in the response;
  - `npm run typecheck` and `npm test`.

## 2. Pure frontend logic

- [x] 2.1 Add `mealCount` and `waterMl` to `DailyTotals`. Add `ui/history/HistoryLogic.kt`:
  - `dayStatus` (D2);
  - `monthGrid`, plus the month arithmetic helpers (D3);
  - the newest-first "days with records" filter.

  Verify with `HistoryLogicTest`:
  - the status boundaries, water only, and a zero target;
  - grids for months starting on Monday and on Sunday, and February in leap and common years;
  - the previous and next month across a year boundary;
  - the filter's order.

## 3. Navigation

- [x] 3.1 Add to `AppState`:
  - `MainTab.HISTORY`;
  - `Screen.DayDetail(date, returnTo)` with `openDayDetail`/`closeDayDetail`;
  - `historyMonth`;
  - the return target for Goals (`closeGoals`);
  - `DayDetail` in `openCaptureFromReminder`.

  Make `MainScreen` report tab changes (D6). Verify with the `AppStateTest` cases in D6.

## 4. Screens and resources

- [x] 4.1 Add the `calendar_month` icon and the strings in both languages (D7). Move `longDate` to a shared place. Verify with `StringResourcesTest`.
- [x] 4.2 Build `HistoryScreen` (D4): the month header and arrows, the weekday initials, the calendar cells with status colors, today's outline and future dimming, the legend, the day cards and the empty-month message. Use `LoadingAware` when changing month.
- [x] 4.3 Build `DayDetailScreen` (D5) and wire it in `App`. Add the History tab to `MainScreen`. Verify with `./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug`.

## 5. Verification

- [x] 5.1 On the emulator, in light and dark and in Spanish and English, with test data on several days of this month and of last month (under, on target, over, water-only):
  - the calendar colors and legend match the data;
  - today is outlined and future days can't be tapped;
  - month navigation works and stops at the current month;
  - the list shows only days with records, newest first, with the right figures;
  - the calendar and the list both open the detail;
  - in the detail, favorite, delete, add a glass on a past day (counts toward that day) and undo work;
  - the goal from the detail returns to the detail;
  - back and system back return to History on the same month;
  - Summary still works.

  Clean the test data afterwards.
