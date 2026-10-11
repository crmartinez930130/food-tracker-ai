# water-tracking Specification

## Purpose

Lets the user log the water they drink through the day with one tap, and see it against a daily target.

## Requirements

### Requirement: Log water
The backend SHALL expose `POST /api/water`, accepting an amount in millilitres, a whole number from 1 to 5000, and an optional ISO-8601 timestamp `loggedAt`. When `loggedAt` is omitted the entry SHALL be logged at the current time. A `loggedAt` more than 5 minutes in the future SHALL be rejected. On success the response SHALL be 201 with the stored entry (`id`, `amountMl`, `loggedAt`). An invalid amount or timestamp SHALL produce a 400 with an error message and SHALL NOT store anything.

#### Scenario: Log a glass now
- **WHEN** the client posts `{ "amountMl": 250 }`
- **THEN** the response is 201 with the entry, and today's water total includes those 250 ml

#### Scenario: Log into a past day
- **WHEN** the client posts an amount with a `loggedAt` on an earlier day
- **THEN** the entry counts toward that day's total, not today's

#### Scenario: Invalid values
- **WHEN** the amount is missing, zero, negative, fractional or above 5000, or `loggedAt` is not a valid timestamp or is in the future
- **THEN** the response is 400 and nothing is stored

### Requirement: Water for a day
The backend SHALL expose `GET /api/water?date=YYYY-MM-DD`, returning that local calendar day's total in millilitres and its individual entries in chronological order. An entry SHALL belong to the user's local calendar day of its `loggedAt`, the same rule used for meals. A day without entries SHALL return a total of 0 and an empty list. A missing or impossible date SHALL produce a 400.

#### Scenario: Day with entries
- **WHEN** three entries of 250 ml were logged today and the client requests today
- **THEN** the response has a total of 750 and the three entries, oldest first

#### Scenario: Day without entries
- **WHEN** the client requests a day with no water logged
- **THEN** the response has a total of 0 and an empty list

#### Scenario: Late-evening entry
- **WHEN** an entry was logged at 23:30 local time, which is already the next day in UTC
- **THEN** it counts toward the local day it was logged on

#### Scenario: Invalid date
- **WHEN** the date is missing or is not a real calendar date, such as `2026-02-30`
- **THEN** the response is 400

### Requirement: Delete a water entry
The backend SHALL expose `DELETE /api/water/:id`, removing the entry and responding 204. The day's total SHALL no longer include it. An unknown id SHALL produce a 404 and an invalid id a 400.

#### Scenario: Remove a mistaken tap
- **WHEN** the client deletes an existing entry
- **THEN** the response is 204, and that day's total drops by the entry's amount

#### Scenario: Unknown entry
- **WHEN** the client deletes an id that does not exist
- **THEN** the response is 404

### Requirement: Water on Home
Home SHALL show, for the day it is displaying, the water consumed against the daily water target, as text and a progress indicator. It SHALL offer a single-tap action that logs one glass, using the glass size from the goal, into that day: at the current time when the day is today, and within the displayed day when it is a past day. It SHALL also offer an undo action that removes the day's most recent entry, available only when the day has entries. Logging or undoing SHALL update the shown total without reloading the day's meals. A failed log or undo SHALL show an error message. The water shown SHALL come from the backend, so it is the same after closing and reopening the app.

#### Scenario: Log a glass from Home
- **WHEN** the user taps the add-glass action on today with a glass size of 250 ml
- **THEN** the shown total grows by 250 ml in one tap

#### Scenario: Undo a mistaken tap
- **WHEN** the user taps undo after adding a glass by mistake
- **THEN** the latest entry is deleted and the total goes back to its previous value

#### Scenario: Past day
- **WHEN** the user is viewing yesterday on Home and taps the add-glass action
- **THEN** the glass is added to yesterday's total, not today's

#### Scenario: Reopen the app
- **WHEN** the user closes and reopens the app after logging water today
- **THEN** Home shows the same water total for today
