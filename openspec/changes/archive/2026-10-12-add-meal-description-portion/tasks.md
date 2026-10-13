## 1. Backend

- [x] 1.1 In `services/vision-analysis.ts`, add `AnalysisPortion`, `parseAnalysisPortion`, and an `analysisPrompt({ language, description, portion, hasImage })` with the text-only variant. Replace `analyzeMealPhoto` with `analyzeMeal(client, input, model)`. Verify with `vision-analysis.test.ts`:
  - the prompt includes the delimited description, the portion sentence, and the text-only wording when there is no image;
  - there is no image part when the image is absent;
  - the image part plus the prompt are sent when there is an image;
  - the portion parse handles small, medium, large and garbage;
  - calling with neither an image nor a description throws.
- [x] 1.2 Make the photo optional in `routes/meals.ts` `/analyze`, and validate the description (trim; more than 500 characters → 400; blank counts as absent), the portion (allow-list) and photo-or-description (else 400). Verify with route tests:
  - photo only works as before;
  - photo plus description plus portion reach the prompt;
  - a description alone is accepted with no image part;
  - neither, or a blank description only, gives 400 with no call to the service;
  - 501 characters gives 400 and 500 is OK;
  - an unknown portion is ignored;
  - the unsupported file type 400 still applies.

## 2. Frontend data and logic

- [x] 2.1 Change the client to `analyzeMeal(photo?, description?, portion?, language)` with a `Portion` enum, and add the pure `canAnalyze(photo, description)` helper. Verify with `FoodTrackerApiClientTest` (multipart parts present or absent: a text-only request has no `photo` part; a portion is sent as `small`) and a `CaptureLogicTest` for `canAnalyze`.

## 3. Frontend UI and speech

- [x] 3.1 Add the `expect`/`actual` `rememberSpeechInput(language, onResult)`. The Android version uses `RecognizerIntent` with an activity result and availability via `queryIntentActivities`; the iOS version is a null stub. Add the manifest `<queries>` for `RECOGNIZE_SPEECH`. Verify with `./gradlew :composeApp:assembleDebug`.
- [x] 3.2 Update `CaptureScreen`:
  - the description field with the microphone, capped at 500 characters;
  - the portion filter chips, with toggle-to-clear;
  - a full-width Analyze button using `canAnalyze`;
  - favorites only when there is no photo and no description;
  - the generic analyze error;
  - strings in both languages.

  Verify with `StringResourcesTest` and `./gradlew :composeApp:testDebugUnitTest assembleDebug`.

## 4. Verification

- [x] 4.1 With real analyses against the real backend:
  - **Photo only:** works as before.
  - **Same photo with context:** with "sin aceite" plus Small it returns fewer calories than without context.
  - **Description only:** "una manzana y un yogur bebible chico", with no photo, goes through Review and can be saved.
  - **No input:** Analyze is disabled with neither a photo nor a description.
  - **Dictation:** works on the S24 (or on the emulator if it has a recognizer), in Spanish.

  Clean up any saved test meals.
