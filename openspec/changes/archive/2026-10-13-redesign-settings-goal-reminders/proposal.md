## Why

This is the fifth redesign issue inspired by the Material 3 Expressive samples (https://m3e.c5inco.dev/). The configuration screens are still flat lists:
- Options stacks radio buttons, divider rows and a server form;
- Reminders is rows with dividers;
- Goal is a plain column of six fields.

The reference is Greenwave's "You" screen and Shelfie's settings. Tracked as GitHub issue #46: https://github.com/crmartinez930130/food-tracker-ai/issues/46

## What Changes

- **A shared settings group:** a green section title over a rounded card that holds the section's rows, separated by thin dividers.
- **Options:**
  - **"Idioma"** as connected buttons: Teléfono · Español · English. The phone option is labeled "Teléfono" / "Phone" so three fit on one row.
  - **"Tema"** as connected buttons: Sistema · Claro · Oscuro.
  - **"General":** Reminders, plus the design sample in debug builds, as rows with a leading icon in a tinted square, a subtitle and a chevron.
  - **"Servidor"** in its own card: hint, field, Save / Test / Restore, and the status line.
- **Reminders:**
  - each meal on its own card, with:
    - the meal type's icon square (the same as Home's groups);
    - the meal name;
    - the time as a large tappable figure;
    - its switch.
  - the "notifications blocked" notice becomes an `errorContainer` card with its button.
- **Goal:**
  - the default-goal hint as a tinted card;
  - a "Calorías y macros" group (calories, protein, fat, carbs);
  - an "Agua" group (daily water, glass size, hint);
  - a full-width pill "Guardar meta" button.
- Everything saves and applies as today: language, theme, server, reminders with their permission, and goal with its validation.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `app-settings`: the language and theme choices are connected buttons, with the phone-language option labeled "Teléfono" / "Phone".

## Impact

- **Frontend common:**
  - a new `ui/components/SettingsGroup.kt`;
  - `SettingsScreen`, `RemindersScreen` and `GoalsScreen` are reorganized;
  - the meal type icon and colors move from `DayContent` to a shared place, so Reminders uses the same squares;
  - a pure `choiceIndex` helper for the connected choices, with tests.
- **Resources:**
  - Material Symbols `notifications` and `palette` icons;
  - new strings: `settings_language_phone`, `settings_general`, `goals_macros_section`;
  - `settings_language_system` is replaced by `settings_language_phone`.
- **Backend:** no change.
