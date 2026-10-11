# Tasks

## 1. Back decisions

- [x] 1.1 Add `ui/BackNavigation.kt` with `captureBackAction(hasPhotoToConfirm, isAnalyzing)` → `IGNORE` (while analyzing)/`DISCARD_PHOTO`/`EXIT` and `reviewBackAction(isSaving, isDiscardPromptShown)` → `IGNORE`/`DISMISS_PROMPT`/`SHOW_PROMPT`; verify with `commonTest` `BackNavigationTest` covering every combination (saving wins over a shown prompt)

## 2. Screens

- [x] 2.1 `CaptureScreen`: `BackHandler` in the non-camera branch applying `captureBackAction` (clear the photo, or `onCancel`); verify it compiles
- [x] 2.2 `ReviewScreen`: discard `AlertDialog` ("Discard this meal?" / "Discard" in error color / "Keep editing", dismiss = keep editing), always-enabled `BackHandler` applying `reviewBackAction`, and "Cancel" opening the prompt instead of leaving; verify `:composeApp:assembleDebug` and `:composeApp:testDebugUnitTest` pass

## 3. End-to-end verification

- [x] 3.1 On the emulator with `adb shell input keyevent KEYCODE_BACK`: capture idle → Home; back during a real analysis is ignored (the analysis completes and opens Review); capture with a gallery photo → back discards it (photo options shown) → back → Home; camera viewfinder → back returns to capture (unchanged from #5); in no case does the app reach the launcher
- [x] 3.2 On the emulator, Review (opened from a favorite, so no API call): edit a value → back shows the prompt → back again closes it with the edit kept → "Cancel" shows the prompt → "Keep editing" keeps the edit → back → "Discard" returns to Home and no meal was created (API list unchanged). Clean up any test data afterwards
