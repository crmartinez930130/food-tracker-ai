# Spec Delta

## Purpose

Lets the user turn a reviewed food analysis into a permanent, correctable record of what they ate, and browse that history.

## ADDED Requirements

### Requirement: Confirm and save a meal entry
The system SHALL allow the client to save a meal entry built from an analysis result, including any corrections the user made to detected foods, quantities, or macros before confirming.

#### Scenario: Save an unmodified analysis
- **WHEN** the client confirms a meal using the analysis result exactly as returned
- **THEN** the system creates a persisted meal entry with that data, timestamped to the current date and time, and returns the created entry

#### Scenario: Save a corrected analysis
- **WHEN** the client confirms a meal after editing item names, quantities, or macro values from the original analysis
- **THEN** the system persists the corrected values, not the original AI-generated values

#### Scenario: Save with an explicit date/time
- **WHEN** the client specifies a date/time for the meal (e.g., logging a meal after the fact)
- **THEN** the system persists the entry under that date/time instead of the current time

### Requirement: Retrieve meal entries
The system SHALL allow the client to list meal entries for a given date and to retrieve a single meal entry by id.

#### Scenario: List entries for a date with meals
- **WHEN** the client requests meal entries for a date that has one or more saved entries
- **THEN** the system returns all entries for that date, each with its foods, quantities, and macros

#### Scenario: List entries for a date with no meals
- **WHEN** the client requests meal entries for a date with no saved entries
- **THEN** the system returns an empty list rather than an error

#### Scenario: Retrieve a nonexistent entry
- **WHEN** the client requests a meal entry by an id that does not exist
- **THEN** the system returns a 404-style error

### Requirement: Edit and delete meal entries
The system SHALL allow the client to modify or remove a previously saved meal entry.

#### Scenario: Edit a saved entry
- **WHEN** the client updates the foods, quantities, or macros of an existing meal entry
- **THEN** the system persists the updated values and the entry's totals reflect the update on subsequent reads

#### Scenario: Delete a saved entry
- **WHEN** the client deletes an existing meal entry
- **THEN** the entry is removed and no longer appears in date listings or summary calculations
