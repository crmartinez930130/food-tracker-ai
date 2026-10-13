## Why

This is the third redesign issue inspired by the Material 3 Expressive samples (https://m3e.c5inco.dev/). Summary shows the week's or month's trends with the pre-redesign layout: plain text averages, a basic bar chart, a weight section with a straight-line chart and text buttons, and ISO dates. The reference is the stat cards and bar charts of Dollop and Heliotrope. Tapping a bar should also open that day's detail, the screen added by #48. Tracked as GitHub issue #44: https://github.com/crmartinez930130/food-tracker-ai/issues/44

## What Changes

- **Week/Month as a connected button group:** two joined pill buttons, with the selected one fully rounded and filled.
- **Readable period label:** "7 – 13 oct" instead of "2026-10-07 – 2026-10-13", with the year only when it isn't the current one or the period crosses years.
- **Stat cards:**
  - the daily calorie average;
  - the days over the goal ("1 / 7");
  - the macro averages as a segmented bar;
  - the period's total kcal on a line below.
- **Calorie chart:**
  - on a card, with bars rounded at the top;
  - a dashed goal line, keyed by a legend above the bars;
  - days over the goal in the error color, as today.
- **Tap a bar** to open that day's detail (#48). Back returns to Summary on the same period and view, which now survive leaving the screen.
- **Weight card:**
  - the latest weight as a big figure, and the period's change;
  - a smoothed line chart with a soft fill under it;
  - the entries with short dates and an icon delete;
  - logging works as today.
- **Export CSV:** restyled as a tonal button with an icon, with the same flow.

No backend change, no new metrics.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `nutrition-summary`: the Summary screen shows stat cards and a readable period label, and tapping a chart bar opens that day's detail.
- `meal-history`: the day detail can also be opened from Summary, and going back returns to where it was opened.
- `weight-tracking`: the weight section shows the latest weight as a big figure and the trend as a smoothed line.

## Impact

- **Frontend common:**
  - `SummaryScreen` is reorganized into cards;
  - `CalorieChart` gets rounded bars, the goal legend and tap handling;
  - `WeightSection` and `WeightChart` become a card with a smoothed line;
  - `ExportSection` is restyled;
  - a new `ConnectedButtonGroup` in `ui/components/`;
  - new pure helpers with tests: the period label, the bar index at a tap position, the connected button shapes and the smooth-curve control points.
- **Navigation:** `SummaryPeriod` and the period's end date move into `AppState`, so returning from a day keeps them. `MainScreen` passes `onOpenDay` to Summary.
- **Resources:**
  - short month names (`month_short_names`) and the new stat and label strings, in both languages;
  - Material Symbols `download` for export and `check` for the selected period button;
  - `summary_period_totals` is replaced by the stat cards' strings.
- **Backend:** no change.
