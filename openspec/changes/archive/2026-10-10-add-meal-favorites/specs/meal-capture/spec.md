## ADDED Requirements

### Requirement: Re-log a favorite meal without analysis
The capture screen SHALL list the user's favorite meals as an alternative to taking or choosing a photo. Choosing one SHALL open the review screen pre-filled with that meal's foods, quantities, macros and notes, without calling the photo analysis service, and confirming SHALL save a new meal entry timestamped now, leaving the original favorite unchanged.

#### Scenario: Re-log a favorite
- **WHEN** the user picks a favorite on the capture screen and confirms the pre-filled review
- **THEN** a new meal entry is saved for the current date and time with the favorite's foods and macros, the original entry is unchanged, and the photo analysis service is not called

#### Scenario: Edit before re-logging
- **WHEN** the user changes quantities or macros in the pre-filled review before confirming
- **THEN** the new entry is saved with the edited values, and the original favorite keeps its own values

#### Scenario: Re-logged copy is not a favorite
- **WHEN** a favorite has been re-logged
- **THEN** the new entry is not itself a favorite, so the favorites list does not gain a duplicate

#### Scenario: No favorites yet
- **WHEN** the user has no favorite meals
- **THEN** the capture screen shows the photo options and a short hint that starring a meal on Home makes it available here

#### Scenario: Remove a favorite from the capture screen
- **WHEN** the user unmarks a favorite from the capture screen's list
- **THEN** it disappears from the list and the original meal is no longer a favorite
