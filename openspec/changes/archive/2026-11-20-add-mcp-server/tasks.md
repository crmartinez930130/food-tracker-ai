# Tasks

## 1. Setup

- [x] 1.1 Add `@modelcontextprotocol/server` and `@modelcontextprotocol/node` (v2) as dependencies and `@modelcontextprotocol/client` as a dev dependency in `backend/`; verify `npm ci` and `npm run typecheck` pass
- [x] 1.2 Bump the version to 0.3.0 in `version.properties`, `backend/package.json` and `backend/package-lock.json`; verify `test/routes/version.test.ts` passes

## 2. Endpoint and authentication

- [x] 2.1 Make the range validation take a maximum number of days (default 366, so `/api` is unchanged) and verify the existing `dates` tests still pass plus a new test for a custom maximum
- [x] 2.2 Create `src/mcp/server.ts` with `createMcpServer(db)` (name `food-tracker`, version `BACKEND_VERSION`, no tools yet) and `src/mcp/route.ts` with the stateless `POST /mcp` handler and 405 for `GET`/`DELETE`; mount it in `createApp` behind `requireAccessToken`; verify the app still starts with `npm run dev`
- [x] 2.3 Add a test helper that starts the test app on a random port and connects the SDK client with the token; add `test/mcp/endpoint.test.ts` covering initialize (name and version), 401 without/with a wrong token (`WWW-Authenticate: Bearer`), and 405 on `GET`/`DELETE`; verify it passes

## 3. Tools

- [x] 3.1 Add the shared tool plumbing (JSON result as text + structured content, `ValidationError`/not-found → `isError` result, unexpected errors logged and returned as a generic tool error, `readOnlyHint`/`idempotentHint` annotations); verify with a test that lists tools and checks the annotations
- [x] 3.2 Implement `get_goals`, `get_data_range` and `get_daily_summary` (totals, meal count, water, goals); verify with tests for stored and default goals, empty and non-empty databases, a local-day evening meal, and an impossible date
- [x] 3.3 Implement `get_range_summary` and `list_weight_logs` (max 366 days); verify with tests for zero-filled days, reversed ranges and over-long ranges
- [x] 3.4 Implement `list_meals` (optional `mealType`, oldest first, max 31 days), `get_meal` and `list_favorite_meals`; verify with tests for items in the output, the type filter, 32-day rejection, unknown and non-positive ids
- [x] 3.5 Implement `list_water_logs` (per-day totals and entries, max 31 days); verify with a test covering several days
- [x] 3.6 Add a test that calls every tool (valid and invalid arguments) and checks that meals, goals, weight and water rows are unchanged; verify it passes

## 4. Documentation and verification

- [x] 4.1 Add an "MCP para agentes de IA" section to the README: tools, limits, `claude mcp add --transport http food-tracker http://<host>:3000/mcp --header "Authorization: Bearer <API_TOKEN>"`, Claude Desktop config, and a note about keeping the token private; verify the command works as written in 4.2
- [x] 4.2 Run `npm run typecheck` and `npm test` (all green), then connect Claude Code to the local backend with the documented command and call several tools against the PC's data; record the result in the PR
