## 1. Pure helpers

- [x] 1.1 Add `periodLabel` (D2), `periodStats` (D3), `barIndexAt` (D4), `smoothControlPoints` (D5) and `connectedCorners` (D1), each with tests:
  - the label's same month, two months, another year and crossing years;
  - the stats' averages with empty days, days over the goal, and an empty list;
  - the bar index at the edges, the middle and out of range;
  - the curve passing through the points, the segment count, and a flat series;
  - the corners for first, middle, last, single and selected buttons.

## 2. Resources and state

- [x] 2.1 Add the `month_short_names` arrays, the stat and label strings and the `download` icon. Remove `summary_period_totals`. Extend `StringResourcesTest`'s array size check to `month_short_names` (12). Move `SummaryPeriod` to `ui/summary/` and add `AppState.summaryView` (D7). Verify with `StringResourcesTest` and `AppStateTest`.

## 3. Screen

- [x] 3.1 Add `ConnectedButtonGroup` (D1) and use it for week/month. Add the period label. Replace the averages with the stat cards (D3).
- [x] 3.2 Restyle `CalorieChart`: a card, rounded bars, the goal pill, and tap to open a day through `onOpenDay` from `MainScreen` (D4).
- [x] 3.3 Turn `WeightSection` into the weight card, with a big figure, a change pill, a smoothed and filled chart, short dates and icon delete (D5). Restyle `ExportSection` (D6). Verify with `./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug`.

## 4. Verification

- [x] 4.1 On the emulator, in light and dark and in Spanish and English, with meals and weights over two weeks:
  - the stat cards match the data;
  - over-goal bars are red and counted;
  - week/month switching and previous/next work, with the label updating;
  - tapping a bar opens that day, and back returns to the same period and view;
  - logging and deleting weight work, and the smoothed chart passes through the points;
  - export opens the picker and shares a CSV;
  - the period with no meals shows zeros.

  Clean the test data afterwards.
