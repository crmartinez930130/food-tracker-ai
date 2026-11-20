import type { AddressInfo } from "node:net";
import type { Server } from "node:http";
import { Client, StreamableHTTPClientTransport, type CallToolResult } from "@modelcontextprotocol/client";
import { buildTestApp, TEST_API_TOKEN } from "./test-app.js";
import type { AppDatabase } from "../../src/db/client.js";

export interface McpTestContext {
  db: AppDatabase;
  /** The `/mcp` URL of the running test app. */
  url: string;
  /** A client connected with the test token. */
  client: Client;
  close(): Promise<void>;
}

/** Starts the real app (in-memory database) on a random local port: the MCP client needs real HTTP. */
export async function startMcpTestApp(): Promise<Omit<McpTestContext, "client">> {
  const { app, db } = buildTestApp(undefined, { authorize: false });
  const server: Server = await new Promise((resolve) => {
    const s = app.listen(0, "127.0.0.1", () => resolve(s));
  });
  const { port } = server.address() as AddressInfo;
  return {
    db,
    url: `http://127.0.0.1:${port}/mcp`,
    close: () => new Promise<void>((resolve, reject) => server.close((err) => (err ? reject(err) : resolve()))),
  };
}

/** The test app plus an MCP client connected to it with the test token. */
export async function connectMcpClient(): Promise<McpTestContext> {
  const ctx = await startMcpTestApp();
  const client = new Client({ name: "food-tracker-tests", version: "1.0.0" });
  await client.connect(
    new StreamableHTTPClientTransport(new URL(ctx.url), {
      requestInit: { headers: { Authorization: `Bearer ${TEST_API_TOKEN}` } },
    }),
  );
  return {
    ...ctx,
    client,
    close: async () => {
      await client.close();
      await ctx.close();
    },
  };
}

/** Calls a tool and returns its result, typed. */
export async function callTool(client: Client, name: string, args: Record<string, unknown> = {}): Promise<CallToolResult> {
  return (await client.callTool({ name, arguments: args })) as CallToolResult;
}

/** The tool's text content: the JSON answer, or an error message. */
export function textOf(result: CallToolResult): string {
  const first = result.content[0];
  if (!first || first.type !== "text") throw new Error("expected a text content block");
  return first.text;
}

/** Calls a tool that must succeed and returns its structured content, checking it equals the text JSON. */
export async function callOk<T = Record<string, unknown>>(client: Client, name: string, args: Record<string, unknown> = {}): Promise<T> {
  const result = await callTool(client, name, args);
  if (result.isError) throw new Error(`${name} failed: ${textOf(result)}`);
  if (JSON.stringify(result.structuredContent) !== textOf(result)) {
    throw new Error(`${name}: structured content and text differ`);
  }
  return result.structuredContent as T;
}
