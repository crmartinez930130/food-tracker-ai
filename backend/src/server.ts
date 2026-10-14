import { config } from "./config.js";
import { openDatabase } from "./db/client.js";
import { createAnthropicClient } from "./services/anthropic-client.js";
import { createApp } from "./app.js";
import { logger } from "./logger.js";
import { createRateLimiter } from "./http/rate-limit.js";

const db = openDatabase(config.dbPath);
const anthropicClient = createAnthropicClient();
const app = createApp({
  db,
  anthropicClient,
  model: config.anthropicModel,
  apiToken: config.apiToken,
  analyzeLimiter: createRateLimiter({ max: config.analyzeRateLimitPerMinute, windowMs: 60_000 }),
});

app.listen(config.port, config.host, () => {
  logger.info("server_started", { host: config.host, port: config.port, model: config.anthropicModel });
});
