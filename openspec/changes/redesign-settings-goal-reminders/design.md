## Context

- **`SettingsScreen`:**
  - a `ChoiceGroup` of radio rows for language and for theme;
  - divider-separated rows for Reminders and, in debug builds only, the design sample;
  - a `ServerSection` (title, hint, field, Save/Test/Restore, status).
- **`RemindersScreen`:**
  - an intro text, then a `ReminderRow` per meal (name, a `TextButton` with the time, a `Switch`) with dividers;
  - a blocked-notifications message with a button;
  - a time picker dialog.
- **`GoalsScreen`:**
  - the default hint as plain text;
  - six `OutlinedTextField`s, with an "Agua" title before the last two;
  - error and saved texts, and a `Button`.
- **Components from earlier issues:**
  - `ConnectedButtonGroup` (#44 and #45);
  - the meal type icon squares (#43, private in `DayContent`);
  - `AppShapes` and the Greenwave colors.

## Goals / Non-Goals

**Goals:**
- Grouped, card-based settings in the expressive style.
- Exactly the same behavior and persistence as today.

**Non-Goals:**
- New options.
- Backend changes.
- Restyling the time picker or the reminder notification.
- Animations (#47).

## Decisions

### D1. `SettingsGroup`
- **The composable:** `SettingsGroup(title, modifier, content)` in `ui/components/`. It shows:
  - the title in `labelLarge`, `primary`, with 4 dp start padding;
  - then a `surfaceContainerLow` card with `shapes.large` holding a `Column`.
- **`SettingsRow(icon, iconColors, title, subtitle, onClick)`** is a clickable row with:
  - a 40 dp icon square (`shapes.small`);
  - the title (`titleMedium`) and subtitle (`bodyMedium`, `onSurfaceVariant`);
  - a trailing chevron.

  The rows go inside the card, separated by `outlineVariant` dividers with an inset.
- **Spacing:** the groups are 24 dp apart.

### D2. Options
- **Language and theme** are `ConnectedButtonGroup`s inside their groups' cards (16 dp padding).
  - `choiceIndex(options, selected)` maps the stored preference to the selected index. It is pure and tested: it returns the index for each value, and −1 for a value not in the list, which can't happen with an enum.
  - Both groups show the check on the selected button. Each button is about 115 dp wide, enough for "Teléfono", "Español" or "English" plus the check; the emulator confirms it.
- **Phone language label:**
  - `settings_language_system` ("Idioma del teléfono") becomes `settings_language_phone`: "Teléfono" / "Phone";
  - the group title "Idioma" gives it context, and the behavior is unchanged.
- **"General" group:**
  - Reminders, with the `notifications` icon on `tertiaryContainer`;
  - in debug builds, the design sample, with the `palette` icon on `secondaryContainer`.
- **"Servidor" group:** the existing `ServerSection` body moves into the card unchanged: hint, field, the three buttons and the status.

### D3. Reminders
- **Intro:** the intro text stays above.
- **One card per meal** (`surfaceContainerLow`), as a row with:
  - the meal type icon square, shared with Home (D5);
  - the name (`titleMedium`);
  - the time as a `TextButton` with `headlineSmall` ExtraBold digits, which opens the picker;
  - the `Switch`.

  The cards are spaced 12 dp apart.
- **Blocked notifications:** an `errorContainer` card with the message and a tonal "Abrir ajustes" button.

### D4. Goal
- **Default hint:** when the goal is the default one, a `secondaryContainer` card holds the hint.
- **`SettingsGroup("Calorías y macros")`** holds the four fields (calories, protein, fat, carbs), 16 dp inside the card.
- **`SettingsGroup("Agua")`** holds daily water, glass size and the hint.
- **Messages:** the error and saved texts go under the groups.
- **Save:** a full-width 56 dp pill "Guardar meta" button, with the small inline progress while saving.
- **Validation:** client-side validation and the backend request are unchanged.

### D5. Shared meal type icons
- **The move:** `MealType?.iconStyle()` (icon, square color, icon color), private in `DayContent`, moves to `ui/MealTypeLabel.kt` as `mealTypeIconStyle()`, next to the meal type labels.
- **Square:** a small `MealTypeIcon(type, size)` composable draws the square. Home's groups and Reminders both use it.

### D6. Resources
- **Icons:** Material Symbols `notifications` and `palette`.
- **Strings:**
  - `settings_language_phone` ("Teléfono" / "Phone") replaces `settings_language_system`;
  - `settings_general` ("General");
  - `goals_macros_section` ("Calorías y macros" / "Calories and macros").

## Risks / Trade-offs

- **"Teléfono" is less explicit than "Idioma del teléfono".** → The group's title, "Idioma", and the spec change make the meaning clear. The behavior is unchanged.
- **Moving the icon style out of `DayContent`.** → It's a pure move, and the emulator check covers Home's groups too.

## Migration Plan

None. This is a frontend-only visual change. The stored preferences are unchanged, because the enum values stay the same.
