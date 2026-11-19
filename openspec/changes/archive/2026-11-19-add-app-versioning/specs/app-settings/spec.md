# Spec Delta

## MODIFIED Requirements

### Requirement: Options screen
The app SHALL provide an Options screen, reachable from a settings action in Home's header. It SHALL contain:
- a language choice between the phone's language (labeled "Phone" / "Teléfono"), Spanish and English;
- a theme choice between System, Light and Dark;
- an entry that opens the meal reminders screen;
- the backend server address;
- a version section, last on the screen, with the app's and the server's versions.

Each choice SHALL be shown as a single-choice group of connected buttons with the current choice selected. A choice SHALL take effect immediately, without restarting the app, and SHALL keep the user on the Options screen. Choices SHALL persist on the device across app restarts. The screen SHALL offer a back action that returns to Home.

#### Scenario: Open Options
- **WHEN** the user taps the settings action in Home's header
- **THEN** the Options screen shows the current language and theme choices, an entry for reminders, the current server address and the version section

#### Scenario: Choice persists
- **WHEN** the user picks English and Dark, closes the app and opens it again
- **THEN** the app is in English and dark, and Options shows those choices selected

## ADDED Requirements

### Requirement: App and server versions
The Options screen's version section SHALL show:
- **App version:** the app's version and Android version code, as "Versión 0.2.0 (200)" / "Version 0.2.0 (200)". In development (debuggable) builds it SHALL also show the short commit the build was made from.
- **Server version:** the version reported by the backend server in use, as "Servidor 0.2.0" / "Server 0.2.0". It SHALL be requested when the screen opens and again after a new server address is saved, and SHALL show a loading state meanwhile.
  - When the server answers that it has no version endpoint (an older server), it SHALL show "anterior a 0.2.0" / "older than 0.2.0".
  - When the server can't be reached, rejects the access token or answers with any other error, it SHALL show "no disponible" / "not available".

Failing to get the server version SHALL NOT block or change anything else on the screen.

#### Scenario: Both versions known
- **WHEN** the app at `0.2.0` (code 200) is connected to a backend at `0.2.0` and the user opens Options
- **THEN** the version section shows "Versión 0.2.0 (200)" and "Servidor 0.2.0"

#### Scenario: Older server
- **WHEN** the backend in use predates the version endpoint
- **THEN** the server version reads "anterior a 0.2.0"

#### Scenario: Server unreachable
- **WHEN** the user opens Options without network access to the server
- **THEN** the server version reads "no disponible" and the rest of the screen works as usual

#### Scenario: Change server
- **WHEN** the user saves the address of another server
- **THEN** the server version is requested again from the new server

#### Scenario: Debug build
- **WHEN** the user opens Options in a development build made from commit `abc1234`
- **THEN** the app version line also shows `abc1234`
