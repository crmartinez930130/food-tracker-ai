## MODIFIED Requirements

### Requirement: Home is the day's diary
The app SHALL open on a Home screen that always shows the current calendar day. It SHALL show the calories consumed against the daily goal, the protein, fat and carbohydrate grams against their targets, today's water and today's meals. Home SHALL NOT offer a way to show another day.

#### Scenario: Open the app
- **WHEN** the user opens the app
- **THEN** Home shows today's calories, macros, water and meals

#### Scenario: Day with no meals
- **WHEN** today has no logged meals
- **THEN** Home shows zero calories against the goal and a message that nothing has been logged yet, rather than an error

### Requirement: Calorie progress against the goal
Home SHALL show the calories consumed today as a large figure with its unit. Next to it, a badge in an expressive shape SHALL show the consumed calories as a whole-number percentage of the daily calorie goal, not capped at 100. Home SHALL also show the goal and either how many calories remain or by how much the goal was exceeded. Tapping the badge SHALL open the goal screen.

#### Scenario: Under the goal
- **WHEN** 1240 kcal were consumed against a goal of 2000 kcal
- **THEN** Home shows "1240 kcal", a badge reading "62%", the goal of 2000 kcal and 760 kcal remaining

#### Scenario: Over the goal
- **WHEN** 2240 kcal were consumed against a goal of 2000 kcal
- **THEN** the badge reads "112%" and is visually distinguished from the under-goal state, and Home shows that the goal was exceeded by 240 kcal

#### Scenario: Open the goal from the badge
- **WHEN** the user taps the percentage badge
- **THEN** the goal screen opens

### Requirement: Macro progress against targets
Home SHALL show protein, fat and carbohydrates as one segmented bar. Each macro's segment SHALL be proportional to its share of the grams consumed today. A legend SHALL name each macro with its color and show the grams consumed against that macro's target. A legend entry SHALL be visually distinguished when its macro is over target.

#### Scenario: Macros below and above target
- **WHEN** protein is below its target and fat exceeds its target
- **THEN** the bar shows each macro's share of the grams consumed, protein's legend entry shows its grams against its target normally, and fat's entry is distinguished as over target

#### Scenario: Nothing eaten yet
- **WHEN** no grams of any macro were consumed today
- **THEN** the bar is shown empty, and the legend shows 0 g against each target

#### Scenario: Macro target of zero
- **WHEN** a macro's target is zero
- **THEN** its legend entry is still shown without error, distinguished as over target if any grams of that macro were consumed, and shown normally otherwise

### Requirement: Meals on Home reflect changes
Home SHALL list today's meals grouped by meal type, in the order breakfast, lunch, dinner, snack and untagged ("Other"). Each group SHALL be headed by an icon specific to its type and the group's total calories. Home SHALL let the user delete a meal from the list, and SHALL update the totals after any change to today's meals.

#### Scenario: Grouped by type
- **WHEN** today has a breakfast and two snacks
- **THEN** Home shows a breakfast group and a snack group, each with its own icon and its total calories, and the snack group lists both snacks

#### Scenario: Save a new meal
- **WHEN** the user captures, reviews and saves a meal
- **THEN** the app returns to Home, and the new meal and its calories and macros are reflected in the list and the totals

#### Scenario: Delete a meal
- **WHEN** the user deletes a meal from Home
- **THEN** the meal disappears from the list and the totals decrease accordingly

### Requirement: Loading indication while Home changes day
When Home moves to a new day, it SHALL visibly mark the content on screen as outdated until the new day's data has loaded, instead of showing the previous day's data unmarked under the new date.

#### Scenario: Change day
- **WHEN** Home switches to the new day and its data is still loading
- **THEN** the previously shown content is dimmed and a progress indicator is shown, and both return to normal once the new day's data is displayed

#### Scenario: First load
- **WHEN** Home opens and has no data loaded yet
- **THEN** it shows a loading indicator instead of empty or zero progress

## ADDED Requirements

### Requirement: Home header
Home SHALL show a header with:
- a greeting for the time of day: morning from 05:00, afternoon from 12:00, evening from 20:00;
- today's date written out in the app language;
- an action that opens Options;
- an action that opens the goal screen.

#### Scenario: Evening in Spanish
- **WHEN** the app is in Spanish and it is 21:30 on Monday, 13 October
- **THEN** the header reads "Buenas noches" and "Lunes, 13 de octubre"

#### Scenario: Morning in English
- **WHEN** the app is in English and it is 08:00 on Monday, 13 October
- **THEN** the header reads "Good morning" and "Monday, October 13"

### Requirement: Home follows the day change
When the calendar day changes while the app is open, or while it is in the background, Home SHALL switch to the new day by itself, without the user restarting the app. The switch SHALL happen within a minute of midnight if Home is on screen, or when the app returns to the foreground otherwise. The greeting SHALL follow the time of day in the same way.

#### Scenario: Midnight with the app open
- **WHEN** Home is on screen at 23:59 and the clock passes midnight
- **THEN** within a minute Home shows the new date, the new day's meals, water and totals, and the night greeting

#### Scenario: Back from the background on a new day
- **WHEN** the app was left in the background yesterday and is brought back today
- **THEN** Home shows today's date and data

## REMOVED Requirements

### Requirement: Navigate between days on Home
**Reason**: Home becomes a today-only dashboard (issue #43). Reviewing earlier days moves to a dedicated History screen.
**Migration**: History (#48) offers a calendar, a list of days and a day detail with the same content and actions. Until it lands, past days are still charted in Summary, and their data stays in the backend.
