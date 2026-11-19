# app-versioning Specification

## Purpose
Gives the phone app, the watch app and the backend one shared, visible version, so it is always clear which build each device runs and whether the backend is up to date.

## Requirements

### Requirement: Single project version
The project SHALL have one version, written once in a file at the repository root. The phone app, the watch app and the backend SHALL all report that version, and changing that file SHALL be the only edit needed to change it. A build of the phone or watch app SHALL fail when the version is missing or invalid.

#### Scenario: Bump the version
- **WHEN** the version file is changed from `0.2.0` to `0.3.0` and the apps and backend are rebuilt
- **THEN** the phone app, the watch app and the backend all report `0.3.0`

#### Scenario: Backend out of sync
- **WHEN** the backend's own package version differs from the version file
- **THEN** the backend test suite fails, naming both versions

#### Scenario: Invalid version
- **WHEN** the version file holds `0.3` or `1.2.100`
- **THEN** building the phone or watch app fails with a message explaining the expected format

### Requirement: Version format and bumps
The version SHALL follow `MAJOR.MINOR.PATCH`, each part a whole number, with MINOR and PATCH from 0 to 99. A change that adds functionality SHALL raise MINOR and reset PATCH; a change that only fixes behavior SHALL raise PATCH; a change that makes the apps and the backend incompatible with older versions of each other SHALL raise MAJOR and reset the others. The bump SHALL be part of the same pull request as the change. Changes that touch neither the apps' nor the backend's behavior (documentation, specs, CI) SHALL NOT bump it. The first version is `0.2.0`.

#### Scenario: New feature
- **WHEN** a pull request adds a new meal type while the version is `0.2.0`
- **THEN** the same pull request sets the version to `0.3.0`

#### Scenario: Bug fix
- **WHEN** a pull request only fixes a bug while the version is `0.3.0`
- **THEN** the same pull request sets the version to `0.3.1`

### Requirement: Android version code
The phone and watch apps SHALL use the version as their Android version name, and SHALL derive their Android version code from it as `MAJOR × 10000 + MINOR × 100 + PATCH`, so a later version always has a higher code and Android refuses to install an older build over a newer one.

#### Scenario: Version code of 0.2.0
- **WHEN** the version is `0.2.0`
- **THEN** both apps have version name `0.2.0` and version code `200`

#### Scenario: Version code of 1.3.5
- **WHEN** the version is `1.3.5`
- **THEN** both apps have version code `10305`

### Requirement: Backend version endpoint
The backend SHALL answer `GET /api/version` with its version as JSON, `{"version": "<MAJOR.MINOR.PATCH>"}`. The endpoint SHALL require the access token like every other `/api` request.

#### Scenario: Read the version
- **WHEN** a client with the access token calls `GET /api/version` on a backend at `0.2.0`
- **THEN** the response is 200 with `{"version": "0.2.0"}`

#### Scenario: Without the token
- **WHEN** a client calls `GET /api/version` without the access token
- **THEN** the response is 401 and the version is not revealed

### Requirement: Release tags
After a pull request that changes the version is merged, the merge commit on `master` SHALL be tagged `v<version>` (for example `v0.2.0`). The repository's README SHALL describe how to bump the version and tag a release.

#### Scenario: Tag after merge
- **WHEN** the pull request that sets the version to `0.2.0` is merged
- **THEN** its merge commit on `master` is tagged `v0.2.0`
