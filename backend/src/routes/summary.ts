import { Router } from "express";
import type { AppDatabase } from "../db/client.js";
import { dailySummary, rangeSummary, weeklySummary } from "../services/summary.js";
import { ValidationError } from "../errors.js";
import { asyncHandler } from "./async-handler.js";
import { invalidDateMessage, isCalendarDate, parseDateRange } from "./dates.js";


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
        throw new ValidationError(invalidDateMessage("date"));
      }
      res.json(dailySummary(db, date));
    }),
  );

  router.get(
    "/weekly",
    asyncHandler(async (req, res) => {
      const startDate = req.query.startDate;
      if (!isCalendarDate(startDate)) {
        throw new ValidationError(invalidDateMessage("startDate"));
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
