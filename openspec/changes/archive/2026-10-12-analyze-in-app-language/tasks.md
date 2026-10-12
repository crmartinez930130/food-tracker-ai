## 0. Housekeeping

- [x] 0.1 Archive `add-server-address-setting` and sync its `app-settings` delta, which was missed when #38 was merged. Verify with `openspec validate --specs`.

## 1. Backend

- [x] 1.1 In `services/vision-analysis.ts`, add `AnalysisLanguage` (`"es" | "en"`), `parseAnalysisLanguage(value)` (allow-list, case-insensitive, `en` default) and the pure `analysisPrompt(language)`. Have `analyzeMealPhoto` accept the language. Verify with `vision-analysis.test.ts`:
  - the prompt contains the Spanish or English instruction;
  - the text sent to the mocked client is the right prompt;
  - parse handles `es`/`ES`/`en`, missing, and `fr`/garbage → `en`.
- [x] 1.2 In `routes/meals.ts` `/analyze`, pass `parseAnalysisLanguage(req.body.language)`. Verify with route tests: a multipart `language=es` reaches the service with the Spanish prompt; missing and invalid values still return 200 and use English.

## 2. Frontend

- [x] 2.1 Have `analyzeMealPhoto(bytes, language)` append the `language` form field, and `CaptureScreen` pass `LocalAppLocale.current`. Update the Review macro labels in both languages. Verify with a `FoodTrackerApiClientTest` case asserting the multipart body contains `language` = `es`, `StringResourcesTest`, and `./gradlew :composeApp:testDebugUnitTest assembleDebug`.

## 3. End-to-end verification

- [x] 3.1 On the emulator against the real backend (the S24 was locked at the time, so the user asked for the emulator), with the app in Spanish, analyze a photo: names and notes come back in Spanish, and the Review macro labels each fit on one line. Switch to English and check the labels in Review. Clean up any test meals.
