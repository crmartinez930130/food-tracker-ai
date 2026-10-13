# nutrition-summary Specification

## Purpose

Gives the user an at-a-glance view of total calories and macros consumed over a day or week, computed from their saved meal history.

## Requirements

### Requirement: Daily nutrition summary
The system SHALL compute and return total calories, protein, fat, and carbohydrates for all meal entries saved on a given date.

#### Scenario: Day with saved meals
- **WHEN** the client requests the daily summary for a date that has saved meal entries
- **THEN** the system returns totals that equal the sum of calories and macros across all entries for that date

#### Scenario: Day with no saved meals
- **WHEN** the client requests the daily summary for a date with no saved meal entries
- **THEN** the system returns zeroed totals rather than an error

### Requirement: Weekly nutrition summary
The system SHALL compute and return total and average-per-day calories and macros for a 7-day period starting from a given date.

#### Scenario: Week with meals on some days
- **WHEN** the client requests the weekly summary for a period where only some days have saved meals
- **THEN** the system returns totals summed across all entries in the period, and an average per day computed over all 7 days (days with no meals count as zero)

### Requirement: Summary reflects current data
The system SHALL compute summaries from the current state of saved meal entries at request time, not from a cached or precomputed value.

#### Scenario: Summary requested after an edit
- **WHEN** a meal entry is edited or deleted and the client then requests a summary covering that entry's date
- **THEN** the returned totals reflect the edit or deletion

### Requirement: Per-day totals over a date range
The system SHALL return, for an inclusive date range, one entry per calendar day containing that day's date, its total calories, protein, fat and carbohydrates, its number of meals, and its total water in millilitres. The entries SHALL be ordered by date, and days with no meals SHALL appear with zero totals and a meal count of zero. Water SHALL be counted by the same local-day rule as meals, independently of whether the day has meals. The system SHALL reject a malformed date, a range whose end is before its start, and a range longer than 366 days.

#### Scenario: Range with meals on every day
- **WHEN** the client requests a range in which every day has saved meals
- **THEN** the system returns one entry per day, in date order, each with the sum of that day's meals

#### Scenario: Range with empty days
- **WHEN** the client requests a range in which some days have no meals
- **THEN** those days are included with zero totals rather than omitted, and the number of entries equals the number of days in the range

#### Scenario: Single-day range
- **WHEN** the client requests a range whose start and end are the same date
- **THEN** the system returns exactly one entry for that date

#### Scenario: Invalid range
- **WHEN** the client requests a range with a malformed date, an end before the start, or more than 366 days
- **THEN** the system responds with a 400-style validation error

#### Scenario: Meal count and water
- **WHEN** the client requests a range in which one day has two meals and 750 ml of water, and another has only 500 ml of water
- **THEN** the first day's entry has a meal count of 2 and 750 ml, and the second has a meal count of 0, zero calories and 500 ml

### Requirement: Calorie trend chart in the app
The Summary screen SHALL show a chart of calories per day for the selected period (the last 7 or 30 days by default), with a labeled reference line at the daily calorie goal and the days above the goal visually distinguished. It SHALL label the period with readable short dates, and SHALL show stat cards with:
- the daily calorie average;
- the number of days over the goal out of the period's days;
- the daily average of each macro;
- the period's total calories.

The user SHALL be able to switch between week and month with a connected button group, and move to earlier or later periods, and the chart and stats SHALL update accordingly.

#### Scenario: Current week
- **WHEN** the user opens Summary on 13 October
- **THEN** it shows "7 – 13 oct" (or "7 – 13 Oct" in English), a bar per day for those 7 days, a labeled goal line, and stat cards with the daily average, the days over the goal out of 7, and each macro's average

#### Scenario: Day above the goal
- **WHEN** a day in the period exceeds the daily calorie goal
- **THEN** that day's bar is visually distinguished from days at or under the goal, and it is counted in the days-over-the-goal card

#### Scenario: Switch to month and navigate
- **WHEN** the user switches to month, or moves to the previous or next period
- **THEN** the chart, the period label and the stat cards update to the newly selected period

#### Scenario: Period with no meals
- **WHEN** the selected period has no logged meals
- **THEN** the chart shows empty bars with the goal line, and the stat cards show zeros, rather than an error

#### Scenario: Period in another year
- **WHEN** the selected period crosses into a new year or lies in a past year
- **THEN** the period label includes the year of each date

### Requirement: Reject impossible dates in summaries
The daily and weekly summaries SHALL reject a date that is malformed or is not a real calendar date, the same way the per-day range summary does.

#### Scenario: Impossible date in the daily summary
- **WHEN** the client requests the daily summary for a date that is formatted correctly but doesn't exist
- **THEN** the system responds with a 400-style validation error

#### Scenario: Impossible start date in the weekly summary
- **WHEN** the client requests the weekly summary starting on a date that doesn't exist
- **THEN** the system responds with a 400-style validation error

### Requirement: Loading indication while the Summary period changes
When the user moves to another period or switches between week and month, the Summary screen SHALL visibly mark the data on screen as outdated until the new period's calorie and weight data have loaded, instead of showing the previous period's data unmarked under the new period's dates.

#### Scenario: Change period
- **WHEN** the user moves to the previous period and its data is still loading
- **THEN** the previously shown chart, averages and weight section are dimmed and a progress indicator is shown, and both return to normal once the new data is displayed

### Requirement: Open a day from the Summary chart
Tapping a bar of the Summary calorie chart SHALL open that day's detail. Going back from the detail SHALL return to Summary showing the same period and the same week or month view.

#### Scenario: Tap a bar
- **WHEN** the user taps Thursday's bar in the week chart
- **THEN** the detail of that Thursday opens

#### Scenario: Return keeps the period
- **WHEN** the user switches to month, moves to the previous month, opens a day from the chart and goes back
- **THEN** Summary still shows month view on that previous period
