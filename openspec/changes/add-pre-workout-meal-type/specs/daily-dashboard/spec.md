## MODIFIED Requirements

### Requirement: Meals on Home reflect changes
Home SHALL list today's meals grouped by meal type, in the order pre-workout, breakfast, lunch, dinner, snack and untagged ("Other"). Each group SHALL be headed by an icon specific to its type and the group's total calories. Home SHALL let the user delete a meal from the list, and SHALL update the totals after any change to today's meals.

#### Scenario: Grouped by type
- **WHEN** today has a breakfast and two snacks
- **THEN** Home shows a breakfast group and a snack group, each with its own icon and its total calories, and the snack group lists both snacks

#### Scenario: Pre-workout group
- **WHEN** today has a breakfast, a pre-workout and a lunch
- **THEN** Home shows the pre-workout group first, above the breakfast and lunch groups, with its own icon and its total calories

#### Scenario: Save a new meal
- **WHEN** the user captures, reviews and saves a meal
- **THEN** the app returns to Home, and the new meal and its calories and macros are reflected in the list and the totals

#### Scenario: Delete a meal
- **WHEN** the user deletes a meal from Home
- **THEN** the meal disappears from the list and the totals decrease accordingly
