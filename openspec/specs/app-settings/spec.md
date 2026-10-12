# app-settings Specification

## Purpose

Lets the user choose the app's language (Spanish or English, following the phone by default) and its light or dark theme from an Options screen, keeps every text the app produces available in both languages, and lets the user point the app at the backend server it should use.

## Requirements

### Requirement: Options screen
The app SHALL provide an Options screen, reachable from a settings action in Home's header. It SHALL contain:
- a language choice: "Phone language", Spanish or English;
- a theme choice: System, Light or Dark;
- an entry that opens the meal reminders screen;
- the backend server address.

A choice SHALL take effect immediately, without restarting the app, and SHALL keep the user on the Options screen. Choices SHALL persist on the device across app restarts. The screen SHALL offer a back action that returns to Home.

#### Scenario: Open Options
- **WHEN** the user taps the settings action in Home's header
- **THEN** the Options screen shows the current language and theme choices, an entry for reminders and the current server address

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

### Requirement: Backend server address
The app SHALL let the user set the address of the backend server in Options, so the app can be used from a real phone on the same network as the server.
- **Valid addresses:** the address SHALL use `http` or `https`, SHALL name a host and an optional port from 1 to 65535, and SHALL have no path, query or fragment. A single trailing `/` SHALL be ignored.
- **Invalid addresses:** an address that does not meet these rules SHALL be rejected with a message, and the stored address SHALL stay unchanged.
- **Saving:** a saved address SHALL be used by every request to the backend from then on, without restarting the app. That includes the check a meal reminder makes while the app is not running. It SHALL persist across restarts.
- **Default:** when no address has been saved, the app SHALL use its built-in default, which is the emulator's host alias on Android.
- **Restore default:** the user SHALL be able to go back to the default.
- **Connection test:** the user SHALL be able to test the address being edited before saving it. The test SHALL report whether the server answered and, if not, the reason.

#### Scenario: Use the app from a phone
- **WHEN** the user enters `http://192.168.1.50:3000` (the server's address on the home network) and saves it
- **THEN** Home and every other screen load their data from that server immediately, and still do after restarting the app

#### Scenario: Invalid address
- **WHEN** the user enters `192.168.1.50:3000` without a scheme, or `http://host/path`, and saves
- **THEN** the app explains the expected format and keeps the previous address

#### Scenario: Test before saving
- **WHEN** the user types an address and taps the connection test
- **THEN** the app reports that it connected, or shows why it couldn't, without saving the address

#### Scenario: Restore the default
- **WHEN** the user restores the default address
- **THEN** the field shows the built-in default and requests use it again

#### Scenario: Reminder uses the saved address
- **WHEN** a meal reminder fires while the app is not running and a custom address is saved
- **THEN** the reminder checks today's meals on the saved server
