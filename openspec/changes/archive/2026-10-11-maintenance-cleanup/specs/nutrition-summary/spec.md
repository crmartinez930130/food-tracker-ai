## ADDED Requirements

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
