## Why

The data lives only inside the app and its SQLite file. Exporting a CSV lets the user analyze it in a spreadsheet (Excel/Sheets) and keep a backup they own. Every reference app offers it. Tracked as GitHub issue #9: https://github.com/crmartinez930130/food-tracker-ai/issues/9

## What Changes

- **Backend:** new `GET /api/meals/export?startDate=&endDate=`, which returns `text/csv` as a download:
  - **One row per meal item**, not per meal, so each row can be summed or filtered in a spreadsheet. A meal saved without items still gets one row, with the item columns empty, so it isn't silently dropped.
  - **Columns:** `date`, `time`, `meal_type`, `item`, `grams`, `calories`, `protein_g`, `fat_g`, `carbs_g`, `notes`. `meal_type` is included because #4 is already implemented.
  - **Date and time** are the meal's local calendar day and time, matching how Home and Summary group meals.
  - **Range validation** is the same as the other range endpoints: real dates, an inclusive range, and at most 366 days.
  - **A range without meals** returns a CSV with only the header row, not an error.
  - **Formatting:** RFC 4180 escaping, UTF-8 with a BOM so Excel shows accents correctly, and rows in chronological order.
- **App:** an **"Export CSV"** button on Summary:
  - It opens a date-range picker, pre-selected with the period Summary is showing. Future days can't be picked, and ranges over 366 days are rejected with a message.
  - The app downloads the CSV and hands it to the Android **share sheet** (`ACTION_SEND`) to send, save to Drive and so on. The file is never saved silently.
  - Errors, such as the backend being unreachable, are shown on screen.

## Capabilities

### New Capabilities
- `data-export`: exporting the meal history as a CSV file, both the API and the app's export-and-share flow.

### Modified Capabilities
<!-- None. The existing meal and summary endpoints are unchanged. -->

## Impact

- **Backend:**
  - New `services/csv-export.ts`, holding the pure CSV formatting (the row type lives in `domain/meal.ts`).
  - New repository query for the meals and items in a range, carrying their local date and time.
  - New route in `routes/meals.ts`, registered above `/:id`.
  - Tests for all of the above.
- **Frontend:**
  - New `exportMealsCsv(startDate, endDate)` client method.
  - New `ui/summary/ExportSection.kt` (the button and the date-range dialog) and `ui/summary/ExportRange.kt` (pure range, file-name and picker-date helpers).
  - An `expect`/`actual` file sharer. The Android version writes to the cache directory and shares it through a `FileProvider`; the iOS version is a stub.
  - Manifest: a `<provider>` entry plus `res/xml/file_paths.xml`.
- **Dependencies:** `androidx.core` (for `FileProvider`) is declared explicitly. It is already on the classpath transitively through activity-compose.
