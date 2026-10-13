## Why

A photo alone can't tell the AI "no oil", "skim milk" or "half the plate", and portion size is the most common estimation error. For a small snack, taking a photo is more effort than saying what it was. Other apps let the user add a typed or spoken description, pick a portion size, or log a meal from the description alone. Tracked as GitHub issue #40: https://github.com/crmartinez930130/food-tracker-ai/issues/40

## What Changes

- **Backend `POST /api/meals/analyze`:**
  - **The photo becomes optional.** A request needs a photo, a description, or both. With neither it gets a 400.
  - **New optional fields:**
    - `description`: trimmed, at most 500 characters. Longer gets a 400 with a clear message; blank counts as absent.
    - `portion`: `small`, `medium` or `large`. Unknown values are ignored.
  - **Prompt:** the description is given to Claude as user-provided context, which overrides what a photo can't show. The portion steers quantities. Without a photo, the estimate comes from the description alone.
  - The response shape is unchanged.
- **App, Capture screen:**
  - **Description:** an optional field plus a **microphone** button that dictates into it, using the system speech recognizer in the app's language. The button is hidden when no recognizer is available.
  - **Portion:** optional chips for Small, Medium and Large, with none selected by default.
  - **"Analyze"** is enabled with a photo, a description or both. A description-only meal goes through Review and Save like any other.
  - With only a photo, nothing changes.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `meal-photo-analysis`: the photo becomes optional when a description is given, and the new description and portion inputs shape the estimate.
- `meal-capture`: the capture screen gains the description (typed or dictated), the portion choice, and analysis without a photo.

## Impact

- **Backend:**
  - `routes/meals.ts`: `/analyze` with an optional file, plus the new field parsing.
  - `services/vision-analysis.ts`: `analyzeMeal({ image?, description?, portion?, language })`. The photo-only call becomes a case of it. `analysisPrompt` gains the description, the portion and a text-only variant.
  - Tests for all of the above.
- **Frontend common:**
  - `FoodTrackerApiClient.analyzeMeal(photo?, description?, portion?, language)`.
  - `CaptureScreen` gains the description field, the portion chips and the new analyze condition.
  - New pure `canAnalyze(photo, description)`.
  - Strings in both languages.
  - The `expect` `rememberSpeechInput()`.
- **Frontend Android:**
  - A speech-input actual based on `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`, using an activity result.
  - Manifest `<queries>` for the recognize-speech intent, needed for package visibility on API 30+.
- **Frontend iOS:** a stub with no microphone.
- **Dependencies:** none.
