import { Router } from "express";
import type { AppDatabase } from "../db/client.js";
import { getDailyGoal, saveDailyGoal } from "../db/goals-repository.js";
import { ValidationError } from "../errors.js";
import { UpdateGoalsRequestSchema } from "./schemas.js";
import { asyncHandler } from "./async-handler.js";

export interface GoalsRouterDeps {
  db: AppDatabase;
}

export function createGoalsRouter({ db }: GoalsRouterDeps): Router {
  const router = Router();

  router.get(
    "/",
    asyncHandler(async (_req, res) => {
      res.json(getDailyGoal(db));
    }),
  );

  router.put(
    "/",
    asyncHandler(async (req, res) => {
      const parsed = UpdateGoalsRequestSchema.safeParse(req.body);
      if (!parsed.success) {
        // One readable line per problem (e.g. "targetCalories: Too small: expected number to be >0"),
        // since the app shows this text to the user as-is.
        throw new ValidationError(
          parsed.error.issues.map((issue) => `${issue.path.join(".")}: ${issue.message}`).join("; "),
        );
      }
      res.json(saveDailyGoal(db, parsed.data));
    }),
  );

  return router;
}
