import "dotenv/config";
import { parseApiToken } from "./http/access-token.js";
import { parseAnalyzeRateLimit } from "./http/rate-limit.js";

function requireEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

export const config = {
  anthropicApiKey: requireEnv("ANTHROPIC_API_KEY"),
  apiToken: parseApiToken(process.env.API_TOKEN),
  analyzeRateLimitPerMinute: parseAnalyzeRateLimit(process.env.ANALYZE_RATE_LIMIT_PER_MINUTE),
  anthropicModel: process.env.ANTHROPIC_MODEL ?? "claude-opus-5",
  host: process.env.HOST ?? "0.0.0.0",
  port: Number(process.env.PORT ?? 3000),
  dbPath: process.env.DB_PATH ?? "data/food-tracker.sqlite",
};
