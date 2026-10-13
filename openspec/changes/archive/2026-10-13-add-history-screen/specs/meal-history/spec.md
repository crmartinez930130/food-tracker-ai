## Purpose

Lets the user review earlier days: a month calendar colored by how each day went against the calorie goal, a list of the days that have records, and a detail screen to see and correct any day.

## ADDED Requirements

### Requirement: History tab
The app SHALL offer a History destination in the main navigation, between Home and Summary. History SHALL show one calendar month at a time, starting on the current month.

#### Scenario: Open History
- **WHEN** the user taps History in the bottom navigation
- **THEN** the app shows the current month's calendar and the list of that month's days with records

### Requirement: Month calendar colored by calorie status
History SHALL show the month as a grid of weeks starting on Monday, with the month name and year. Each day SHALL be marked with one of four statuses against the current daily calorie goal, shown with a legend:
- no meals logged;
- under: below 90% of the goal;
- on target: from 90% to 100% of the goal;
- over: above the goal.

Today SHALL be visually marked. Days after today SHALL be shown as unavailable and SHALL NOT be selectable.

#### Scenario: Statuses
- **WHEN** with a goal of 2000 kcal, one day has no meals, one has 1500 kcal, one 1900 kcal and one 2100 kcal
- **THEN** the calendar marks them as no meals, under, on target and over respectively

#### Scenario: Only water
- **WHEN** a day has water entries but no meals
- **THEN** the calendar marks it as no meals

#### Scenario: Future days
- **WHEN** the user views the current month
- **THEN** the days after today are shown as unavailable and tapping them does nothing

### Requirement: Month navigation
History SHALL let the user move to the previous and the next month. It SHALL NOT move past the current month.

#### Scenario: Previous month
- **WHEN** the user taps the previous-month control
- **THEN** History shows the previous month's calendar and its days with records

#### Scenario: Cannot reach future months
- **WHEN** History shows the current month
- **THEN** the next-month control is disabled

### Requirement: List of days with records
Below the calendar, History SHALL list the shown month's days that have at least one meal or water entry, newest first. Each day SHALL show its date, its calories against the goal, its macro composition as a small bar, and its water total. A month without records SHALL show a message instead of an empty list.

#### Scenario: Days with meals or water
- **WHEN** the month has a day with meals, a day with only water, and days with nothing
- **THEN** the list shows the first two, newest first, and omits the rest

#### Scenario: Empty month
- **WHEN** the shown month has no meals and no water
- **THEN** History shows the calendar and a message that nothing was logged that month

### Requirement: Day detail
Tapping a day, in the calendar or in the list, SHALL open a detail screen for that day. The detail SHALL show the day's written-out date, calories against the goal with the percentage badge, macros, water, and meals grouped by type, the same content Home shows for today. In the detail the user SHALL be able to:
- mark and unmark favorites;
- delete meals;
- log a glass of water;
- undo the day's last water entry.

A glass logged on a past day SHALL count toward that day. A back action, and the system back gesture, SHALL return to History on the same month.

#### Scenario: Open a past day
- **WHEN** the user taps yesterday in the calendar
- **THEN** the detail shows yesterday's date, totals, water and meals

#### Scenario: Log water on a past day
- **WHEN** the user adds a glass in the detail of a past day
- **THEN** that day's water total grows by one glass, and today's total is unchanged

#### Scenario: Correct a past day
- **WHEN** the user deletes a meal in a past day's detail and goes back
- **THEN** History shows the same month with that day's calories and status updated
