# app-settings Specification

## Purpose

Lets the user choose the app's language (Spanish or English, following the phone by default) and its light or dark theme from an Options screen, and keeps every text the app produces available in both languages.

## Requirements

### Requirement: Options screen
The app SHALL provide an Options screen, reachable from a settings action in Home's header. It SHALL contain:
- a language choice: "Phone language", Spanish or English;
- a theme choice: System, Light or Dark;
- an entry that opens the meal reminders screen.

A choice SHALL take effect immediately, without restarting the app, and SHALL keep the user on the Options screen. Choices SHALL persist on the device across app restarts. The screen SHALL offer a back action that returns to Home.

#### Scenario: Open Options
- **WHEN** the user taps the settings action in Home's header
- **THEN** the Options screen shows the current language and theme choices and an entry for reminders

#### Scenario: Choice persists
- **WHEN** the user picks English and Dark, closes the app and opens it again
- **THEN** the app is in English and dark, and Options shows those choices selected

### Requirement: App language
Every user-visible text produced by the app SHALL exist in Spanish and English and SHALL follow the language in use. This covers screen texts, dialogs, the app's own messages, chart labels, accessibility descriptions, relative day names, and the month and weekday names in date and time pickers.
- With "Phone language", which SHALL be the default, the app SHALL use English when the phone's language is English, and Spanish for any other phone language, including Spanish.
- With Spanish or English chosen, the app SHALL use that language whatever the phone's language is.

Changing the language from Options SHALL keep the user on the Options screen.

Texts that come from outside the app SHALL be shown as received: the details of backend error messages, the CSV export's column headers, and the food names produced by photo analysis.

#### Scenario: Fresh install, phone in English
- **WHEN** the app is opened for the first time on a phone set to English
- **THEN** the whole interface is in English

#### Scenario: Fresh install, phone in another language
- **WHEN** the app is opened for the first time on a phone set to French
- **THEN** the whole interface is in Spanish

#### Scenario: Fixed language
- **WHEN** the user chooses Spanish while the phone is set to English
- **THEN** the whole interface switches to Spanish immediately, and stays in Spanish after restarting the app

### Requirement: Unreadable preferences
Missing or unreadable stored preferences SHALL be treated as the defaults, "Phone language" and System theme, without crashing.

#### Scenario: Corrupt stored preferences
- **WHEN** the stored preferences cannot be read
- **THEN** the app starts with "Phone language" and System theme
