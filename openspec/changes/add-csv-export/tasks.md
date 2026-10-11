## 1. Backend: CSV formatting

- [x] 1.1 Create `services/csv-export.ts` with a pure `toCsv(rows)`: header, RFC 4180 quoting, CRLF, BOM, integer grams and kcal, 1-decimal macros, formula-injection prefix for `item`/`notes`, empty item columns for item-less rows. Verify with `services/csv-export.test.ts` covering each rule; round-trip parsing of quotes, commas and newlines; accents; and `=`/`+`/`-`/`@` prefixes.

## 2. Backend: query and endpoint

- [x] 2.1 Add `exportRowsBetween(db, start, endExclusive)` to `meal-repository.ts`: a LEFT JOIN of meals and items with SQLite local date/time, ordered by occurred_at, meal id and item id. Verify with repository tests: a multi-item meal, an item-less meal, a late-evening local date, ordering, and range bounds.
- [x] 2.2 Add `GET /api/meals/export` in `routes/meals.ts` above `/:id`, using `parseDateRange`, `text/csv; charset=utf-8` and `Content-Disposition` with the file name. Verify with route tests: 200 with the CSV for data, header-only for an empty range, and 400 for a missing date, an impossible date, a reversed range and more than 366 days.

## 3. Frontend: client and range helpers

- [x] 3.1 Add `exportMealsCsv(startDate, endDate): String` to `FoodTrackerApiClient`, with the backend error message surfaced via `ApiException`. Verify with `FoodTrackerApiClientTest` (MockEngine): the request path and query, the CSV body returned as-is, and a 400 mapped to the backend message.
- [x] 3.2 Add the pure helpers `exportFileName(start, end)`, `exportRangeError(start, end)` (max 366 days) and picker-millis ↔ `YYYY-MM-DD` conversion. Verify with `ExportRangeTest` in commonTest: the 366/367 boundary, a single day, and millis round-trips.

## 4. Frontend: sharing and UI

- [x] 4.1 Add `expect`/`actual` `rememberFileSharer()`. The Android version writes `cacheDir/exports/<name>`, shares it via `FileProvider` + `ACTION_SEND` chooser (`text/csv`, read grant); the iOS version is a stub. Also add the `<provider>` and `res/xml/file_paths.xml` to the manifest, and declare `androidx.core` explicitly. Verify with `./gradlew :composeApp:assembleDebug`.
- [x] 4.2 Add `ExportSection`/dialog on Summary: an "Export CSV" button opens `DateRangePicker` pre-selected with Summary's period and future days disabled; the confirm button is disabled with a message when `exportRangeError` returns one; it shows progress while exporting and the error text on failure. Verify with `./gradlew :composeApp:testDebugUnitTest assembleDebug`.

## 5. End-to-end verification

- [x] 5.1 On the emulator against the real backend, with seeded meals including a multi-item meal, accented names, notes with commas and quotes, and an item-less meal: export the week from Summary, check the share sheet opens with `food-tracker-<start>_<end>.csv`, and that the shared file's content parses correctly. Then try a range over 366 days (rejected) and the backend stopped (error shown). Clean up the seeded data.
- [x] 5.2 Open an exported CSV in a spreadsheet (LibreOffice Calc headless conversion or similar) and confirm the columns, accents and quoted notes come through intact.
