## MODIFIED Requirements

### Requirement: Backend server address
The app SHALL let the user set the address of the backend server and its access token in Options, so the app can be used from a real phone on the same network as the server.
- **Valid addresses:** the address SHALL use `http` or `https`, SHALL name a host and an optional port from 1 to 65535, and SHALL have no path, query or fragment. A single trailing `/` SHALL be ignored.
- **Invalid addresses:** an address that does not meet these rules SHALL be rejected with a message, and the stored address SHALL stay unchanged.
- **Saving:** a saved address SHALL be used by every request to the backend from then on, without restarting the app. That includes the check a meal reminder makes while the app is not running. It SHALL persist across restarts.
- **Default:** when no address has been saved, the app SHALL use its built-in default, which is the emulator's host alias on Android.
- **Restore default:** the user SHALL be able to go back to the default.
- **Unencrypted addresses:** when the address uses `http://`, the setting SHALL say that the connection isn't encrypted and is meant for trusted networks. This SHALL inform, not block saving.
- **Access token:** the token SHALL be entered in a field hidden by default, which the user can reveal. It SHALL be saved together with the address and sent with every request to the backend, including the meal reminder's check. When the server rejects the token, the app SHALL say so and point to this setting, instead of showing the server's raw error.
- **Connection test:** the user SHALL be able to test the address and token being edited before saving them. The test SHALL report whether the server answered and, if not, the reason, telling a rejected token apart from an unreachable server.

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

#### Scenario: Save the access token
- **WHEN** the user enters the server's token next to the address and saves
- **THEN** every screen loads its data with that token, and it is still used after restarting the app

#### Scenario: Rejected token
- **WHEN** the saved token doesn't match the server's
- **THEN** screens show that the server rejected the access token and point to Options → Server, and the connection test reports the token as rejected

#### Scenario: Unencrypted address
- **WHEN** the address field contains `http://192.168.1.50:3000`
- **THEN** a note under it says the connection isn't encrypted and to use it only on trusted networks, and it can still be saved

#### Scenario: Encrypted address
- **WHEN** the address field contains an `https://` address
- **THEN** no unencrypted-connection note is shown

## ADDED Requirements

### Requirement: Settings stay on the device
The app's settings (language, theme, server address, access token and meal reminders) SHALL NOT be included in Android's cloud backups or in device-to-device transfers, on any supported Android version. Setting the app up on a new phone SHALL start from the defaults.

#### Scenario: Cloud backup
- **WHEN** Android backs up the phone to the cloud
- **THEN** none of the app's settings, including the access token, are part of the backup

#### Scenario: New phone
- **WHEN** the user moves to a new phone with a device-to-device transfer and installs the app
- **THEN** the app starts with its default settings and no access token
