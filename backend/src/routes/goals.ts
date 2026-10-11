import { Router } from "express";
import type { AppDatabase } from "../db/client.js";
import { getDailyGoal, saveDailyGoal } from "../db/goals-repository.js";
import { UpdateGoalsRequestSchema } from "./schemas.js";
import { asyncHandler } from "./async-handler.js";
import { validationErrorFrom } from "./validation.js";

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
        throw validationErrorFrom(parsed.error);
      }
      res.json(saveDailyGoal(db, parsed.data));
    }),
  );

  return router;
}
