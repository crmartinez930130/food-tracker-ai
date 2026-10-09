## Purpose

The app's home screen: shows how the selected day is going against the user's daily goal, with calorie and macro progress plus that day's meals, and lets the user move between days.

## ADDED Requirements

### Requirement: Home is the day's diary
The app SHALL open on a Home screen that, for a selected day (today by default), shows the calorie progress against the daily goal, the protein, fat and carbohydrate progress against their targets, and the meals logged that day.

#### Scenario: Open the app
- **WHEN** the user opens the app
- **THEN** Home shows today's calorie and macro progress and today's meals

#### Scenario: Day with no meals
- **WHEN** the selected day has no logged meals
- **THEN** Home shows zero progress against the goal and a message that nothing has been logged that day, rather than an error

### Requirement: Calorie progress against the goal
Home SHALL show a circular progress indicator of the calories consumed on the selected day relative to the daily calorie goal, together with the consumed amount, the goal, and how many calories remain or by how much the goal was exceeded.

#### Scenario: Under the goal
- **WHEN** the day's consumed calories are below the goal
- **THEN** the indicator is filled in proportion to consumed / goal and Home shows the calories remaining

#### Scenario: Over the goal
- **WHEN** the day's consumed calories exceed the goal
- **THEN** the indicator is shown full, is visually distinguished from the under-goal state, and Home shows by how many calories the goal was exceeded

### Requirement: Macro progress against targets
Home SHALL show, for protein, fat and carbohydrates, the grams consumed on the selected day against that macro's target as a progress bar with the consumed and target amounts.

#### Scenario: Macros below and above target
- **WHEN** one macro is below its target and another exceeds it
- **THEN** the first bar is filled proportionally, and the second is shown full and visually distinguished as over target

#### Scenario: Macro target of zero
- **WHEN** a macro's target is zero
- **THEN** its bar is still shown without error, as full and over target if any grams of that macro were consumed, and as empty otherwise

### Requirement: Navigate between days on Home
Home SHALL let the user move to the previous and next day without leaving the screen, SHALL label today and yesterday as such, and SHALL NOT move past today.

#### Scenario: Go to a previous day
- **WHEN** the user taps the previous-day control
- **THEN** Home shows that day's progress and meals, labeled "Yesterday" if it is the day before today, or with its date otherwise

#### Scenario: Cannot go past today
- **WHEN** Home is showing today
- **THEN** the next-day control is disabled

### Requirement: Meals on Home reflect changes
Home SHALL list the selected day's meals grouped by meal type, SHALL let the user delete a meal from the list, and SHALL update the progress after any change to that day's meals.

#### Scenario: Save a new meal
- **WHEN** the user captures, reviews and saves a meal
- **THEN** the app returns to Home on today, and the new meal and its calories and macros are reflected in the list and the progress

#### Scenario: Delete a meal
- **WHEN** the user deletes a meal from Home
- **THEN** the meal disappears from the list and the progress decreases accordingly

### Requirement: Default goal hint on Home
Home SHALL indicate when progress is being measured against the built-in default goal, and SHALL offer a way to set a personal goal from that indication.

#### Scenario: Goal never configured
- **WHEN** the user has never saved a goal
- **THEN** Home shows that the default goal is in use, with an action that opens the goal screen
