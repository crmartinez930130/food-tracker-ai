# Proposal

## Why

The food tracker's data (meals with their items, goals, weight and water) can only be seen in the phone app, on the watch or through the CSV export. AI agents such as Claude Code or Claude Desktop can't query it, so analyses like "how did my protein go this month against my goal" or "cross my meals with my Hevy workouts" need a manual export first (issue #122).

## What Changes

- The backend gains a **read-only MCP server** (Model Context Protocol) at `/mcp`, served over Streamable HTTP by the same process and port as `/api`, so any agent on the network can connect to either backend (PC or laptop) without copying the database.
- `/mcp` requires the same access token as `/api` (`Authorization: Bearer <API_TOKEN>`); without it the request gets 401 before any MCP handling.
- Tools offered to agents, all read-only:
  - `get_goals`: daily calorie, macro and water targets.
  - `get_daily_summary(date)`: one day's totals, meal count and water, next to the goals.
  - `get_range_summary(startDate, endDate)`: totals per day over a range, zero-filled.
  - `list_meals(startDate, endDate, mealType?)`: meals with their items and macros.
  - `get_meal(id)`: one meal with its items.
  - `list_favorite_meals`: meals marked as favorite.
  - `list_weight_logs(startDate, endDate)`: weighings.
  - `list_water_logs(startDate, endDate)`: water entries with per-day totals.
  - `get_data_range`: the first day with meals and the server's today, so the agent knows what it can ask for.
- Range limits so a single answer stays a reasonable size for an agent: 366 days for summaries and weight, 31 days for the tools that return individual meals or water entries.
- README section on connecting Claude Code and Claude Desktop.
- Version bumped to **0.3.0** (new functionality).
- Out of scope: writing, editing or deleting anything through MCP; the stdio transport; OAuth.

## Capabilities

### New Capabilities

- `mcp-server`: the read-only MCP endpoint, its authentication, the tools it offers, their inputs, outputs, limits and errors.

### Modified Capabilities

(none — `/api` behavior, including `api-access`, is unchanged)

## Impact

- Backend: new `src/mcp/` module (server factory + tools), `/mcp` route mounted in `createApp` behind `requireAccessToken`.
- Dependencies: `@modelcontextprotocol/server` and `@modelcontextprotocol/node` (SDK v2); `@modelcontextprotocol/client` as a dev dependency for tests.
- Tests: new MCP tests that connect the SDK client to the app over HTTP.
- `version.properties`, `backend/package.json`, `backend/package-lock.json` → 0.3.0.
- README: how to connect agents.
- Deploy: the laptop backend must be redeployed to expose `/mcp` against the phone's data. The phone and watch apps don't change.
