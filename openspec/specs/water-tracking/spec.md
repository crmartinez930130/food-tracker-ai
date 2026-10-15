# water-tracking Specification

## Purpose

Lets the user log the water they drink through the day with one tap, and see it against a daily target.

## Requirements

### Requirement: Log water
The backend SHALL expose `POST /api/water`, accepting an amount in millilitres, a whole number from 1 to 5000, an optional ISO-8601 timestamp `loggedAt` and an optional `clientId`. When `loggedAt` is omitted the entry SHALL be logged at the current time. A `loggedAt` more than 5 minutes in the future SHALL be rejected. `clientId` is a UUID the client generates once for an entry and repeats on every retry; UUIDs SHALL be compared case-insensitively. On success the response SHALL be 201 with the stored entry (`id`, `amountMl`, `loggedAt`, `clientId`, the latter `null` when none was sent). When a stored entry already has the request's `clientId`, the system SHALL NOT store another entry and SHALL respond 200 with the stored entry, ignoring the other fields of the repeated request. An invalid amount, timestamp or `clientId` SHALL produce a 400 with an error message and SHALL NOT store anything.

#### Scenario: Log a glass now
- **WHEN** the client posts `{ "amountMl": 250 }`
- **THEN** the response is 201 with the entry, and today's water total includes those 250 ml

#### Scenario: Log into a past day
- **WHEN** the client posts an amount with a `loggedAt` on an earlier day
- **THEN** the entry counts toward that day's total, not today's

#### Scenario: Invalid values
- **WHEN** the amount is missing, zero, negative, fractional or above 5000, `loggedAt` is not a valid timestamp or is in the future, or `clientId` is not a UUID
- **THEN** the response is 400 and nothing is stored

#### Scenario: Retry of an entry that was already stored
- **WHEN** the client posts water with a `clientId` that a stored entry already has
- **THEN** the response is 200 with the stored entry, and the day's total counts it only once

#### Scenario: Log without a client id
- **WHEN** the client posts the same amount twice without a `clientId`
- **THEN** two entries are stored, as before

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
Home SHALL show today's water consumed against the daily water target, as a figure and a progress indicator, on its own card. It SHALL offer a single-tap action that logs one glass, using the glass size from the goal, at the current time. It SHALL also offer an undo action that removes today's most recent entry, available only when today has entries. Logging or undoing SHALL update the shown total without reloading the day's meals. A failed log or undo SHALL show an error message. The water shown SHALL come from the backend, so it is the same after closing and reopening the app. Going over the water target SHALL NOT be shown as an error.

#### Scenario: Log a glass from Home
- **WHEN** the user taps the add-glass action with a glass size of 250 ml
- **THEN** the shown total grows by 250 ml in one tap

#### Scenario: Undo a mistaken tap
- **WHEN** the user taps undo after adding a glass by mistake
- **THEN** the latest entry is deleted and the total goes back to its previous value

#### Scenario: Over the water target
- **WHEN** today's water exceeds the water target
- **THEN** the progress indicator is full and keeps its normal color

#### Scenario: Past day
- **WHEN** a glass was logged yesterday and the user opens Home today
- **THEN** Home's water total does not include it

#### Scenario: Reopen the app
- **WHEN** the user closes and reopens the app after logging water today
- **THEN** Home shows the same water total for today
