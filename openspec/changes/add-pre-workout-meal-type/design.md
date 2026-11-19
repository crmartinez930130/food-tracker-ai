# Design

## Context

See proposal.md for motivation. Meal types live in two places that must agree: the backend's `MEAL_TYPES` tuple (`backend/src/db/schema.ts`), from which the Zod `MealTypeSchema` and the TS type derive, and the shared Kotlin `MealType` enum (`frontend/shared/.../data/Models.kt`), serialized with `@SerialName`. `meal_type` is a nullable `text` column validated only in Zod (see the archived `2026-10-05-add-meal-type` design), so a new value needs no migration.

On Android, `MealType.ordinal` is used as identity in two places: the reminder alarm's `PendingIntent` request code (`ReminderScheduler.kt`) and the notification id `1000 + ordinal` (`ReminderNotifications.kt`). The review selector and the time-based suggestion use `MealType.entries` order (`ReviewScreen.kt`).

## Goals / Non-Goals

**Goals:**
- Add `pre_workout` end to end without disturbing existing alarms, stored reminder settings or saved meals.
- Fit five type options on the review screen at phone width.

**Non-Goals:**
- Wear OS UI for pre-workout (the watch just gets rebuilt with the new shared enum).
- Making the client tolerant of unknown future meal types (`coerceInputValues`); see Risks.
- Any link to training data (Hevy, Fitbit) — this is only a label.

## Decisions

### 1. Append `PRE_WORKOUT` at the end of the enum; display order is explicit
**Choice:** `enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK, PRE_WORKOUT }`, and a single shared display order `MEAL_TYPE_DISPLAY_ORDER = [PRE_WORKOUT, BREAKFAST, LUNCH, DINNER, SNACK]` (in `MealTypeLabel.kt`) used by the review selector and Home's sections (Home appends `null` for "Other").

**Why:** Inserting it after `BREAKFAST` would shift the ordinals of lunch/dinner/snack, so an already-scheduled lunch alarm (request code 1) would be overwritten or cancelled as pre-workout, and a lunch notification still on screen would not be replaced by the next one. Appending keeps every existing ordinal stable. Alternative considered: switching request codes to a stable per-type constant — cleaner long term but touches scheduling and needs cancelling old codes; not worth it for this change.

### 2. Selector: icon above a small label in the existing connected group
**Choice:** Keep `ConnectedButtonGroup` (M3 Expressive connected style) and add an optional per-option icon. With icons, each button shows the type's icon (already defined in `MealType.iconStyle()`) above a `labelSmall` label, and the group grows to 56 dp. At 360 dp width each of the five buttons is ~64 dp, enough for "Preentreno" at 11 sp.

**Why:** "Preentreno" at the current `labelLarge` needs ~75 dp, so a plain five-option row would truncate. Alternatives considered: a wrapping row of filter chips (fits, but a different look from every other single-choice group in the app); a horizontally scrolling row (hides options, against the spec). Fallback if the emulator shows truncation at 360 dp in Spanish: wrap into chips — decided during implementation, behavior in the spec unchanged.

### 3. Time-based suggestion stays a client-only UI default
**Choice:** Extend `suggestedMealType(hour)` with `hour in 7..8 -> PRE_WORKOUT` before the breakfast branch; make it `internal` so it gets a unit test. The backend still never infers a type.

### 4. Reminders: one more entry in the defaults
**Choice:** Add `MealReminder(PRE_WORKOUT, 7, 15)` first in `ReminderSettings.DEFAULT` (the screen lists reminders in that order, so pre-workout shows above breakfast, chronological). `decodeReminderSettings` already falls back to the default for any meal missing from stored JSON, which gives the "update keeps settings, pre-workout off" behavior for free. Add `reminder_title_pre_workout` and the time-dialog string; `reminderTitle()`/dialog `when`s get the new branch, and `SNACK` stays the only type without a reminder.

### 5. Icon
**Choice:** Material Symbols `fitness_center` as `ic_fitness_center.xml`, on `tertiaryContainer` like breakfast (both morning meals) — no new colors.

## Risks / Trade-offs

- **[Risk] Old clients can't decode `pre_workout`.** The Ktor/cache `Json` has `ignoreUnknownKeys` but not `coerceInputValues`, so an older phone APK or the current watch build fails to read any day that contains a pre-workout. → Mitigation: install the new APK on the phone and rebuild/reinstall the watch app in the same rollout; documented in the issue. Making decoding tolerant is a separate change.
- **[Risk] Backend deployed after the APK** → saves of pre-workout meals get a 400 and sit in the offline queue. → Mitigation: redeploy the laptop backend first (rollout below).
- **[Trade-off]** Breakfast's default reminder (08:30) falls inside the pre-workout window; harmless, since reminders are per type.

## Migration Plan

1. Merge; redeploy the laptop backend (systemd service) — no DB migration.
2. Install the new APK on the phone; rebuild and install the Wear app on the watch.
Rollback: revert the backend; any meals already saved as `pre_workout` would then fail old-client decoding, so rollback means editing those meals' type first.
