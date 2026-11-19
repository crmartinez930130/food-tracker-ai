# Proposal

## Why

The user eats something before training, usually between 07:00 and 09:00, and today has to log it as breakfast or snack, which mixes it with the real breakfast and hides it in the day. A dedicated "Pre-workout" meal type keeps it apart (issue #115).

## What Changes

- New meal type `pre_workout` ("Preentreno" / "Pre-workout"), accepted by the backend on save and edit, returned on reads and written to the CSV export.
- The app shows it everywhere a meal type is shown: the review screen's type selector (now five options), Home's groups (first, above breakfast) with its own icon, and edit flows.
- The review screen suggests pre-workout for meals logged between 07:00 and 08:59 local time; breakfast keeps being suggested before 07:00 and from 09:00 to 10:59.
- New opt-in daily reminder "¿Ya registraste tu preentreno?", default 07:15, editable like the others, skipped when a pre-workout is already logged that day.
- Out of scope: showing pre-workout on the Wear OS app. The watch shares the data model, so it needs a reinstall with a build from this change to keep reading days that contain a pre-workout.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `meal-tracking`: the allowed meal types include `pre_workout`.
- `data-export`: the CSV `meal_type` column can be `pre_workout`.
- `daily-dashboard`: Home groups pre-workout meals first, above breakfast.
- `meal-reminders`: a fourth reminder, for pre-workout, default 07:15.
- `meal-capture`: the review screen's suggested meal type by time of day becomes a spec requirement, including the new 07:00–08:59 pre-workout window.

## Impact

- Backend: `MEAL_TYPES` (`backend/src/db/schema.ts`), `backend/src/domain/meal.ts`, Zod schemas pick it up automatically; no DB migration (`meal_type` is `text`). Must be redeployed (laptop service) before the new APK is used.
- Shared Kotlin model: `MealType` enum (`frontend/shared/.../data/Models.kt`), so the Wear app is rebuilt too.
- App: `MealTypeLabel.kt` (label, icon, colors), `ReviewScreen.kt` (selector, suggestion), `home/DayContent.kt` (group order), reminders (`ReminderSettings.kt`, `RemindersScreen.kt`, Android scheduler/notifications), strings (es/en), a new `ic_fitness_center` drawable.
