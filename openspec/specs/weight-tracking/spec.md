# weight-tracking Specification

## Purpose

Lets the user record their body weight over time and see how it trends alongside what they eat.

## Requirements

### Requirement: Log a weight entry
The system SHALL let the client record a body weight in kilograms for a calendar day, defaulting to the current local day when none is given, and SHALL return the created entry. Several entries for the same day SHALL be allowed. The system SHALL reject a weight that is not a number greater than 0 and at most 500 kg, a malformed or impossible date, and a date in the future.

#### Scenario: Log today's weight
- **WHEN** the client logs a valid weight without a date
- **THEN** the system persists it for the current local day and returns the entry with its id, weight and day

#### Scenario: Log a past day's weight
- **WHEN** the client logs a valid weight for an earlier day
- **THEN** the system persists it for that day

#### Scenario: Invalid weight or date
- **WHEN** the client logs a weight of 0 or less, above 500, or not a number, or with a malformed, impossible or future date
- **THEN** the system responds with a 400-style validation error and stores nothing

### Requirement: List weight entries in a range
The system SHALL return the weight entries whose day falls within an inclusive date range, in chronological order (by day, then by creation time), and SHALL return an empty list when the range has none. The system SHALL reject a malformed range, an end before the start, and a range longer than 366 days.

#### Scenario: Entries in range
- **WHEN** the client requests a range containing entries on several days, plus entries outside it
- **THEN** only the entries inside the range are returned, oldest first

#### Scenario: Empty range
- **WHEN** the client requests a range with no entries
- **THEN** the system returns an empty list rather than an error

### Requirement: Delete a weight entry
The system SHALL let the client delete a weight entry by id, and SHALL respond with a 404-style error for an id that doesn't exist.

#### Scenario: Delete an entry
- **WHEN** the client deletes an existing entry
- **THEN** it no longer appears in any listing

#### Scenario: Delete a missing entry
- **WHEN** the client deletes an id that doesn't exist
- **THEN** the system responds with a 404-style error

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
