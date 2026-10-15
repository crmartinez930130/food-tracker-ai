## MODIFIED Requirements

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
