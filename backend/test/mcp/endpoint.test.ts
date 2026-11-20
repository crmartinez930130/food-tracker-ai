import { afterEach, describe, expect, it } from "vitest";
import { connectMcpClient, startMcpTestApp, type McpTestContext } from "../helpers/mcp-client.js";
import { TEST_API_TOKEN } from "../helpers/test-app.js";
import { BACKEND_VERSION } from "../../src/version.js";

const INITIALIZE = {
  jsonrpc: "2.0",
  id: 1,
  method: "initialize",
  params: { protocolVersion: "2025-06-18", capabilities: {}, clientInfo: { name: "raw", version: "1" } },
};

const MCP_HEADERS = { "Content-Type": "application/json", Accept: "application/json, text/event-stream" };

const TOOL_NAMES = [
  "get_daily_summary",
  "get_data_range",
  "get_goals",
  "get_meal",
  "get_range_summary",
  "list_favorite_meals",
  "list_meals",
  "list_water_logs",
  "list_weight_logs",
];

let ctx: { close(): Promise<void> } | undefined;
afterEach(async () => {
  await ctx?.close();
  ctx = undefined;
});

describe("MCP endpoint", () => {
  it("initializes with the server name and the backend version", async () => {
    const c: McpTestContext = await connectMcpClient();
    ctx = c;
    expect(c.client.getServerVersion()).toMatchObject({ name: "food-tracker", version: BACKEND_VERSION });
  });

  it("lists every tool, each read-only and idempotent with a description and an input schema", async () => {
    const c = await connectMcpClient();
    ctx = c;
    const { tools } = await c.client.listTools();

    expect(tools.map((t) => t.name).sort()).toEqual(TOOL_NAMES);
    for (const tool of tools) {
      expect(tool.description, tool.name).toBeTruthy();
      expect(tool.inputSchema.type, tool.name).toBe("object");
      expect(tool.annotations, tool.name).toMatchObject({ readOnlyHint: true, idempotentHint: true, destructiveHint: false });
    }
  });

  it("rejects a request without a token with 401 before any MCP handling", async () => {
    const app = await startMcpTestApp();
    ctx = app;
    const res = await fetch(app.url, { method: "POST", headers: MCP_HEADERS, body: JSON.stringify(INITIALIZE) });

    expect(res.status).toBe(401);
    expect(res.headers.get("www-authenticate")).toBe("Bearer");
    expect(await res.json()).toEqual({ error: "Missing or invalid access token" });
  });

  it("rejects a wrong token with 401", async () => {
    const app = await startMcpTestApp();
    ctx = app;
    const res = await fetch(app.url, {
      method: "POST",
      headers: { ...MCP_HEADERS, Authorization: "Bearer wrong-token-0123456789" },
      body: JSON.stringify(INITIALIZE),
    });

    expect(res.status).toBe(401);
  });

  it("answers an initialize posted with the token", async () => {
    const app = await startMcpTestApp();
    ctx = app;
    const res = await fetch(app.url, {
      method: "POST",
      headers: { ...MCP_HEADERS, Authorization: `Bearer ${TEST_API_TOKEN}` },
      body: JSON.stringify(INITIALIZE),
    });

    expect(res.status).toBe(200);
    expect(await res.text()).toContain('"name":"food-tracker"');
  });

  it.each(["GET", "DELETE"])("answers %s with 405, since there are no sessions", async (method) => {
    const app = await startMcpTestApp();
    ctx = app;
    const res = await fetch(app.url, { method, headers: { Authorization: `Bearer ${TEST_API_TOKEN}` } });

    expect(res.status).toBe(405);
    expect(res.headers.get("allow")).toBe("POST");
  });
});
