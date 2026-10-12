# meal-reminders Specification

## Purpose

Offers optional, quiet daily reminders to log breakfast, lunch or dinner, which open the meal capture screen, without pushing the user into anxious tracking.

## Requirements

### Requirement: Reminders are opt-in
The app SHALL offer daily reminders for breakfast, lunch and dinner. Each SHALL have its own on/off setting and time of day, with default times of 08:30, 13:00 and 20:00. All reminders SHALL be off until the user turns them on. No notification SHALL be scheduled or shown for a reminder that is off.

#### Scenario: Fresh install
- **WHEN** the user installs and opens the app without visiting the reminders screen
- **THEN** no reminder is enabled and no reminder notification is ever shown

#### Scenario: Unreadable stored settings
- **WHEN** the stored reminder settings are missing or cannot be read
- **THEN** the app treats every reminder as off with its default time, without crashing

### Requirement: Configure reminders in the app
The app SHALL provide a reminders screen, reachable from the Options screen. On it the user SHALL be able to turn each meal's reminder on or off and change its time. Changes SHALL take effect immediately and SHALL persist on the device across app restarts. The screen SHALL offer a back action that returns to Options.

#### Scenario: Turn a reminder on and set its time
- **WHEN** the user turns on the lunch reminder and changes its time to 12:45
- **THEN** a lunch reminder is scheduled for the next 12:45, and reopening the screen, including after restarting the app, shows lunch on at 12:45

#### Scenario: Change the time of an enabled reminder
- **WHEN** the user changes the time of a reminder that is on
- **THEN** it is rescheduled for the new time and no longer fires at the old one

### Requirement: Notification permission
On platforms that require a runtime permission to show notifications, turning a reminder on SHALL request that permission if it has not been granted. If the user denies it, the reminder SHALL stay off and the screen SHALL explain that notifications are blocked and offer a way to open the system notification settings for the app. Whenever the reminders screen returns to the foreground, for example coming back from system settings, the explanation SHALL reflect the current state:
- it SHALL be shown when a reminder is on but notifications are not allowed;
- it SHALL be hidden once notifications are allowed.

#### Scenario: Permission granted
- **WHEN** the user turns on their first reminder and allows notifications
- **THEN** the reminder is on and scheduled

#### Scenario: Permission denied
- **WHEN** the user turns on a reminder and denies notifications
- **THEN** the reminder stays off and the screen explains how to allow notifications

#### Scenario: Allowed from system settings
- **WHEN** the screen shows that notifications are blocked, and the user opens system settings, allows them and comes back
- **THEN** the explanation is no longer shown, without leaving and reopening the screen

#### Scenario: Blocked from system settings
- **WHEN** a reminder is on, and the user blocks the app's notifications in system settings and comes back to the reminders screen
- **THEN** the screen explains that notifications are blocked

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

### Requirement: Tapping a reminder opens capture
Tapping a reminder notification SHALL open the app on the meal capture screen and dismiss the notification. If the user is already capturing or reviewing a meal, the app SHALL stay on that screen and SHALL NOT discard the work in progress.

#### Scenario: App closed
- **WHEN** the user taps a reminder while the app is not running
- **THEN** the app opens directly on the capture screen

#### Scenario: Reviewing a meal
- **WHEN** the user taps a reminder while reviewing an analyzed meal
- **THEN** the review screen stays as it was, with its analysis

### Requirement: Turning reminders off cancels them
Turning a reminder off SHALL cancel its scheduled notification, so it does not fire again. The scheduled reminders SHALL always match the on/off settings, including after app restarts.

#### Scenario: Turn a reminder off
- **WHEN** the user turns off the dinner reminder before its time
- **THEN** no dinner notification is shown at that time or on later days, and no dinner alarm remains scheduled on the device
