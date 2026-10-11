# Design

## Context

- **Backend:** meals store `occurred_at` as a UTC timestamp and group days with `date(occurred_at,'localtime')`. In #7, `/api/summary/range` gained `isCalendarDate` (a regex plus a calendar round-trip) and the 366-day cap, both defined locally in `routes/summary.ts`.
- **Frontend:** in #7, `SummaryScreen` gained a Week/Month period with an `endDate`, a `CalorieChart` drawn with `Canvas`, and pure helpers in `ui/summary/ChartLayout.kt`. Bottom navigation has only Home and Summary, by the user's choice in #3.

See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- Logging today's weight takes three taps: Summary, "Log weight", Save.
- The weight trend reads on the same timeline as calories, using the same period and the same day slots.
- Reuse #7's charting approach and date validation, with no new dependency.

**Non-Goals:**
- Pounds or other units. Only kg for now.
- A weight goal, BMI, smoothed or moving-average trend lines.
- Editing an entry in place. Deleting it and logging it again covers corrections.
- A separate navigation destination for weight.

## Decisions

**1. Store a local calendar day, not a timestamp.**
`weight_logs(id, weight_kg REAL NOT NULL, day TEXT NOT NULL, created_at TEXT NOT NULL)`, with an index on `day`. Weight is inherently a per-day measurement, so storing the user's day avoids the UTC/local conversion that meals need, and range queries become plain string comparisons. `created_at` (UTC ISO) orders multiple entries within the same day.
- *Alternative:* a `logged_at` timestamp, as the issue suggests. That would need the same `'localtime'` conversion as meals on every query, for no benefit.

**2. No uniqueness per day, and the chart uses each day's last entry.**
The API stores every entry, as the issue states. `latestPerDay(entries)` (pure, tested) reduces them to one point per day for the chart and for the "latest" and "change" figures. All entries still appear in the list, so a mistaken extra entry is visible and can be deleted.

**3. Shared date validation.** `isCalendarDate`, `MAX_RANGE_DAYS` and the range checks move from `routes/summary.ts` into `routes/dates.ts`, and both routers import them. "Not in the future" for `POST` compares with the server's local today, which matches how meals define the day.

**4. Validation is done with Zod.** `LogWeightRequestSchema` is `{ weightKg: number > 0 and ≤ 500, day?: string }`. The `day` is then checked with `isCalendarDate` and "not after today". Errors are formatted as `field: message`, like `/api/goals`.

**5. Placement: a Weight section inside Summary, not a new tab or screen.**
Summary is already the place for trends, and showing weight below the calorie chart for the same period is the correlation the issue is after. Navigation stays at the two destinations the user chose in #3. The section contains:
- a header row with "Weight", the latest value and its change (for example "72.4 kg · −0.8 kg"), and a "Log weight" button;
- `WeightChart`;
- the period's entries, newest first, each with its date, the weight and a delete icon button.

"Log weight" opens an `AlertDialog` with a decimal `OutlinedTextField`, pre-filled with the last weight because it rarely changes much, and Save and Cancel buttons. Invalid input shows an error in the dialog. On success, Summary reloads the weight data.
- *Alternative:* a dedicated screen reached from Home. It would add a route and back handling for a single numeric field.

**6. `WeightChart` is a `Canvas` line aligned to the calorie chart's day slots.**
- It uses the same width and slot math as `CalorieChart`, so day N sits under the same x position.
- Points are plotted only on days that have an entry, and are connected by straight lines that skip days without data.
- The y axis isn't zero-based, because weight changes are small relative to its value. `weightScale(values)` (pure, tested) returns a min and max padded by 0.5 kg below and above, at least 2 kg apart so a flat series doesn't look like noise.
- The line uses `primary` and the points are filled circles. There are no x labels: the calorie chart above already labels the days.
- It provides a `contentDescription` with the first and last values and the change.

## Risks / Trade-offs

- **[Two charts on one screen get long on small phones]** → Summary already scrolls. The weight chart is shorter, about 140dp.
- **[Logging several times a day can look confusing]** → The chart uses the last entry per day, and the list shows every entry with a delete button.
- **[Moving `isCalendarDate` touches #7's route]** → It is a pure move with no behavior change, and the existing summary route tests keep covering it.
