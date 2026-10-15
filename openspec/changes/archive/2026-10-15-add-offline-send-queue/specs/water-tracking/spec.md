## MODIFIED Requirements

### Requirement: Water on Home
Home SHALL show today's water consumed against the daily water target, as a figure and a progress indicator, on its own card. It SHALL offer a single-tap action that logs one glass, using the glass size from the goal, at the current time. It SHALL also offer an undo action that removes today's most recent entry, available only when today has entries. Logging or undoing SHALL update the shown total without the day visibly reloading. A failed log or undo SHALL show an error message, except that a glass logged while the backend can't be reached SHALL be queued on the phone and counted at once, and undoing a queued glass SHALL remove it on the phone (see `offline-queue`). The water shown SHALL come from the backend, plus the glasses still queued, so it is the same after closing and reopening the app. Going over the water target SHALL NOT be shown as an error.

#### Scenario: Log a glass from Home
- **WHEN** the user taps the add-glass action with a glass size of 250 ml
- **THEN** the shown total grows by 250 ml in one tap

#### Scenario: Log a glass without a connection
- **WHEN** the user taps the add-glass action and the backend can't be reached
- **THEN** the shown total grows by 250 ml and the glass is sent later

#### Scenario: Undo a mistaken tap
- **WHEN** the user taps undo after adding a glass by mistake
- **THEN** the latest entry is deleted and the total goes back to its previous value

#### Scenario: Over the water target
- **WHEN** today's water exceeds the water target
- **THEN** the progress indicator is full and keeps its normal color

#### Scenario: Past day
- **WHEN** a glass was logged yesterday and the user opens Home today
- **THEN** Home's water total does not include it

#### Scenario: Reopen the app
- **WHEN** the user closes and reopens the app after logging water today
- **THEN** Home shows the same water total for today
