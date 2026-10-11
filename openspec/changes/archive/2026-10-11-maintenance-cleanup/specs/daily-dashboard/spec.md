## ADDED Requirements

### Requirement: Loading indication while Home changes day
When the user moves to another day, Home SHALL visibly mark the progress and meals on screen as outdated until the new day's data has loaded, instead of showing the previous day's data unmarked under the new day's label.

#### Scenario: Change day
- **WHEN** the user moves to the previous day and its data is still loading
- **THEN** the previously shown progress and meals are dimmed and a progress indicator is shown, and both return to normal once the new day's data is displayed

#### Scenario: First load
- **WHEN** Home opens and has no data loaded yet
- **THEN** it shows a loading indicator instead of empty or zero progress
