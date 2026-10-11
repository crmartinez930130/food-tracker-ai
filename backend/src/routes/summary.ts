import { Router } from "express";
import type { AppDatabase } from "../db/client.js";
import { dailySummary, rangeSummary, weeklySummary } from "../services/summary.js";
import { ValidationError } from "../errors.js";
import { asyncHandler } from "./async-handler.js";
import { isCalendarDate, parseDateRange } from "./dates.js";


export interface SummaryRouterDeps {
  db: AppDatabase;
}

export function createSummaryRouter({ db }: SummaryRouterDeps): Router {
  const router = Router();

  router.get(
    "/daily",
    asyncHandler(async (req, res) => {
      const date = req.query.date;
      if (!isCalendarDate(date)) {
        throw new ValidationError("Query parameter 'date' must be in YYYY-MM-DD format");
      }
      res.json(dailySummary(db, date));
    }),
  );

  router.get(
    "/weekly",
    asyncHandler(async (req, res) => {
      const startDate = req.query.startDate;
      if (!isCalendarDate(startDate)) {
        throw new ValidationError("Query parameter 'startDate' must be in YYYY-MM-DD format");
      }
      res.json(weeklySummary(db, startDate));
    }),
  );

  router.get(
    "/range",
    asyncHandler(async (req, res) => {
      const { startDate, endDate } = parseDateRange(req.query);
      res.json(rangeSummary(db, startDate, endDate));
    }),
  );

  return router;
}
