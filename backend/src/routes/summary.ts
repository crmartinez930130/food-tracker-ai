import { Router } from "express";
import type { AppDatabase } from "../db/client.js";
import { dailySummary, weeklySummary } from "../services/summary.js";
import { ValidationError } from "../errors.js";
import { asyncHandler } from "./async-handler.js";

const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export interface SummaryRouterDeps {
  db: AppDatabase;
}

export function createSummaryRouter({ db }: SummaryRouterDeps): Router {
  const router = Router();

  router.get(
    "/daily",
    asyncHandler(async (req, res) => {
      const date = req.query.date;
      if (typeof date !== "string" || !DATE_RE.test(date)) {
        throw new ValidationError("Query parameter 'date' must be in YYYY-MM-DD format");
      }
      res.json(dailySummary(db, date));
    }),
  );

  router.get(
    "/weekly",
    asyncHandler(async (req, res) => {
      const startDate = req.query.startDate;
      if (typeof startDate !== "string" || !DATE_RE.test(startDate)) {
        throw new ValidationError("Query parameter 'startDate' must be in YYYY-MM-DD format");
      }
      res.json(weeklySummary(db, startDate));
    }),
  );

  return router;
}
