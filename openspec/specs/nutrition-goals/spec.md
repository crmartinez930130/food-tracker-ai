# nutrition-goals Specification

## Purpose

Lets the user set daily calorie and macro targets so the app can measure each day's intake against them, and gives every client a usable goal even before the user has configured one.

## Requirements

### Requirement: Retrieve the daily goal
The system SHALL return the user's current daily goal: target calories, protein, fat and carbohydrates, plus an indicator of whether the goal is the built-in default or one the user configured.

#### Scenario: No goal configured yet
- **WHEN** the client requests the daily goal and the user has never saved one
- **THEN** the system returns the built-in default goal (2000 kcal, 125 g protein, 67 g fat, 225 g carbohydrates) marked as a default, rather than an error or an empty response

#### Scenario: Goal previously configured
- **WHEN** the client requests the daily goal after the user saved one
- **THEN** the system returns the saved targets, marked as not being a default

### Requirement: Set the daily goal
The system SHALL allow the client to create or replace the daily goal with new target calories, protein, fat and carbohydrates. Only one goal exists at a time; saving replaces the previous one without keeping history.

#### Scenario: Save a goal for the first time
- **WHEN** the client saves a valid goal and none existed before
- **THEN** the system persists it and returns the saved goal, and later requests for the goal return those targets

#### Scenario: Replace an existing goal
- **WHEN** the client saves a valid goal while one already exists
- **THEN** the system replaces the previous targets, and later requests for the goal return only the new ones

#### Scenario: Goal survives a restart
- **WHEN** the backend restarts after a goal was saved
- **THEN** requests for the goal still return the saved targets

### Requirement: Validate goal values
The system SHALL reject a goal that is missing any of the four targets, has a non-numeric target, has a calorie target that is not greater than zero, or has a negative macro target, and SHALL leave the stored goal unchanged when it rejects one.

#### Scenario: Missing target
- **WHEN** the client saves a goal that omits one of the four targets
- **THEN** the system responds with a 400-style validation error and the stored goal is unchanged

#### Scenario: Out-of-range values
- **WHEN** the client saves a goal with a calorie target of zero or less, or with a negative macro target
- **THEN** the system responds with a 400-style validation error and the stored goal is unchanged

### Requirement: Edit the goal from the app
The app SHALL provide a screen, reachable from Home at any time (through an edit-goal action in Home's header and by tapping the calorie progress indicator), that shows the current goal loaded from the backend and lets the user edit and save all four targets. The screen SHALL offer a back action, and going back SHALL return to Home on the day that was being viewed, with progress measured against the latest saved goal.

#### Scenario: Open the goal screen
- **WHEN** the user opens the goal screen
- **THEN** the app shows the goal currently stored in the backend, or the default when none is configured, and indicates when it is showing the default

#### Scenario: Save from the app
- **WHEN** the user edits the targets and saves them
- **THEN** the app sends them to the backend, and reopening the screen, including after restarting the app, shows the saved values

#### Scenario: Backend rejects the values
- **WHEN** the user saves values that the backend rejects, or the request fails
- **THEN** the app shows an error message and keeps the user's edits on screen so they can correct them

#### Scenario: Return to Home
- **WHEN** the user goes back from the goal screen (back arrow or system back) after viewing a past day on Home and saving a new goal
- **THEN** Home shows the same day it was showing before, with progress measured against the newly saved goal
