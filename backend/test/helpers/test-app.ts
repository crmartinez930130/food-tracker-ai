import type Anthropic from "@anthropic-ai/sdk";
import express from "express";
import { createApp, type AppDeps } from "../../src/app.js";
import { openDatabase, type AppDatabase } from "../../src/db/client.js";
import { createRateLimiter, type RateLimiter } from "../../src/http/rate-limit.js";

type ParseImpl = (...args: unknown[]) => unknown;

/** The token test apps are configured with. */
export const TEST_API_TOKEN = "test-token-0123456789abcdef";

const DEFAULT_PARSE_RESULT = {
  parsed_output: {
    items: [],
    totalCalories: 0,
    totalProteinGrams: 0,
    totalFatGrams: 0,
    totalCarbsGrams: 0,
  },
  usage: { input_tokens: 0, output_tokens: 0 },
};

export interface TestAppOptions {
  /** When true (default), requests without an Authorization header get the test token added. */
  authorize?: boolean;
  analyzeLimiter?: RateLimiter;
}

/**
 * The real app on an in-memory database and a fake Anthropic client. By default every request
 * is sent with the test token, so route tests don't repeat it; auth tests pass `authorize: false`.
 */
export function buildTestApp(
  parseImpl?: ParseImpl,
  { authorize = true, analyzeLimiter = createRateLimiter({ max: 1000, windowMs: 60_000 }) }: TestAppOptions = {},
): { app: express.Express; db: AppDatabase } {
  const db = openDatabase(":memory:");
  const anthropicClient = {
    messages: {
      parse: parseImpl ?? (async () => DEFAULT_PARSE_RESULT),
    },
  } as unknown as Anthropic;

  const deps: AppDeps = { db, anthropicClient, model: "claude-opus-5", apiToken: TEST_API_TOKEN, analyzeLimiter };
  const app = createApp(deps);
  if (!authorize) return { app, db };

  const authorized = express();
  authorized.use((req, _res, next) => {
    req.headers.authorization ??= `Bearer ${TEST_API_TOKEN}`;
    next();
  });
  authorized.use(app);
  return { app: authorized, db };
}
