## MODIFIED Requirements

### Requirement: Per-day totals over a date range
The system SHALL return, for an inclusive date range, one entry per calendar day containing that day's date, its total calories, protein, fat and carbohydrates, its number of meals, and its total water in millilitres. The entries SHALL be ordered by date, and days with no meals SHALL appear with zero totals and a meal count of zero. Water SHALL be counted by the same local-day rule as meals, independently of whether the day has meals. The system SHALL reject a malformed date, a range whose end is before its start, and a range longer than 366 days.

#### Scenario: Range with meals on every day
- **WHEN** the client requests a range in which every day has saved meals
- **THEN** the system returns one entry per day, in date order, each with the sum of that day's meals

#### Scenario: Range with empty days
- **WHEN** the client requests a range in which some days have no meals
- **THEN** those days are included with zero totals rather than omitted, and the number of entries equals the number of days in the range

#### Scenario: Single-day range
- **WHEN** the client requests a range whose start and end are the same date
- **THEN** the system returns exactly one entry for that date

#### Scenario: Invalid range
- **WHEN** the client requests a range with a malformed date, an end before the start, or more than 366 days
- **THEN** the system responds with a 400-style validation error

#### Scenario: Meal count and water
- **WHEN** the client requests a range in which one day has two meals and 750 ml of water, and another has only 500 ml of water
- **THEN** the first day's entry has a meal count of 2 and 750 ml, and the second has a meal count of 0, zero calories and 500 ml
