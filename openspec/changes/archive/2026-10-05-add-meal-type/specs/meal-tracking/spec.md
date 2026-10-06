# Spec Delta

## ADDED Requirements

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
