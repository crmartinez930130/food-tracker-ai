import { Router } from "express";
import type { AppDatabase } from "../db/client.js";
import { dailySummary, rangeSummary, weeklySummary } from "../services/summary.js";
import { ValidationError } from "../errors.js";
import { asyncHandler } from "./async-handler.js";

const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;
const MAX_RANGE_DAYS = 366;
const MS_PER_DAY = 24 * 60 * 60 * 1000;

/** YYYY-MM-DD that is also a real calendar date (the regex alone accepts 2026-02-30). */
function isCalendarDate(value: unknown): value is string {
  if (typeof value !== "string" || !DATE_RE.test(value)) return false;
  const parsed = new Date(`${value}T00:00:00.000Z`);
  return !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value;
}

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

  router.get(
    "/range",
    asyncHandler(async (req, res) => {
      const { startDate, endDate } = req.query;
      if (!isCalendarDate(startDate) || !isCalendarDate(endDate)) {
        throw new ValidationError("Query parameters 'startDate' and 'endDate' must be dates in YYYY-MM-DD format");
      }
      const days = (Date.parse(endDate) - Date.parse(startDate)) / MS_PER_DAY + 1;
      if (days < 1) {
        throw new ValidationError("'endDate' must not be before 'startDate'");
      }
      if (days > MAX_RANGE_DAYS) {
        throw new ValidationError(`The range can't be longer than ${MAX_RANGE_DAYS} days`);
      }
      res.json(rangeSummary(db, startDate, endDate));
    }),
  );

  return router;
}
