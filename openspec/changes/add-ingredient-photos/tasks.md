## 1. Backend

- [x] 1.1 Route: `upload.fields` with `photo` (1) and `ingredients` (5), the 40 MB total check, the content check on every file, and "something to analyze" counting ingredients; preprocess all images (D1). Verify with `test/routes/meals.test.ts`: meal and ingredients reach the analysis with their count; ingredients only plus description work; six ingredients get 400 without calling the model; a non-image ingredient gets 400; over 40 MB in total gets 400; nothing to analyze still gets 400.
- [x] 1.2 `analyzeMeal` and the prompt: label/image pairs in order, `INGREDIENTS_INSTRUCTION`, the text base without a meal photo, and the ingredient count in the log (D2). Verify with tests of `analysisPrompt` and of the content sent to a fake client (order, labels, prompt last).

## 2. App: sending and offline storage

- [x] 2.1 `FoodTrackerApiClient.analyzeMeal` sends `ingredients` parts; add `downscaledJpeg` (`expect`/`actual`: Android with EXIF rotation, iOS unchanged) (D3). Verify with a `FoodTrackerApiClientTest` case for the multipart parts, and on the emulator for the downscaler.
- [x] 2.2 `capture_queue.ingredient_files` with Room version 5 (auto-migration) and `4.json` → `5.json`; `CaptureQueueRepository` keeps, sends, checks and deletes the ingredient files (D5). Verify with `CaptureQueueRepositoryTest`: kept with its ingredient files; analyzed with them; a missing ingredient file fails the row; discard and complete delete them all.

## 3. App: Capture

- [x] 3.1 The ingredients section, camera target, multi-select gallery picker (single when one slot remains), removal, the 5-photo cap, `canAnalyze` with ingredients, and downscaling every added photo (D3, D4); strings in Spanish and English. Verify with `CaptureLogicTest`/`BackNavigationTest` for `canAnalyze`, `StringResourcesTest`, and the build.

## 4. Verification

- [x] 4.1 On the emulator, installed over the current build (database version 4), with the backend from this branch over a copy of the real database and the real API key:
  - add a meal photo and ingredient photos from the camera and from the gallery (several at once), remove one, and check that the add actions disappear at 5;
  - one real analysis with a nutrition label as an ingredient: Review shows the analysis, and the backend log shows the ingredient count;
  - ingredients only plus a description: analyzed;
  - without a connection: a capture with ingredients is kept with all its files, analyzed when the backend is back, and saving deletes them;
  - `curl`: six ingredients and a non-image ingredient both get 400.

  Restore the app's settings and delete test data afterwards.
