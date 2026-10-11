import express, { type Express, type NextFunction, type Request, type Response } from "express";
import type Anthropic from "@anthropic-ai/sdk";
import type { AppDatabase } from "./db/client.js";
import { createMealsRouter } from "./routes/meals.js";
import { createSummaryRouter } from "./routes/summary.js";
import { createGoalsRouter } from "./routes/goals.js";
import { createWaterRouter } from "./routes/water.js";
import { createWeightRouter } from "./routes/weight.js";
import { AnalysisError, NotFoundError, ValidationError } from "./errors.js";
import { logger } from "./logger.js";

export interface AppDeps {
  db: AppDatabase;
  anthropicClient: Anthropic;
  model: string;
}

export function createApp(deps: AppDeps): Express {
  const app = express();
  app.use(express.json());

  app.use("/api/meals", createMealsRouter(deps));
  app.use("/api/summary", createSummaryRouter(deps));
  app.use("/api/goals", createGoalsRouter(deps));
  app.use("/api/weight", createWeightRouter(deps));
  app.use("/api/water", createWaterRouter(deps));

  app.use((err: unknown, _req: Request, res: Response, _next: NextFunction) => {
    if (err instanceof ValidationError) {
      res.status(400).json({ error: err.message });
      return;
    }
    if (err instanceof NotFoundError) {
      res.status(404).json({ error: err.message });
      return;
    }
    if (err instanceof AnalysisError) {
      res.status(502).json({ error: err.message });
      return;
    }
    const message = err instanceof Error ? err.message : "Unknown error";
    logger.error("unhandled_request_error", { message });
    res.status(500).json({ error: "Internal server error" });
  });

  return app;
}
