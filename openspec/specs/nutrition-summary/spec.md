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
The system SHALL return, for an inclusive date range, one entry per calendar day containing that day's date and its total calories, protein, fat and carbohydrates. The entries SHALL be ordered by date, and days with no meals SHALL appear with zero totals. The system SHALL reject a malformed date, a range whose end is before its start, and a range longer than 366 days.

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

### Requirement: Calorie trend chart in the app
The Summary screen SHALL show a chart of calories per day for the selected period (the last 7 or 30 days by default), with a reference line at the daily calorie goal, days above the goal visually distinguished, and the period's daily averages alongside. The user SHALL be able to switch between week and month and move to earlier or later periods, and the chart SHALL update accordingly.

#### Scenario: Current week
- **WHEN** the user opens Summary
- **THEN** it shows a bar per day for the last 7 days ending today, a goal reference line, and the daily averages for those days

#### Scenario: Day above the goal
- **WHEN** a day in the period exceeds the daily calorie goal
- **THEN** that day's bar is visually distinguished from days at or under the goal

#### Scenario: Switch to month and navigate
- **WHEN** the user switches to month, or moves to the previous or next period
- **THEN** the chart and averages update to show the days of the newly selected period

#### Scenario: Period with no meals
- **WHEN** the selected period has no logged meals
- **THEN** the chart shows empty bars with the goal line and zero averages, rather than an error

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
