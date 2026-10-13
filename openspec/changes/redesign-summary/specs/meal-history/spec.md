## MODIFIED Requirements

### Requirement: Day detail
Tapping a day in History's calendar or list, or a bar of the Summary calorie chart, SHALL open a detail screen for that day. The detail SHALL show the day's written-out date, calories against the goal with the percentage badge, macros, water, and meals grouped by type, the same content Home shows for today. In the detail the user SHALL be able to:
- mark and unmark favorites;
- delete meals;
- log a glass of water;
- undo the day's last water entry.

A glass logged on a past day SHALL count toward that day. A back action, and the system back gesture, SHALL return to the screen the detail was opened from: History on the same month, or Summary on the same period.

#### Scenario: Open a past day
- **WHEN** the user taps yesterday in the calendar
- **THEN** the detail shows yesterday's date, totals, water and meals

#### Scenario: Log water on a past day
- **WHEN** the user adds a glass in the detail of a past day
- **THEN** that day's water total grows by one glass, and today's total is unchanged

#### Scenario: Correct a past day
- **WHEN** the user deletes a meal in a past day's detail and goes back
- **THEN** History shows the same month with that day's calories and status updated

#### Scenario: Back to Summary
- **WHEN** the user opens a day from a Summary chart bar and goes back
- **THEN** the app returns to Summary, not to History
