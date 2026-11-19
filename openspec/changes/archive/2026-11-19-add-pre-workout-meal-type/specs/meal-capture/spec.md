## ADDED Requirements

### Requirement: Review suggests a meal type by time of day
The review screen SHALL let the user choose the meal's type among pre-workout, breakfast, lunch, dinner and snack, in that order, showing all five options at once with their full labels on a phone-width screen. For a new meal it SHALL preselect a type from the local time of the meal: breakfast before 07:00, pre-workout from 07:00 to 08:59, breakfast from 09:00 to 10:59, lunch from 11:00 to 14:59, snack from 15:00 to 18:59 and dinner from 19:00. The user SHALL be able to change the suggestion before saving, and the chosen type is the one saved. The labels SHALL be available in Spanish ("Preentreno") and English ("Pre-workout").

#### Scenario: Meal at 07:30
- **WHEN** the user reviews a new meal captured at 07:30
- **THEN** pre-workout is preselected

#### Scenario: Meal at 09:30
- **WHEN** the user reviews a new meal captured at 09:30
- **THEN** breakfast is preselected

#### Scenario: Meal at 06:45
- **WHEN** the user reviews a new meal captured at 06:45
- **THEN** breakfast is preselected

#### Scenario: Override the suggestion
- **WHEN** a meal at 07:30 is preselected as pre-workout and the user selects breakfast and saves
- **THEN** the meal is saved as breakfast

#### Scenario: Narrow phone
- **WHEN** the review screen is shown on a 360 dp wide screen in Spanish
- **THEN** all five type options are visible in full, without truncated labels
