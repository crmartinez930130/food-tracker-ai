## ADDED Requirements

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
