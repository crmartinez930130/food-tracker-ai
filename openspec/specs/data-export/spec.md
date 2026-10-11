# data-export Specification

## Purpose

Lets the user take their meal history out of the app as a CSV file, to analyze it in a spreadsheet or keep their own backup.

## Requirements

### Requirement: CSV export endpoint
The backend SHALL expose `GET /api/meals/export?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`. It SHALL return the meals whose local calendar day falls within the inclusive range as a CSV file, with content type `text/csv; charset=utf-8` and a `Content-Disposition: attachment` header whose file name contains both dates. The range SHALL be validated like the other range endpoints: both dates SHALL be real calendar dates, `endDate` SHALL NOT be before `startDate`, and the range SHALL NOT exceed 366 days. Otherwise the response SHALL be 400 with an error message.

#### Scenario: Range with meals
- **WHEN** the client requests an export for a range containing meals
- **THEN** the response is 200 with a CSV body and an attachment file name like `food-tracker-2026-10-01_2026-10-07.csv`

#### Scenario: Range without meals
- **WHEN** the client requests an export for a valid range with no meals
- **THEN** the response is 200 with a CSV containing only the header row

#### Scenario: Invalid range
- **WHEN** the client omits a date, sends an impossible date such as `2026-02-30`, sends an end before the start, or sends a range longer than 366 days
- **THEN** the response is 400 with an error message

### Requirement: CSV layout
The CSV SHALL start with the header row `date,time,meal_type,item,grams,calories,protein_g,fat_g,carbs_g,notes`. It SHALL contain one row per meal item, in chronological order of the meals.
- `date` and `time` SHALL be the meal's local calendar day (`YYYY-MM-DD`) and local time (`HH:MM`), the same day the meal is listed under in the app.
- `meal_type` SHALL be the meal's type (`breakfast`, `lunch`, `dinner` or `snack`), or empty when it has none.
- `notes` SHALL repeat the meal's notes on each of its item rows.
- A meal with no items SHALL produce exactly one row, with the item columns empty.
- Grams and calories SHALL be whole numbers and macros SHALL have at most one decimal, using `.` as the decimal separator.

#### Scenario: Meal with several items
- **WHEN** a lunch with two items is exported
- **THEN** the CSV has two rows for it, each with that meal's date, time, `lunch` and notes, and each item's own name and nutrition values

#### Scenario: Meal without items
- **WHEN** a meal with no items is exported
- **THEN** the CSV has one row for it, with date, time, meal type and notes filled and the item columns empty

#### Scenario: Meal logged late in the evening
- **WHEN** a meal occurred at 22:30 local time, which is already the next day in UTC
- **THEN** its row shows the local date and `22:30`

### Requirement: CSV is safe to open in a spreadsheet
The CSV SHALL follow RFC 4180 quoting: a field containing a comma, a double quote or a line break SHALL be enclosed in double quotes, with inner double quotes doubled. Lines SHALL end in CRLF. The file SHALL be UTF-8 and begin with a byte-order mark, so spreadsheets detect the encoding. Text fields (`item`, `notes`) whose value starts with `=`, `+`, `-`, `@`, a tab or a carriage return SHALL be prefixed with a single quote, so a spreadsheet does not evaluate them as formulas.

#### Scenario: Notes with commas, quotes and line breaks
- **WHEN** a meal's notes are `with "salsa", extra` followed by a line break and more text
- **THEN** the field is quoted with the inner quotes doubled, and parsing the CSV yields the original text

#### Scenario: Accented item names
- **WHEN** an item is named `Ñoquis con jamón`
- **THEN** the file is UTF-8 with a BOM and the name appears unchanged

#### Scenario: Text that looks like a formula
- **WHEN** an item name or note starts with `=`
- **THEN** the exported field starts with `'=`, and numeric columns are never altered

### Requirement: Export from the app
The Summary screen SHALL offer an export action that opens a date-range picker pre-selected with the period Summary is currently showing. Days after today SHALL NOT be selectable. A selected range longer than 366 days SHALL be rejected with a message before any request is made. Confirming SHALL download the CSV for the chosen range and hand it as a file to the platform's share sheet (on Android, a share intent with a `text/csv` attachment), instead of saving it silently. A failed export SHALL show an error on the screen.

#### Scenario: Export the current week
- **WHEN** the user is on Summary showing Week, taps export and confirms the pre-selected range
- **THEN** the share sheet opens with a CSV file named after that week's dates

#### Scenario: Range too long
- **WHEN** the user picks a range longer than 366 days
- **THEN** the dialog explains the limit and the export cannot be confirmed

#### Scenario: Backend unreachable
- **WHEN** the export request fails
- **THEN** Summary shows an error message and no share sheet opens
