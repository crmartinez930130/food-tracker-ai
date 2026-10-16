## MODIFIED Requirements

### Requirement: App language
Every user-visible text produced by the app SHALL exist in Spanish and English and SHALL follow the language in use. This covers screen texts, dialogs, the app's own messages, chart labels, accessibility descriptions, relative day names, and the month and weekday names in date and time pickers.
- With the phone's language option ("Phone" / "Teléfono"), which SHALL be the default, the app SHALL use English when the phone's language is English, and Spanish for any other phone language, including Spanish.
- With Spanish or English chosen, the app SHALL use that language whatever the phone's language is.

Changing the language from Options SHALL keep the user on the Options screen.

On Android 13 and later, the language in use SHALL hold until the user changes it in Options, or changes the phone's language under the phone's language option. That includes after using the in-app camera, and in the language sent with a photo analysis.

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

#### Scenario: After using the camera
- **WHEN** the app is in Spanish on an Android 13+ phone set to English, and the user opens the in-app camera, cancels it or takes the photo, and goes back to Home
- **THEN** Home and every screen opened afterwards are in Spanish, and an analysis requested afterwards is requested in Spanish

#### Scenario: Phone language changes
- **WHEN** the phone's language option is chosen and the user changes the phone from Spanish to English and returns to the app
- **THEN** the app is in English
