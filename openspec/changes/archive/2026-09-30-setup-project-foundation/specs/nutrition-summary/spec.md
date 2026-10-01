# Spec Delta

## Purpose

Gives the user an at-a-glance view of total calories and macros consumed over a day or week, computed from their saved meal history.

## ADDED Requirements

### Requirement: Daily nutrition summary
The system SHALL compute and return total calories, protein, fat, and carbohydrates for all meal entries saved on a given date.

#### Scenario: Day with saved meals
- **WHEN** the client requests the daily summary for a date that has saved meal entries
- **THEN** the system returns totals that equal the sum of calories and macros across all entries for that date

#### Scenario: Day with no saved meals
- **WHEN** the client requests the daily summary for a date with no saved meal entries
- **THEN** the system returns zeroed totals rather than an error

### Requirement: Weekly nutrition summary
The system SHALL compute and return total and average-per-day calories and macros for a 7-day period starting from a given date.

#### Scenario: Week with meals on some days
- **WHEN** the client requests the weekly summary for a period where only some days have saved meals
- **THEN** the system returns totals summed across all entries in the period, and an average per day computed over all 7 days (days with no meals count as zero)

### Requirement: Summary reflects current data
The system SHALL compute summaries from the current state of saved meal entries at request time, not from a cached or precomputed value.

#### Scenario: Summary requested after an edit
- **WHEN** a meal entry is edited or deleted and the client then requests a summary covering that entry's date
- **THEN** the returned totals reflect the edit or deletion
