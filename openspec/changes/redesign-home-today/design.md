# Design

## Context

The current structure is a `MainScreen` `Scaffold` with three tabs (`MainTab.HISTORY / SUMMARY / GOALS`) plus full-screen `Screen.Capture` / `Screen.Review` routes in `AppState`.

- **History** (`HistoryScreen`) holds its own date in `rememberSaveable`, loads `GET /api/meals?date=`, groups the meals by `mealType` and lets the user delete them.
- **Summary** has Daily and Weekly modes, backed by `GET /api/summary/daily|weekly`.
- **Goals** (`GoalsScreen`, #2) loads and saves `GET/PUT /api/goals`.

Navigation is a hand-rolled `when (screen)`. There is no back handling for full-screen routes. Theming and Material Symbols icons come from #1. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- One Home screen that makes the selected day's progress against the goal the first thing the user sees.
- Keep the hand-rolled navigation, adding only what Goals needs: a route, back handling and the preserved Home date.
- Put the numeric rules (fractions, over/remaining, day labels) in pure functions with unit tests.

**Non-Goals:**
- Charts or trends in Summary. That is #7. Summary only loses its Daily mode here.
- A navigation library (Navigation 3 / Voyager) or animated transitions.
- Back handling for Capture/Review. It is an existing gap, separate from this issue.
- Redesigning the Review screen, such as its `[Lunch]` selector. That belongs to a later UI issue.
- Backend or API changes.

## Decisions

**1. Home computes the day's totals from the meal list, not from `GET /api/summary/daily`.**
Home already loads the day's meals for the list, and summing their `total*` fields gives exactly what the summary endpoint returns (`nutrition-summary` spec, "Daily nutrition summary"). This saves a request per day change and guarantees that the ring and the list can never disagree, for example after a delete.
- *Alternative:* call both endpoints. That adds a second source of truth and a second loading/error state, with no gain.

**2. Progress rules as pure, tested functions.**
`ui/home/Progress.kt` holds:
- `progressOf(consumed, target)`: returns `fraction`, clamped to `0..1` for drawing; `isOver`; and `remaining`, or the excess when the target is passed.
- `dayLabel(date, today)`: returns "Today", "Yesterday" or the ISO date.

A target of 0 is defined explicitly: `fraction` is 1 and `isOver` is true when anything was consumed, otherwise `fraction` is 0. This avoids dividing by zero, and the spec scenario "Macro target of zero" relies on it. Today's date is passed in as a parameter, so the tests are deterministic.

**3. Material 3 progress components, with no custom Canvas drawing.**
- **Calories:** a large determinate `CircularProgressIndicator(progress = { fraction })`, about 180dp with a thick stroke, inside a `Box` that shows consumed / goal kcal and "N left" or "N over" in the center.
- **Macros:** one `LinearProgressIndicator` per macro, with a label row such as "Protein  56 / 125 g".
- **Over target:** the indicator color switches from `primary` to `error`, and the center text says "over". The `error` / `surface` pair is already in #1's contrast test.
- *Alternative:* a custom `Canvas` ring, for example with segments for each macro. It looks nicer, but brings more code and more tests, and M3's indicator is enough for the acceptance criteria. It can be revisited if we want a richer ring later.

**4. Goals becomes a full-screen route, and `AppState` owns the Home date.**
- `Screen.Goals` joins `Capture` and `Review`, with `openGoals()` and `openMain(...)` in `AppState`.
- **The Home date moves from `HistoryScreen`'s `rememberSaveable` into `AppState.homeDate`.** Leaving `Main` for `Goals` disposes `MainScreen`, so a date kept in local state would reset to today, which contradicts the "Return to Home" scenario. After a save, `openMain` sets `homeDate` to today, because new meals default to now.
- **Back:** `GoalsScreen` gets a `TopAppBar` with an `arrow_back` icon, and a `BackHandler` from `org.jetbrains.compose.ui:ui-backhandler` (`@OptIn(ExperimentalComposeUiApi::class)`). Both call `appState.openMain(HOME)`, and Home reloads the goal when it reappears.
  - *Correction found during implementation:* that artifact was only on the **runtime** classpath, pulled in transitively by material3 at 1.9.0-beta03, and not on the compile classpath. It is now declared explicitly in `libs.versions.toml` / `commonMain`, pinned to the `compose-multiplatform` version (1.9.3). This adds no new library to the app, because it was already shipped; it only makes the dependency explicit and version-aligned.
- *Alternative:* keep Goals as a tab. The user chose to reach it from Home, so the bottom bar keeps the issue's two destinations.

**5. Home header:**
- `IconButton(chevron_left)`, then the label from `dayLabel`, then `IconButton(chevron_right)`. The right chevron is disabled on today.
- At the end of the header, an `IconButton(edit)` whose `contentDescription` is "Edit daily goal".
- Tapping the ring also opens Goals, and the ring has an `onClickLabel` for accessibility.
- When `goal.isDefault` is true, an `AssistChip` reads "Using default goal — set yours" and opens Goals.

**6. Summary becomes weekly-only.** I remove the Daily/Weekly toggle and the daily state. The week header gets the same chevron `IconButton`s as Home. Its content (week total and per-day average cards) is unchanged. The default week now ends today, covering the last 7 days, instead of starting today. The old default showed today plus six future, empty days, which made the per-day average meaningless.

**7. Icons:** five more Material Symbols Outlined 24px drawables, added the same way as in #1 (`tint` attribute stripped, literal fill, and a source/license header): `home`, `chevron_left`, `chevron_right`, `arrow_back` and `edit`.

## Risks / Trade-offs

- **[Big rewrite of the main screen]** → The existing pieces are reused. Grouping and deleting move from `HistoryScreen` to `HomeScreen` almost unchanged, and `GoalsScreen` only gains a top bar. `HistoryScreen.kt` is deleted, not kept around dead.
- **[`BackHandler` is experimental in CMP 1.9 and deprecated in 1.10 in favor of `NavigationBackHandler`]** → It is used in one place behind an opt-in. Migrating it is a one-line change when we upgrade CMP.
- **[Totals summed on the client could drift from the backend's summary]** → Both sum the same stored `total*` fields of the same day's meals, and the backend groups by local date the same way the list query does.
