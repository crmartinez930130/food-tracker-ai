## Context

- Meals store `occurred_at` as a UTC ISO timestamp. Day grouping uses SQLite's `date(occurred_at, 'localtime')`, because the server's timezone is the user's: this is a single-user, single-machine app.
- Range endpoints validate with `parseDateRange`: real dates, an inclusive range, at most 366 days.
- Summary shows a rolling Week (7 days) or Month (30 days) window ending on `endDate`.
- The app is Kotlin Multiplatform, and Android is the only real target. Platform features such as the camera and the image picker are `expect`/`actual` composables that return a launcher.

## Goals / Non-Goals

**Goals:**
- A CSV that opens cleanly in Excel and Google Sheets.
- One code path for formatting, unit-testable without HTTP.

**Non-Goals:**
- Exporting weight, goals or totals. This change covers meals only; other data can get its own columns or files later.
- Importing CSV.
- "All history" exports beyond 366 days. The user can export year by year.
- Scheduled or automatic exports.

## Decisions

### D1. Build the CSV in the backend, not the app
- **Choice:** the backend already has the data and the local-time rules, and the CSV is equally useful from `curl`.
- **Alternative:** have the app fetch meals per day and build the CSV itself. Rejected: that means N requests, and the timezone logic would be duplicated in the client.

### D2. A separate route under `/api/meals`
- The route is `GET /api/meals/export`. It must be registered before `/:id`, the same way `/favorites` is.
- It reuses `parseDateRange` for validation.
- Response headers:
  - `Content-Type: text/csv; charset=utf-8`
  - `Content-Disposition: attachment; filename="food-tracker-<start>_<end>.csv"`

### D3. Local date and time come from SQLite
- The query selects `date(occurred_at,'localtime')` and `strftime('%H:%M', occurred_at, 'localtime')` next to each row. The CSV's date therefore always matches the day the meal shows under on Home.
- Rows are ordered by `occurred_at`, then meal id, then item id.
- A `LEFT JOIN` on items gives a meal without items exactly one row, with NULL item columns.

### D4. Formatting rules (pure `toCsv(rows)`)
- **Fields:** a field is quoted when it contains `,`, `"`, CR or LF, and inner quotes are doubled (RFC 4180). Lines end in CRLF.
- **Numbers:**
  - grams and calories are rounded to integers, matching the app's display;
  - macros are rounded to 1 decimal;
  - the decimal separator is always `.`, which is standard CSV. Locale-specific separators are left to the spreadsheet's import.
- **BOM:** the output starts with a UTF-8 BOM (`﻿`). Without it, Excel opens UTF-8 as ANSI and mangles accents ("Ñoquis", "jamón").
- **`meal_type`:** exported as the API value (`breakfast`, `lunch`, `dinner`, `snack`), or empty when unset.
- **Formula injection:** text fields (`item`, `notes`) that start with `=`, `+`, `-`, `@`, tab or CR get a leading `'`, as OWASP recommends. Item names come from the model's analysis, and both fields can be edited. Without this, a value such as `=HYPERLINK(...)` would run as a formula when opened. Numeric columns are not affected.

### D5. Range picker: Material 3 `DateRangePicker` in a dialog
- **Initial selection:** Summary's current `startDate` to `endDate`, the most likely intent.
- **Selectable dates:** `SelectableDates` disables dates after today.
- **Range limit:** checked by a pure helper `exportRangeError(start, end)`, which allows at most 366 days. The Export button is disabled and the reason is shown, so the backend's 400 never shows up as the user-facing error.
- **Date conversion:** between the picker's UTC millis and `YYYY-MM-DD`, using kotlinx-datetime `LocalDate` and `LocalDate.fromEpochDays`.
- **Alternative:** two separate date fields. Rejected as clunkier; the range picker is the standard M3 component.

### D6. Sharing: cache file + `FileProvider` + `ACTION_SEND`
- **Common code:** `expect @Composable fun rememberFileSharer(): FileSharer`, with `share(fileName, mimeType, content)`.
- **Android:**
  - writes the file to `cacheDir/exports/<fileName>` (overwritten each time, never on public storage);
  - gets a `content://` URI from a `FileProvider` (authority `${applicationId}.fileprovider`);
  - starts `Intent.createChooser(ACTION_SEND)` with `EXTRA_STREAM`, `text/csv` and `FLAG_GRANT_READ_URI_PERMISSION`.
- **iOS:** a stub, consistent with the other iOS actuals.
- **Why not `EXTRA_TEXT`?** It would paste the CSV as message text, which is not a file the target apps can save.

### D7. Client method returns the CSV text
- `exportMealsCsv(start, end): String` returns the raw body.
- A non-2xx response goes through the same error-body path as the other calls, so `ApiException` carries the backend's message.
- The file name is built in the app from the dates, with the same pattern as the backend's header.

## Risks / Trade-offs

- **Dot decimals in a Spanish-locale spreadsheet.** Excel set to a `,` decimal locale may read `12.5` as text when the file is opened directly. → Mitigation: this is standard CSV, and Sheets and Excel's "Import from text" let the user pick the locale. A locale-aware export can come later if it turns out to matter.
- **Large exports.** A year of meals is a few thousand rows, a few hundred KB, held in memory on both ends. → Acceptable for a single-user app; the 366-day cap bounds it.
- **Leftover files in `cacheDir`.** → Each export overwrites the same name per range, the cache is cleared by the OS, and files are never public.

## Migration Plan

None. This change adds a new endpoint and a new UI only, with no schema change.
