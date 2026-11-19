## MODIFIED Requirements

### Requirement: CSV layout
The CSV SHALL start with the header row `date,time,meal_type,item,grams,calories,protein_g,fat_g,carbs_g,notes`. It SHALL contain one row per meal item, in chronological order of the meals.
- `date` and `time` SHALL be the meal's local calendar day (`YYYY-MM-DD`) and local time (`HH:MM`), the same day the meal is listed under in the app.
- `meal_type` SHALL be the meal's type (`breakfast`, `pre_workout`, `lunch`, `dinner` or `snack`), or empty when it has none.
- `notes` SHALL repeat the meal's notes on each of its item rows.
- A meal with no items SHALL produce exactly one row, with the item columns empty.
- Grams and calories SHALL be whole numbers and macros SHALL have at most one decimal, using `.` as the decimal separator.

#### Scenario: Meal with several items
- **WHEN** a lunch with two items is exported
- **THEN** the CSV has two rows for it, each with that meal's date, time, `lunch` and notes, and each item's own name and nutrition values

#### Scenario: Pre-workout meal
- **WHEN** a pre-workout meal is exported
- **THEN** its rows have `pre_workout` in the `meal_type` column

#### Scenario: Meal without items
- **WHEN** a meal with no items is exported
- **THEN** the CSV has one row for it, with date, time, meal type and notes filled and the item columns empty

#### Scenario: Meal logged late in the evening
- **WHEN** a meal occurred at 22:30 local time, which is already the next day in UTC
- **THEN** its row shows the local date and `22:30`
