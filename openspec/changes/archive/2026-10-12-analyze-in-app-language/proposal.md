## Why

With the app in Spanish (#34), a photo analysis still returns the food names and notes in English. Those texts are written by Claude, and the request never says which language to use. Separately, in Review the "Carbohidratos g" label doesn't fit its field and wraps onto two lines on an S24. Tracked as GitHub issue #37: https://github.com/crmartinez930130/food-tracker-ai/issues/37

## What Changes

- **Backend:** `POST /api/meals/analyze` accepts an optional multipart field `language` (`es` or `en`).
  - The analysis prompt asks Claude to write every item name and the notes in that language.
  - A missing or unknown value falls back to English. Such a request is never rejected.
  - Numbers, confidence values and the response shape don't change.
- **App:** the analyze request sends the language the app is showing.
- **Review:** the macro labels become short and uniform, so none wraps:
  - "Proteína (g)", "Grasa (g)", "Carbs (g)" in Spanish;
  - "Protein (g)", "Fat (g)", "Carbs (g)" in English.
- **Housekeeping:** this PR also archives `add-server-address-setting` and syncs its `app-settings` delta. That step was missed when #38 was merged.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `meal-photo-analysis`: photo submission accepts the language for the analysis texts, and the output's names and notes are written in it.

## Impact

- **Backend:**
  - `services/vision-analysis.ts` takes an `AnalysisLanguage` and builds the prompt with a language instruction.
  - `routes/meals.ts` reads and validates `req.body.language` from the multipart form.
  - Tests for both.
- **Frontend:**
  - `FoodTrackerApiClient.analyzeMealPhoto(bytes, language)` appends the form field.
  - `CaptureScreen` passes `LocalAppLocale.current`.
  - Review's label strings change.
  - Client test updated.
- **Dependencies:** none.
