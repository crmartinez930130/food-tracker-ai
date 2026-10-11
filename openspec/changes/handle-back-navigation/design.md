# Design

## Context

`AppState` routes between `Screen.Main`, `Screen.Capture`, `Screen.Review` and `Screen.Goals` with no navigation library. Two screens already handle system back:
- `GoalsScreen` (#3) uses the multiplatform `BackHandler` from `androidx.compose.ui.backhandler`, declared explicitly, with `@OptIn(ExperimentalComposeUiApi::class)`.
- `CameraCapture`'s Android `actual` (#5) uses activity-compose's `BackHandler`.

`CaptureScreen` holds the camera as an in-place mode (`showCamera`) plus the `photo` awaiting confirmation. `ReviewScreen` has a "Cancel" `OutlinedButton` that calls `onCancel` immediately, and an `isSaving` flag.

The project has no Compose UI test harness that runs in CI. Android instrumented tests would need an emulator in CI. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- System back never exits the app from Capture or Review.
- An unsaved analysis can't be lost with a single accidental tap or back.
- The back decisions are unit-tested even without UI tests.

**Non-Goals:**
- Predictive-back animations.
- Confirmation on leaving Goals with unsaved edits.
- Preserving the review across process death.

## Decisions

**1. Pure decision functions, tested in `commonTest`; thin Compose wiring.**
`ui/BackNavigation.kt` holds:
- `captureBackAction(hasPhotoToConfirm: Boolean, isAnalyzing: Boolean): CaptureBack`, which returns `IGNORE`, `DISCARD_PHOTO` or `EXIT`. `IGNORE` takes precedence while analyzing. *Correction from the PR review:* without it, back during an analysis cleared the photo while the in-flight request still went on to open Review with the "discarded" photo, or showed its error on the reset screen. This mirrors `IGNORE` while saving in Review.
- `reviewBackAction(isSaving: Boolean, isDiscardPromptShown: Boolean): ReviewBack`, which returns `IGNORE`, `DISMISS_PROMPT` or `SHOW_PROMPT`. `IGNORE` takes precedence while saving.

The screens map these results to state changes. The functions are trivial on purpose: they turn every spec scenario into a deterministic unit test, and the emulator check covers the wiring.
- *Alternative:* add Compose UI tests (`runComposeUiTest`). They need a desktop/JVM target or instrumented tests, and this project has neither in CI. Setting that up is its own piece of work.

**2. The multiplatform `BackHandler`, the same as `GoalsScreen`.**
- In `CaptureScreen` it is composed only in the non-camera branch, after the early `return` for `showCamera`. The camera's own handler therefore stays the only active one while the viewfinder is shown, with no precedence games.
- In `ReviewScreen` it is always enabled while the screen is shown, so back never falls through to the activity. Its action follows `reviewBackAction`. Leaving it enabled while saving and returning `IGNORE` matters: if it were disabled, back would reach the activity and close the app mid-save.

**3. The discard prompt is a Material 3 `AlertDialog`.**
- Title: "Discard this meal?"
- Text: "The analysis and your edits will be lost."
- Confirm button: "Discard", using the `error` color.
- Dismiss button: "Keep editing".
- `onDismissRequest`, raised by tapping outside or by back while the dialog is shown, behaves like "Keep editing".

The "Cancel" button opens the same prompt instead of calling `onCancel` directly. Only "Discard" calls `onCancel`, so `AppState` and the route change stay exactly as before.

## Risks / Trade-offs

- **[Logic is tested, wiring isn't automated]** → The wiring is a handful of lines per screen, and the emulator check covers every scenario with `KEYCODE_BACK`. A future issue can add UI tests once there's a target for them.
- **[`BackHandler` in CMP 1.9 is experimental and deprecated in 1.10]** → It is already used in `GoalsScreen`, so the migration to `NavigationBackHandler` will be a single coordinated change for all three screens.
