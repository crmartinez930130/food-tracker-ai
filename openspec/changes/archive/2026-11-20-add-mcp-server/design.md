# Design

## Context

The backend is one Express app (`createApp`) over a single SQLite database, used by one person. Everything under `/api` sits behind `requireAccessToken` (Bearer token from `API_TOKEN`). The data access the MCP tools need already exists as plain functions over `AppDatabase`: `getDailyGoal`, `dailySummary`, `rangeSummary`, `listMealsBetween`, `getMealById`, `listFavoriteMeals`, `firstMealDate`, `listWeightLogs`, `waterForDay` / `waterTotalsBetween`, plus `isCalendarDate`, `parseDateRange` and `localToday` in `routes/dates.ts`. Days are local days via SQLite `'localtime'`, which assumes the server's timezone is the owner's. The backend runs on the PC (emulator) and on the laptop (phone), both on the home LAN. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- One MCP endpoint that any MCP client with the token can use from the LAN, against whichever backend it points at.
- Reuse the existing repositories and date rules so MCP answers match the app.
- Testable end to end with the SDK's own client.

**Non-Goals:**
- Writes, MCP resources/prompts, sessions, server-initiated notifications, OAuth, stdio.

## Decisions

### SDK v2 split packages
Use `@modelcontextprotocol/server` (`McpServer`, `registerTool`) and `@modelcontextprotocol/node` (`NodeStreamableHTTPServerTransport`), current v2 line (server 2.4.x). Tests use `@modelcontextprotocol/client` (`Client`, `StreamableHTTPClientTransport` with `requestInit.headers.Authorization`). v2 takes Zod v4 schemas directly, which the backend already uses (`zod` ^4.6).
*Alternative:* the v1 monolithic `@modelcontextprotocol/sdk` (1.32). It still works, but v2 is the maintained line and its docs; starting on v1 would mean a migration later. We don't use `@modelcontextprotocol/express` (`createMcpExpressApp`), since it creates its own app and we mount a route on ours.

### Mount in the existing app, behind the same token
`app.use("/mcp", requireAccessToken(apiToken), express.json(), mcpRouter)` in `createApp`, so the token check runs before the body is parsed or any MCP code runs. Same port, same process, same database handle; nothing new to deploy or configure.
*Alternative:* a separate `npm run mcp` stdio process opening the SQLite file. Rejected by the owner: it only works on the machine holding the database.

### Stateless: one server + transport per POST
For each `POST /mcp`, build an `McpServer` with the tools, a `NodeStreamableHTTPServerTransport({ sessionIdGenerator: undefined })`, connect, `handleRequest(req, res, req.body)`, and close both when the response closes. `GET` and `DELETE` answer 405 with a JSON-RPC error body. Building the server per request is cheap (a few closures) and avoids shared state between agents.
*Alternative:* sessions with an in-memory map. Unneeded: no tool streams or notifies, and stateless survives restarts.

### Tool module layout
`src/mcp/server.ts` exports `createMcpServer(db)` (registers all tools) and `src/mcp/route.ts` exports `createMcpRouter(db)`. Tools are thin: validate → call the existing repository/service → return `{ content: [{ type: "text", text: JSON }], structuredContent }`. Keeping the server factory separate from HTTP lets tests call it in-process too if needed.

### Validation reuses the API's rules
Zod input schemas declare shapes (`z.string()`, `z.number().int().positive()`, `z.enum(MEAL_TYPES)`), so agents see proper JSON Schema. Calendar validity and range checks call the existing `isCalendarDate` and a variant of `parseDateRange` that takes the max days as a parameter (366 for summaries/weight, 31 for meals/water). A `ValidationError` or a missing meal is turned into `{ isError: true, content: [{ type: "text", text: message }] }`; any other exception is logged and returned as a generic tool error, never a 500.
*Why 31 days for meals/water:* `list_meals` returns every item; a year of meals is hundreds of KB of JSON, too much for an agent's context. 31 days covers "this month"; longer questions go through `get_range_summary`.

### `get_daily_summary` includes water and goals
The `/api/summary/daily` route returns only meal totals; the app fetches water and goals separately. For an agent one call is better, so the tool composes `rangeSummary(db, date, date)[0]` (totals, meal count, water) with `getDailyGoal(db)`. No change to the HTTP route.

### Output shapes
Return the repository objects as they are (they already match what the app shows), except `list_meals` for a range is ordered oldest first (`occurredAt`) for readability, and `list_water_logs` groups entries per day using `waterForDay` for each day that `waterTotalsBetween` reports. `clientId` stays in meal objects; it's harmless and keeps one shape.

## Risks / Trade-offs

- [The token is a full-access secret now given to agent configs] → Same token as the app; documented in the README with a note to keep it out of shared repos. A separate read-only token can come later if needed.
- [LAN exposure over plain HTTP] → Unchanged from `/api`: home network only, laptop firewall limits port 3000 to 192.168.1.0/24.
- [DNS rebinding against a localhost MCP server] → Not exploitable here: a browser page can't attach the Bearer token, so every request it makes gets 401.
- [SDK v2 is younger than v1; API may move] → Pin versions in package.json with `^2.x`; tests cover connect/list/call so a breaking bump fails CI.
- [`listMealsBetween` loads items one query per meal] → Bounded by the 31-day cap (~150 meals); SQLite in-process, fine.
- [Server timezone must be the owner's] → Same assumption as the whole backend; documented already.

## Migration Plan

Merge → tag `v0.3.0` → redeploy the laptop backend (stop service, back up `data/`, copy `backend/`, `npm ci && npm run build`, start). No database migration. Rollback: redeploy the previous tag; nothing depends on `/mcp`.
