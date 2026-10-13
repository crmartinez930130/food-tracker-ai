## Why

This is the fourth redesign issue inspired by the Material 3 Expressive samples (https://m3e.c5inco.dev/). Logging a meal is what the app is used for most. Capture and Review still have the pre-redesign layout:
- stacked plain buttons;
- portion filter chips;
- a favorites list with dividers;
- a "[Snack]"-with-brackets meal type picker;
- raw text fields per food;
- a one-line text total;
- save and cancel buttons at the bottom of a long list.

The references are Dollop (product detail and the fixed total pill) and Pinhole (camera screen). Tracked as GitHub issue #45: https://github.com/crmartinez930130/food-tracker-ai/issues/45

## What Changes

- **Capture:**
  - a top app bar with a close action that cancels;
  - "Take photo" and "Choose from gallery" as two large tiles with icons;
  - the chosen photo shown large and rounded, with "Retake" / "Choose another" as a tonal button;
  - the description field with the microphone, as today;
  - portion as connected buttons (Small · Medium · Large), none selected by default, and tapping the selected one clears it;
  - a large pill "Analyze" button;
  - while analyzing, the expressive loading indicator on a container with an "Analyzing…" caption;
  - favorites as horizontal cards (foods, kcal, star to unmark). Tapping one re-logs it, as today.
- **Review:**
  - a top app bar with a close action that asks to discard, as the cancel button did;
  - the meal type as connected buttons instead of "[Snack]";
  - each food on a card: an editable name, a remove icon, grams and kcal, the three macros and a confidence chip;
  - the notes field;
  - a total card with the kcal as a big figure and the macro segmented bar with grams;
  - **a fixed pill at the bottom: "Guardar · 736 kcal"**, always visible above the list and updated live as foods are edited.
- No change to the flow: photo, description only, favorites, edit and remove foods, discard with confirmation, save.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `meal-capture`: the review screen shows a live total (big figure and macro bar), and its save action shows the total calories while staying visible.

## Impact

- **Frontend common:**
  - `CaptureScreen` and `ReviewScreen` are reorganized;
  - `ConnectedButtonGroup` gains "no selection" (index −1) and an optional check icon;
  - new pure helpers `toggledPortion`, `editableNumber` (now accepting a decimal comma) and `mealTotals`, with tests.
- **Resources:**
  - Material Symbols `photo_camera`, `photo_library` and `close`;
  - new strings: the save pill, the total title, the analyzing caption and the close descriptions;
  - `review_total` and `review_confirm_save` are replaced.
- **Backend:** no change.
