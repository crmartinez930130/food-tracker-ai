## MODIFIED Requirements

### Requirement: Favorites available offline
The capture screen SHALL keep a copy of the favorites list it last loaded and SHALL show it when the backend can't be reached, so a favorite can be re-logged without a connection. The copy SHALL be replaced whenever the list loads, and changing the server address SHALL discard it, like the rest of the cache.

While the copy is shown because the backend can't be reached, the favorites section SHALL say so with the time the copy was loaded ("No connection · data from 14:32", with the date when it isn't from today), like History and Summary. Without a copy and without a connection, the section SHALL say that the favorites haven't been loaded yet instead of showing a connection error. When the backend answers with an error, the section SHALL keep showing the copy, or the error when there is no copy. These texts SHALL be available in Spanish and English.

#### Scenario: Re-log a favorite offline
- **WHEN** the user opens capture without a connection after having seen the favorites before
- **THEN** the favorites are listed with "No connection · data from" and the time they were loaded, and saving one queues it

#### Scenario: Favorites never loaded, no connection
- **WHEN** the user opens capture without a connection and the favorites were never loaded from the current server
- **THEN** the section says the favorites haven't been loaded yet, and no connection error is shown

#### Scenario: Connected
- **WHEN** the user opens capture and the backend answers
- **THEN** the favorites are listed without any notice

#### Scenario: Another server
- **WHEN** the user switches to another server and opens capture without a connection
- **THEN** the previous server's favorites aren't listed
