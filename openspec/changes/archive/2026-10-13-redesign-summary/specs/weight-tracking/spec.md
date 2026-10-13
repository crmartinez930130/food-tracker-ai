## MODIFIED Requirements

### Requirement: Weight in the app
The Summary screen SHALL show a weight card for the selected period with a trend chart, drawn as a smooth curve through the last entry of each day and aligned to the period's days. The card SHALL show the latest weight as a large figure and its change across the period. It SHALL let the user log a weight for today, with the field pre-filled from the last entry, and delete the period's entries.

#### Scenario: Trend with entries
- **WHEN** the selected period has weight entries on at least two days
- **THEN** the chart shows a smooth line through each day's last entry, and the section shows the latest weight and the change from the first to the last day with data

#### Scenario: Log from the app
- **WHEN** the user logs a weight from the Summary screen
- **THEN** it is saved for today and the chart, latest weight and entry list update

#### Scenario: No entries in the period
- **WHEN** the selected period has no weight entries
- **THEN** the section shows a short message inviting the user to log their weight instead of an empty chart

#### Scenario: Invalid weight in the app
- **WHEN** the user enters a value that isn't a positive number up to 500
- **THEN** the dialog shows an error and nothing is saved
