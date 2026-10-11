# nutrition-goals Specification

## Purpose

Lets the user set daily calorie, macro and water targets so the app can measure each day's intake against them, and gives every client a usable goal even before the user has configured one.

## Requirements

### Requirement: Retrieve the daily goal
The system SHALL return the user's current daily goal: target calories, protein, fat and carbohydrates, the daily water target in millilitres, and the glass size in millilitres used for one-tap water logging, plus an indicator of whether the goal is the built-in default or one the user configured.

#### Scenario: No goal configured yet
- **WHEN** the client requests the daily goal and the user has never saved one
- **THEN** the system returns the built-in default goal (2000 kcal, 125 g protein, 67 g fat, 225 g carbohydrates, 2000 ml of water and a 250 ml glass) marked as a default, rather than an error or an empty response

#### Scenario: Goal previously configured
- **WHEN** the client requests the daily goal after the user saved one
- **THEN** the system returns the saved targets, marked as not being a default

#### Scenario: Goal saved before water targets existed
- **WHEN** a goal was saved before the water target and glass size existed
- **THEN** the system returns that goal with the default water target (2000 ml) and glass size (250 ml)

### Requirement: Set the daily goal
The system SHALL allow the client to create or replace the daily goal with new target calories, protein, fat and carbohydrates, and optionally a water target and a glass size. When the water target or the glass size is omitted, the system SHALL keep its current value, or the default when no goal has been saved yet. Only one goal exists at a time; saving replaces the previous one without keeping history.

#### Scenario: Save a goal for the first time
- **WHEN** the client saves a valid goal and none existed before
- **THEN** the system persists it and returns the saved goal, and later requests for the goal return those targets

#### Scenario: Replace an existing goal
- **WHEN** the client saves a valid goal while one already exists
- **THEN** the system replaces the previous targets, and later requests for the goal return only the new ones

#### Scenario: Save without water fields
- **WHEN** the client saves only the four nutrition targets while a water target of 2500 ml is stored
- **THEN** the nutrition targets are replaced and the water target stays 2500 ml

#### Scenario: Goal survives a restart
- **WHEN** the backend restarts after a goal was saved
- **THEN** requests for the goal still return the saved targets

### Requirement: Validate goal values
The system SHALL reject a goal that:
- is missing any of the four nutrition targets;
- has a non-numeric target;
- has a calorie target that is not greater than zero;
- has a negative macro target;
- has a water target that is not a whole number from 1 to 20000;
- has a glass size that is not a whole number from 1 to 5000.

The system SHALL leave the stored goal unchanged when it rejects one.

#### Scenario: Missing target
- **WHEN** the client saves a goal that omits one of the four nutrition targets
- **THEN** the system responds with a 400-style validation error and the stored goal is unchanged

#### Scenario: Out-of-range values
- **WHEN** the client saves a goal with a calorie target of zero or less, a negative macro target, a water target outside 1–20000 ml, or a glass size outside 1–5000 ml
- **THEN** the system responds with a 400-style validation error and the stored goal is unchanged

### Requirement: Edit the goal from the app
The app SHALL provide a screen, reachable from Home at any time (through an edit-goal action in Home's header and by tapping the calorie progress indicator), that shows the current goal loaded from the backend. On that screen the user SHALL be able to edit and save the four nutrition targets, the daily water target and the glass size. The screen SHALL offer a back action, and going back SHALL return to Home on the day that was being viewed, with progress measured against the latest saved goal.

#### Scenario: Open the goal screen
- **WHEN** the user opens the goal screen
- **THEN** the app shows the goal currently stored in the backend, or the default when none is configured, and indicates when it is showing the default

#### Scenario: Save from the app
- **WHEN** the user edits the targets, including the water target and glass size, and saves them
- **THEN** the app sends them to the backend, and reopening the screen, including after restarting the app, shows the saved values

#### Scenario: Backend rejects the values
- **WHEN** the user saves values that the backend rejects, or the request fails
- **THEN** the app shows an error message and keeps the user's edits on screen so they can correct them

#### Scenario: Return to Home
- **WHEN** the user goes back from the goal screen (back arrow or system back) after viewing a past day on Home and saving a new goal
- **THEN** Home shows the same day it was showing before, with progress measured against the newly saved goal, including the new water target and glass size
