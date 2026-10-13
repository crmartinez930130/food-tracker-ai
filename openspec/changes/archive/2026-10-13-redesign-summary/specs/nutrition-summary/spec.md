## MODIFIED Requirements

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

## ADDED Requirements

### Requirement: Open a day from the Summary chart
Tapping a bar of the Summary calorie chart SHALL open that day's detail. Going back from the detail SHALL return to Summary showing the same period and the same week or month view.

#### Scenario: Tap a bar
- **WHEN** the user taps Thursday's bar in the week chart
- **THEN** the detail of that Thursday opens

#### Scenario: Return keeps the period
- **WHEN** the user switches to month, moves to the previous month, opens a day from the chart and goes back
- **THEN** Summary still shows month view on that previous period
