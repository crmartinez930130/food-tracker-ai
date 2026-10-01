# Tasks

## 1. Monorepo scaffolding

- [x] 1.1 Create root `.gitignore` (node_modules, `backend/data/*.sqlite`, `backend/.env`, Gradle/Xcode build output) and verify `git status` shows no build artifacts or secrets after a full build
- [x] 1.2 Create `backend/` as a Node.js + TypeScript project (`package.json`, `tsconfig.json`) and verify `npm run build` (or `tsc --noEmit`) succeeds on an empty entrypoint
- [x] 1.3 Create `frontend/` as a Kotlin Multiplatform + Compose Multiplatform project (`composeApp` module, `androidMain`/`commonMain`/`iosMain` source sets) and verify the Android target builds with `./gradlew :composeApp:assembleDebug`
- [x] 1.4 Write root `README.md` covering how to run the backend locally, set `ANTHROPIC_API_KEY`, and point the Android app at the backend's LAN address

## 2. Backend: database

- [x] 2.1 Add `drizzle-orm`, `better-sqlite3`, `drizzle-kit` and define the schema (`meals`, `meal_items` tables) in `backend/src/db/schema.ts`; verify `drizzle-kit generate` produces a migration file with no errors
- [x] 2.2 Wire migration execution on backend startup against `backend/data/food-tracker.sqlite` and verify the sqlite file is created with the expected tables (`sqlite3 backend/data/food-tracker.sqlite ".tables"`)
- [x] 2.3 Implement a data-access module (create/list/get/update/delete meal entries) and verify with unit tests covering each operation against an in-memory or temp sqlite file

## 3. Backend: Claude vision analysis

- [x] 3.1 Add `@anthropic-ai/sdk`, `zod`, `sharp`; define `MealAnalysisSchema` (Zod) per design.md and verify it compiles and matches the documented shape
- [x] 3.2 Implement image preprocessing (resize to ≤~1568px long edge, JPEG re-encode) and verify with a unit test asserting output dimensions/format on a sample oversized image
- [x] 3.3 Implement the vision-analysis service calling `client.messages.parse()` with the schema and the model read from `ANTHROPIC_MODEL` (default `claude-opus-5`), and verify with an integration test (real or recorded API call) that a sample food photo returns a parsed, schema-valid result
- [x] 3.4 Handle analysis failures (API error, schema validation failure) by surfacing a clear error without throwing an unhandled exception, and verify with a test that simulates an API error and asserts a clean error response
- [x] 3.5 Log `usage.input_tokens`/`usage.output_tokens` per analysis call and verify the log line appears for a successful analysis

## 4. Backend: REST API

- [x] 4.1 Implement `POST /api/meals/analyze` (multipart upload → preprocessing → vision analysis → response, no persistence) satisfying the `meal-photo-analysis` spec, and verify with an integration test covering a valid image, an unsupported file type, and a no-food-detected case
- [x] 4.2 Implement `POST /api/meals` (confirm + save, with optional corrections and optional `occurredAt`) satisfying the `meal-tracking` spec's save requirement, and verify with a test that a saved entry reflects corrected values when provided
- [x] 4.3 Implement `GET /api/meals`, `GET /api/meals/:id`, `PATCH /api/meals/:id`, `DELETE /api/meals/:id` satisfying the remaining `meal-tracking` spec requirements, and verify with tests covering empty-day listing, 404 on unknown id, edit reflected on re-read, and delete removing the entry from listings
- [x] 4.4 Implement `GET /api/summary/daily` and `GET /api/summary/weekly` satisfying the `nutrition-summary` spec, and verify with tests covering a day/week with meals, a day/week with none (zeroed totals), and a summary reflecting a just-made edit
- [x] 4.5 Bind the server to `0.0.0.0` on a configurable port and verify the API is reachable from another device on the same LAN by IP address

## 5. Frontend: networking and data layer

- [x] 5.1 Add an HTTP client (e.g. Ktor client) to `commonMain` and define request/response models matching the backend API contract; verify with a unit test that a sample JSON response deserializes correctly
- [x] 5.2 Add a configurable backend base URL (not hardcoded to `localhost`) and verify it can be changed without a code change (build config or in-app setting)

## 6. Frontend: screens (Android)

- [x] 6.1 Implement the photo capture/selection screen (camera or gallery picker) and verify by running the app and confirming a photo can be captured or selected and uploaded to `/api/meals/analyze`
- [x] 6.2 Implement the review/correction screen showing the analysis result with editable food items, quantities, and macros, and verify by manually editing a value and confirming it is what gets sent to `POST /api/meals`
- [x] 6.3 Implement the daily history screen (list of saved meals for a date) and verify it shows entries created via `POST /api/meals`
- [x] 6.4 Implement the daily/weekly summary screen and verify totals shown match the backend's `/api/summary/*` responses for the same data

## 7. End-to-end verification

- [x] 7.1 Run the full flow on a physical Android device against the backend on the local network — capture a real meal photo, review/correct, save, and confirm it appears in history and in the daily summary
