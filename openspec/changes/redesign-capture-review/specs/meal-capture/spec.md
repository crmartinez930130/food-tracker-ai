## ADDED Requirements

### Requirement: Review shows the total on the save action
The review screen SHALL show the meal's total calories as a large figure and its protein, fat and carbohydrate composition, recomputed as the user edits, adds or removes foods. The save action SHALL show the current total calories ("Save · 736 kcal") and SHALL stay visible at the bottom of the screen while the user scrolls the foods.

#### Scenario: Edit a food's calories
- **WHEN** the user changes a food's calories from 300 to 400 on a meal totalling 736 kcal
- **THEN** the total figure and the save action both show 836 kcal

#### Scenario: Remove a food
- **WHEN** the user removes a food
- **THEN** the total figure, the macro composition and the save action update without it

#### Scenario: Long list
- **WHEN** the review has more foods than fit on the screen
- **THEN** the save action stays visible at the bottom while the list scrolls under it
