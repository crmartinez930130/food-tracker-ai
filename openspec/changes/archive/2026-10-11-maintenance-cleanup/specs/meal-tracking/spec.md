## ADDED Requirements

### Requirement: Readable validation errors for meals
When the system rejects a meal save or update because of invalid input, it SHALL respond with a 400-style error whose message names each offending field with a human-readable reason, and SHALL NOT expose raw validation data structures.

#### Scenario: Invalid meal save
- **WHEN** the client saves a meal whose items are missing or malformed
- **THEN** the system responds with a 400-style error whose message has the form "field: reason" for each problem, with no raw JSON

#### Scenario: Invalid meal update
- **WHEN** the client updates a meal with a value of the wrong type, such as a non-boolean favorite flag
- **THEN** the system responds with a 400-style error naming that field, and the meal is unchanged

### Requirement: Reject impossible dates when listing meals
The system SHALL reject a meal listing request whose date is malformed or is not a real calendar date, such as February 30th.

#### Scenario: Impossible date
- **WHEN** the client lists meals for a date that is formatted correctly but doesn't exist
- **THEN** the system responds with a 400-style validation error rather than an empty list
