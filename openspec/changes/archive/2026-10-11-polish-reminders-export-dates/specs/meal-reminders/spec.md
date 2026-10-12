## MODIFIED Requirements

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
Each enabled reminder SHALL show a notification once a day, at its configured local time or within about ten minutes after it, unless a meal of that type has already been logged for the current day. The notification SHALL ask whether that meal was logged, for example "Did you log your lunch?". Whether the meal is already logged SHALL be checked when the reminder fires. If that check cannot complete within a few seconds, the reminder SHALL be shown. A meal without a type SHALL NOT count as logging any particular meal. A new reminder for a meal SHALL replace any earlier one for the same meal still on screen, instead of stacking. Reminders SHALL keep firing on the following days, including after the device restarts or its clock or timezone changes, without the user opening the app.

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
