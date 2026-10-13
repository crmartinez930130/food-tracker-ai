## Context

- **`SummaryScreen` today:**
  - holds the period (`WEEK` 7 days, `MONTH` 30 days, a private enum) and `endDate` in `rememberSaveable`. Leaving Main disposes them, so opening a day and coming back would reset to this week;
  - loads `rangeSummary` plus `getGoals()`, and weight separately;
  - shows `SingleChoiceSegmentedButtonRow`, an ISO range, `CalorieChart`, plain-text averages, `WeightSection` (a straight-line `WeightChart`, text delete buttons) and `ExportSection` (an outlined button plus a date range picker).
- **Chart math** lives in `ChartLayout.kt` (`chartScaleMax`, `barFraction`, `isOverGoal`, `xAxisLabel`, `latestPerDay`, `weightScale`) with tests.
- **Components from #42 and #43:**
  - `StatCard`, `BigNumber`, `SegmentedBar`;
  - the `ic_delete` icon;
  - `DateLabels.kt` with the month and weekday names.
- **Navigation from #48:**
  - `AppState.openDayDetail(date)` records the current screen as the return target;
  - `MainScreen` reports tab changes, so from Summary the return target is `Main(SUMMARY)`.

## Goals / Non-Goals

**Goals:**
- Same data as today in the expressive style, with tap-to-open-day on the chart.
- Summary keeps its period and view when coming back from a day.
- New pure logic is tested.

**Non-Goals:**
- Backend changes or new metrics.
- Chart animations (#47).
- Changing the export flow or the date range picker itself.
- A per-bar TalkBack action. The chart keeps its single summary description. The days are still reachable through History, which is fully accessible.

## Decisions

### D1. Connected button group, our own
- **Component:** `ConnectedButtonGroup(options, selectedIndex, onSelect)` in `ui/components/`. It is a `Row` of `Surface` buttons, 48 dp tall, with 2 dp gaps.
- **Shapes:**
  - the selected button is a full pill;
  - an unselected one has fully round outer corners and 8 dp inner corners, the M3 Expressive "connected" look.
- **Colors:** the selected button is `primary` / `onPrimary` with a check icon (Material Symbols `check`); the others are `secondaryContainer` / `onSecondaryContainer`.
- **Shape logic:** `connectedCorners(index, count, selected)` returns the four corner kinds (`FULL` or `INNER`) and is tested: first, middle, last, single and selected.
- **Why our own:** the M3 Expressive `ButtonGroup` and `ToggleButton` are internal in Compose Multiplatform 1.9's material3 (#42 D4).

### D2. Readable period label
- **The helper:** `periodLabel(start, end, currentYear, monthShort)` produces:
  - the same month: "7 – 13 oct";
  - different months: "28 sep – 4 oct";
  - a year other than `currentYear`, or a period crossing years: the year after each date ("28 dic 2025 – 3 ene 2026").
- **Month names:** the short names come from a new `month_short_names` string array: "ene… dic" and "Jan… Dec".
- **Tests:** the four shapes.

### D3. Stat cards
- **First row:**
  - `StatCard("Promedio diario", "842", "kcal")` on `primaryContainer`, weight 1.4;
  - `StatCard("Sobre la meta", "1", "/ 7")` on `tertiaryContainer`, or `errorContainer` when any day is over, weight 1.
- **Second row:** one "Promedio de macros" card on `surfaceContainerHigh`, with a `SegmentedBar` of the average grams and its legend ("Proteína 36 g · Grasa 36 g · Carbohidratos 86 g"). Three separate macro cards were dropped on the emulator: at a third of the width, "Carbohidratos" doesn't fit on one line.
- **Below them:** one line, "5895 kcal en el período".
- **Computation:**
  - `periodStats(days, goal)` is a pure function returning the average kcal, the days over the goal and the macro averages. Empty days count as zero, as today;
  - it is tested against the previous inline computation's cases.
- **`summary_period_totals`** (a plural) is removed. Its two facts are now the "over the goal" card and the total line.

### D4. Calorie chart
- **Card:** the chart sits on a `surfaceContainerLow` card with 16 dp padding.
- **Bars:**
  - 70% of the slot;
  - top corners fully rounded and bottom corners at 4 dp, drawn as a `Path` with `RoundRect` per-corner radii;
  - `primary`, or `error` when over the goal (the same `isOverGoal` as before).
- **Goal line:** dashed `outline` across the bars, keyed by a legend above the chart (a short dashed stroke and "Meta diaria: 2000 kcal"). A pill drawn on the line was tried first, but on the emulator it covered the top of a tall bar in month view.
- **Tap a bar:**
  - `pointerInput { detectTapGestures }` maps the x position to a bar with `barIndexAt(x, width, count)` (pure, tested: the edges, the middle and out of range);
  - it calls `onOpenDay(days[i].date)`;
  - every day in a Summary period is today or earlier, so any bar can open.
- **Accessibility:** the chart keeps its content description, which now also says the bars can be tapped. A per-bar action is a non-goal.

### D5. Weight card
- **The card** (`surfaceContainerLow`) holds:
  - the title "Peso" and the "Registrar peso" tonal button;
  - `BigNumber(latest, "kg")`;
  - the period change as a small pill: `tertiaryContainer` when losing, `secondaryContainer` otherwise. There's no judgment color, because the goal direction is unknown.
- **Chart:**
  - same slots and scale as before;
  - the line is a cubic curve through each day's point, with control points from `smoothControlPoints(points)` (Catmull–Rom converted to Bézier, tension 0.5, pure and tested: the curve passes through every point, n − 1 segments, and a horizontal series stays horizontal);
  - a vertical gradient fill from `primary` at 20% alpha to transparent sits under it.
- **Entries:**
  - each shows a short date ("13 oct", from `month_short_names`) and the weight;
  - delete is an `ic_delete` icon button (the "Eliminar" description).
- **Log dialog:** unchanged.

### D6. Export restyle
- `ExportSection`'s button becomes a full-width `FilledTonalButton` with a Material Symbols `download` icon.
- The in-progress state keeps the small inline spinner (#42 D7).
- The flow, the picker and the error messages are unchanged.

### D7. Period state in `AppState`
- **`SummaryPeriod`** becomes public in `ui/summary/`.
- **`AppState.summaryView: SummaryView(period, endDate)`** has `endDate` null meaning "ending today". It's read and written by `SummaryScreen`, so leaving for a day's detail and coming back shows the same period and view.
- **Why null for today:** a "this week" view left open across midnight follows today, as Home does. A past period stays fixed.
- **Tests:** `AppStateTest` covers that opening a day from `Main(SUMMARY)` and closing it returns to `Main(SUMMARY)`, and that `summaryView` is untouched by it.

## Risks / Trade-offs

- **Tapping a narrow bar in month view.** → 30 slots on a phone are about 11 dp each. The tap maps by slot, not by bar, so the whole column counts. Days are also reachable in History.
- **Hand-rolled connected buttons.** → They can be swapped for the official expressive `ButtonGroup` when Compose Multiplatform makes it public; the call site is one line.

## Migration Plan

None. This is a frontend-only visual change, plus a new navigation entry point.
