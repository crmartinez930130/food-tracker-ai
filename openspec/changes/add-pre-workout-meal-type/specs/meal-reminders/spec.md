## MODIFIED Requirements

### Requirement: Reminders are opt-in
The app SHALL offer daily reminders for pre-workout, breakfast, lunch and dinner. Each SHALL have its own on/off setting and time of day, with default times of 07:15, 08:30, 13:00 and 20:00. All reminders SHALL be off until the user turns them on. No notification SHALL be scheduled or shown for a reminder that is off. Settings stored before the pre-workout reminder existed SHALL keep their values, with the pre-workout reminder off at its default time.

#### Scenario: Fresh install
- **WHEN** the user installs and opens the app without visiting the reminders screen
- **THEN** no reminder is enabled and no reminder notification is ever shown

#### Scenario: Unreadable stored settings
- **WHEN** the stored reminder settings are missing or cannot be read
- **THEN** the app treats every reminder as off with its default time, without crashing

#### Scenario: Update from a version without pre-workout
- **WHEN** the lunch reminder was on at 12:45 and the app is updated to a version with the pre-workout reminder
- **THEN** lunch is still on at 12:45, still fires as lunch, and the pre-workout reminder is off at 07:15

### Requirement: Daily reminder notification
Each enabled reminder SHALL show a notification once a day, at its configured local time or within about ten minutes after it, unless a meal of that type has already been logged for the current day. The notification SHALL ask whether that meal was logged, for example "¿Ya registraste tu almuerzo?" in Spanish or "Did you log your lunch?" in English, in the app's language as chosen in Options, even when the app is not running. Whether the meal is already logged SHALL be checked when the reminder fires. If that check cannot complete within a few seconds, the reminder SHALL be shown. A meal without a type SHALL NOT count as logging any particular meal. A new reminder for a meal SHALL replace any earlier one for the same meal still on screen, instead of stacking. Reminders SHALL keep firing on the following days, including after the device restarts or its clock or timezone changes, without the user opening the app.

#### Scenario: Reminder fires
- **WHEN** the lunch reminder is on at 13:00, no lunch is logged today, and the device's local time reaches 13:00
- **THEN** a "Did you log your lunch?" notification appears shortly after

#### Scenario: Pre-workout reminder fires
- **WHEN** the pre-workout reminder is on at 07:15, no pre-workout is logged today, and the device's local time reaches 07:15
- **THEN** a "¿Ya registraste tu preentreno?" notification appears shortly after when the app language is Spanish

#### Scenario: Meal already logged
- **WHEN** the lunch reminder fires and a meal tagged as lunch is already logged today
- **THEN** no notification is shown, and the reminder still fires again tomorrow

#### Scenario: Breakfast does not count as pre-workout
- **WHEN** the pre-workout reminder fires and today only has a meal tagged as breakfast
- **THEN** the pre-workout notification is shown

#### Scenario: Backend unreachable
- **WHEN** the lunch reminder fires and today's meals can't be retrieved within a few seconds
- **THEN** the "Did you log your lunch?" notification is shown

#### Scenario: Next day
- **WHEN** a reminder has fired today
- **THEN** it fires again at the same time tomorrow

#### Scenario: Device restart
- **WHEN** the device restarts while a reminder is on
- **THEN** the reminder is still scheduled and fires at its next time

#### Scenario: Notification language
- **WHEN** the app language is Spanish, the phone is set to English, and the lunch reminder fires
- **THEN** the notification reads "¿Ya registraste tu almuerzo?"
