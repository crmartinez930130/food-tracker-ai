## ADDED Requirements

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
