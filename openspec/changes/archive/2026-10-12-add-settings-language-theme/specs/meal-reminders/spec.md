## MODIFIED Requirements

### Requirement: Configure reminders in the app
The app SHALL provide a reminders screen, reachable from the Options screen. On it the user SHALL be able to turn each meal's reminder on or off and change its time. Changes SHALL take effect immediately and SHALL persist on the device across app restarts. The screen SHALL offer a back action that returns to Options.

#### Scenario: Turn a reminder on and set its time
- **WHEN** the user turns on the lunch reminder and changes its time to 12:45
- **THEN** a lunch reminder is scheduled for the next 12:45, and reopening the screen, including after restarting the app, shows lunch on at 12:45

#### Scenario: Change the time of an enabled reminder
- **WHEN** the user changes the time of a reminder that is on
- **THEN** it is rescheduled for the new time and no longer fires at the old one

### Requirement: Daily reminder notification
Each enabled reminder SHALL show a notification once a day, at its configured local time or within about ten minutes after it, unless a meal of that type has already been logged for the current day. The notification SHALL ask whether that meal was logged, for example "¿Ya registraste tu almuerzo?" in Spanish or "Did you log your lunch?" in English, in the app's language as chosen in Options, even when the app is not running. Whether the meal is already logged SHALL be checked when the reminder fires. If that check cannot complete within a few seconds, the reminder SHALL be shown. A meal without a type SHALL NOT count as logging any particular meal. A new reminder for a meal SHALL replace any earlier one for the same meal still on screen, instead of stacking. Reminders SHALL keep firing on the following days, including after the device restarts or its clock or timezone changes, without the user opening the app.

#### Scenario: Reminder fires
- **WHEN** the lunch reminder is on at 13:00, no lunch is logged today, and the device's local time reaches 13:00
- **THEN** a "Did you log your lunch?" notification appears shortly after

#### Scenario: Meal already logged
- **WHEN** the lunch reminder fires and a meal tagged as lunch is already logged today
- **THEN** no notification is shown, and the reminder still fires again tomorrow

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
