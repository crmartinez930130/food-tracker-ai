import type Anthropic from "@anthropic-ai/sdk";
import { createApp, type AppDeps } from "../../src/app.js";
import { openDatabase, type AppDatabase } from "../../src/db/client.js";

type ParseImpl = (...args: unknown[]) => unknown;

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

export function buildTestApp(parseImpl?: ParseImpl): { app: ReturnType<typeof createApp>; db: AppDatabase } {
  const db = openDatabase(":memory:");
  const anthropicClient = {
    messages: {
      parse: parseImpl ?? (async () => DEFAULT_PARSE_RESULT),
    },
  } as unknown as Anthropic;

  const deps: AppDeps = { db, anthropicClient, model: "claude-opus-5" };
  return { app: createApp(deps), db };
}
