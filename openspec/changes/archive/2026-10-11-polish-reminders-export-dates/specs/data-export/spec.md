## ADDED Requirements

### Requirement: Earliest meal date
The backend SHALL expose `GET /api/meals/first-date`, returning `{ "date": "YYYY-MM-DD" }` with the local calendar day of the earliest logged meal, or `{ "date": null }` when no meals exist.

#### Scenario: Meals exist
- **WHEN** the earliest meal occurred on 2026-09-30 local time
- **THEN** the response is 200 with `{ "date": "2026-09-30" }`

#### Scenario: No meals
- **WHEN** no meal has ever been logged
- **THEN** the response is 200 with `{ "date": null }`

## MODIFIED Requirements

### Requirement: Export from the app
The Summary screen SHALL offer an export action that opens a date-range picker pre-selected with the period Summary is currently showing. Days after today SHALL NOT be selectable. The picker's years SHALL run from the year of the earliest logged meal, or the year the pre-selected period starts if that is earlier, to the current year. When the earliest meal date is unknown, because there are no meals or the request failed, the picker SHALL offer a range of years ending in the current year that includes the pre-selected period. A selected range longer than 366 days SHALL be rejected with a message before any request is made. Confirming SHALL download the CSV for the chosen range and hand it as a file to the platform's share sheet (on Android, a share intent with a `text/csv` attachment), instead of saving it silently. A failed export SHALL show an error on the screen.

#### Scenario: Export the current week
- **WHEN** the user is on Summary showing Week, taps export and confirms the pre-selected range
- **THEN** the share sheet opens with a CSV file named after that week's dates

#### Scenario: Picker years follow the data
- **WHEN** the earliest logged meal is from 2024 and the user opens the export picker in 2026
- **THEN** the picker offers the years 2024 to 2026

#### Scenario: Range too long
- **WHEN** the user picks a range longer than 366 days
- **THEN** the dialog explains the limit and the export cannot be confirmed

#### Scenario: Backend unreachable
- **WHEN** the export request fails
- **THEN** Summary shows an error message and no share sheet opens
