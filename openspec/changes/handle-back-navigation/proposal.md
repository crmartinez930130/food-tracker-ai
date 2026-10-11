## Why

`CaptureScreen` and `ReviewScreen` don't handle the system back button or gesture, so Android finishes the activity and the app drops to the launcher. On Review that throws away an unsaved analysis, which cost an API call, along with any edits. It already happened twice during emulator verification, in #3 and #6. Goals (#3) and the camera (#5) already handle back. These two screens are the remaining gap. Tracked as GitHub issue #23: https://github.com/crmartinez930130/food-tracker-ai/issues/23

## What Changes

- **Capture screen, system back:**
  - With a photo awaiting confirmation, it discards the photo and returns to the photo options and favorites.
  - Otherwise it returns to Home, the same as "Cancel".
  - The in-app camera keeps its own back handling from #5.
- **Review screen, system back and the "Cancel" button:** both ask "Discard this meal?" before leaving.
  - "Discard" returns to Home without saving.
  - "Keep editing" closes the dialog and leaves the analysis and edits untouched.
  - While a save is in progress, back is ignored.
- The back decisions live in small pure functions with unit tests. The Compose wiring is verified on the emulator.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `meal-capture`: adds a requirement for leaving the logging flow with system back or cancel, without losing an unsaved analysis by accident and without exiting the app.

## Impact

- **Frontend only:**
  - Changes to `ui/CaptureScreen.kt` and `ui/ReviewScreen.kt`.
  - A new `ui/BackNavigation.kt` with the pure decision functions, plus its `commonTest`.
- **Dependencies:** none new. It uses `ui-backhandler`, which was already declared in #3, and the Material 3 `AlertDialog`.
- **Backend/API:** none.
