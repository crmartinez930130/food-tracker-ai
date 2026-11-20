# Spec Delta

## Purpose

Lets AI agents read the owner's food tracker data (meals, goals, weight and water) through the Model Context Protocol, without being able to change it.

## ADDED Requirements

### Requirement: MCP endpoint
The backend SHALL serve an MCP server over the Streamable HTTP transport at `/mcp`, on the same host and port as `/api`. It SHALL be stateless: each request is answered on its own, with no session to open or resume. The server SHALL identify itself as `food-tracker` with the backend's version.

#### Scenario: Agent connects
- **WHEN** an MCP client with the access token initializes a connection to `http://<host>:<port>/mcp`
- **THEN** the initialization succeeds and reports the server name `food-tracker` and the backend version

#### Scenario: Agent lists tools
- **WHEN** a connected client lists the tools
- **THEN** it gets `get_goals`, `get_daily_summary`, `get_range_summary`, `list_meals`, `get_meal`, `list_favorite_meals`, `list_weight_logs`, `list_water_logs` and `get_data_range`, each with a description and an input schema

#### Scenario: Session-only HTTP methods
- **WHEN** a client with the token sends `GET` or `DELETE` to `/mcp`
- **THEN** the response is 405, since there are no sessions or server-initiated streams

### Requirement: MCP access token
Every request to `/mcp` SHALL carry the same access token as `/api` (`Authorization: Bearer <token>`). A request without it, with another scheme or with a different token SHALL be rejected with 401 and an error message before any MCP message is read or any data is read.

#### Scenario: No token
- **WHEN** a client posts an MCP initialize request to `/mcp` without an `Authorization` header
- **THEN** the response is 401 with a `WWW-Authenticate: Bearer` header and no MCP response

#### Scenario: Wrong token
- **WHEN** a client sends a different token
- **THEN** the response is 401 and no tool runs

### Requirement: Read-only tools
Every MCP tool SHALL only read data: none SHALL create, change or delete meals, items, goals, weight or water entries. Every tool SHALL be marked to clients as read-only and idempotent. The existing `/api` routes SHALL keep their behavior.

#### Scenario: Tools don't write
- **WHEN** an agent calls every tool, with valid and invalid arguments
- **THEN** the stored meals, goals, weight and water entries are exactly the same as before

#### Scenario: Read-only hint
- **WHEN** a client lists the tools
- **THEN** each one carries `readOnlyHint: true` and `idempotentHint: true`

### Requirement: Tool results
Each tool SHALL return its data as JSON, both as structured content and as a text block holding the same JSON. Days SHALL be the owner's local calendar days, grouped the same way as the app's summaries (a meal or water entry belongs to the local day of its timestamp; a weighing to its stored day). Dates SHALL be `YYYY-MM-DD`, timestamps the stored ISO values, quantities in kcal, grams and millilitres.
- `get_goals` (no arguments): calorie, protein, fat, carb and water targets, the water glass size, and whether they are the built-in defaults.
- `get_daily_summary(date)`: the day's calorie and macro totals, meal count and water total, plus the goals.
- `get_range_summary(startDate, endDate)`: one entry per day from start to end inclusive, zero-filled, with calorie and macro totals, meal count and water.
- `list_meals(startDate, endDate, mealType?)`: the meals of those days, oldest first, each with id, timestamp, meal type, notes, favorite flag, totals and items (name, estimated grams, confidence, calories and macros); when `mealType` is given (`breakfast`, `lunch`, `dinner`, `snack` or `pre_workout`), only meals of that type.
- `get_meal(id)`: one meal in the same shape.
- `list_favorite_meals` (no arguments): the favorite meals in the same shape, newest first.
- `list_weight_logs(startDate, endDate)`: the weighings of those days, oldest first, with id, day, weight in kg and creation time.
- `list_water_logs(startDate, endDate)`: per day with entries, the day, its total and its entries (id, amount, timestamp), oldest first.
- `get_data_range` (no arguments): the local day of the earliest meal (or null when there are none) and the server's today.

#### Scenario: Daily summary
- **WHEN** an agent calls `get_daily_summary` with a date that has two meals and 500 ml of water
- **THEN** the result has those meals' summed totals, `mealCount` 2, `waterMl` 500 and the current goals

#### Scenario: Evening meal on the local day
- **WHEN** a meal was logged at 22:30 local time in a timezone behind UTC (the next day in UTC)
- **THEN** `list_meals` and `get_daily_summary` place it on the local day, like the app does

#### Scenario: Meals filtered by type
- **WHEN** an agent calls `list_meals` for a week with `mealType` `breakfast`
- **THEN** only that week's breakfasts are returned, each with its items

#### Scenario: Empty database
- **WHEN** an agent calls `get_data_range` and no meals were ever logged
- **THEN** the first day is null and today is the server's local date

### Requirement: Tool argument validation and limits
Tool arguments SHALL be validated before any data is read. Dates SHALL be real calendar dates in `YYYY-MM-DD` format, the end of a range SHALL NOT be before its start, and a range SHALL span at most 366 days for `get_range_summary` and `list_weight_logs`, and at most 31 days for `list_meals` and `list_water_logs`. `get_meal`'s id SHALL be a positive integer. An invalid argument, or a meal id that doesn't exist, SHALL produce a tool error result with a message that says what is wrong, not an HTTP or protocol failure.

#### Scenario: Impossible date
- **WHEN** an agent calls `get_daily_summary` with `2026-02-30`
- **THEN** the tool result is an error saying the date must be a real date in `YYYY-MM-DD` format

#### Scenario: Reversed range
- **WHEN** an agent calls `get_range_summary` with an end date before the start date
- **THEN** the tool result is an error saying the end must not be before the start

#### Scenario: Range too long for meals
- **WHEN** an agent calls `list_meals` for 32 days
- **THEN** the tool result is an error saying the range can't be longer than 31 days

#### Scenario: Unknown meal
- **WHEN** an agent calls `get_meal` with an id that doesn't exist
- **THEN** the tool result is an error saying the meal was not found
