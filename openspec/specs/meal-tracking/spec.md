# meal-tracking Specification

## Purpose

Lets the user turn a reviewed food analysis into a permanent, correctable record of what they ate, and browse that history.

## Requirements

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

### Requirement: Categorize a meal entry by type
The system SHALL allow a meal entry to optionally carry a meal type (`breakfast`, `lunch`, `dinner`, or `snack`) that is persisted and returned alongside the entry's other fields.

#### Scenario: Save a meal with a type
- **WHEN** the client confirms a meal and specifies a meal type
- **THEN** the system persists that type with the entry and returns it in the created entry

#### Scenario: Save a meal without a type
- **WHEN** the client confirms a meal without specifying a meal type
- **THEN** the system persists the entry with no type set, rather than rejecting the request

#### Scenario: Edit a meal's type
- **WHEN** the client updates an existing meal entry's type
- **THEN** the system persists the new type and subsequent reads of that entry reflect it

#### Scenario: Entries saved before meal types existed remain valid
- **WHEN** the client retrieves a meal entry that was saved before this capability existed
- **THEN** the system returns it normally with no type set, rather than an error

### Requirement: Mark a meal as a favorite
The system SHALL let the client mark and unmark a saved meal entry as a favorite, SHALL return each entry's favorite state with the entry, and SHALL treat entries saved before this capability existed as not favorite.

#### Scenario: Mark a meal as favorite
- **WHEN** the client updates an existing meal entry setting it as a favorite
- **THEN** the system persists that state and subsequent reads of the entry show it as a favorite

#### Scenario: Unmark a favorite
- **WHEN** the client updates a favorite meal entry setting it as not a favorite
- **THEN** subsequent reads show it as not a favorite and it no longer appears in the favorites list

#### Scenario: New and pre-existing meals are not favorites
- **WHEN** the client saves a new meal entry, or reads one saved before favorites existed
- **THEN** the entry is reported as not a favorite

#### Scenario: Invalid favorite value
- **WHEN** the client sends a favorite value that is not a boolean
- **THEN** the system responds with a 400-style validation error and the entry is unchanged

### Requirement: List favorite meals
The system SHALL return all meal entries currently marked as favorite, regardless of the date they were logged, most recent first.

#### Scenario: Favorites across dates
- **WHEN** favorite meals exist on different dates along with non-favorite meals
- **THEN** the system returns only the favorite meals, from every date, ordered from most to least recently logged

#### Scenario: No favorites
- **WHEN** no meal is marked as favorite
- **THEN** the system returns an empty list rather than an error
