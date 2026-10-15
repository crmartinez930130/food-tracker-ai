## ADDED Requirements

### Requirement: Save a meal at most once per client id
`POST /api/meals` SHALL accept an optional `clientId`: a UUID the client generates once for a meal and repeats on every retry of that save. UUIDs SHALL be compared case-insensitively. When no stored meal has that `clientId`, the meal SHALL be created as usual and the response SHALL be 201. When a stored meal already has that `clientId`, the system SHALL NOT create another meal and SHALL respond 200 with the stored meal, ignoring the other fields of the repeated request. A `clientId` that is not a UUID SHALL produce a 400 and SHALL NOT store anything. A request without `clientId` SHALL behave as before. Every meal entry returned by the API SHALL include `clientId`, `null` when the meal was saved without one.

#### Scenario: First save with a client id
- **WHEN** the client saves a meal with a `clientId` no stored meal has
- **THEN** the response is 201 with the created meal, including that `clientId`

#### Scenario: Retry of a save that was already stored
- **WHEN** the client saves a meal with a `clientId` that a stored meal already has
- **THEN** the response is 200 with the stored meal, and the day's meal list still contains it only once

#### Scenario: Retry with the client id in another letter case
- **WHEN** the client repeats a save with the same UUID written in upper case
- **THEN** it is treated as the same meal and nothing new is created

#### Scenario: Invalid client id
- **WHEN** the client saves a meal with a `clientId` that is not a UUID
- **THEN** the response is 400 with an error message and nothing is stored

#### Scenario: Save without a client id
- **WHEN** the client saves the same meal twice without a `clientId`
- **THEN** two meals are created, as before, and both are returned with `clientId` `null`
