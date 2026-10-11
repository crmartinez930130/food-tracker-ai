import { Router } from "express";
import type { AppDatabase } from "../db/client.js";
import { createWeightLog, deleteWeightLog, listWeightLogs } from "../db/weight-repository.js";
import { NotFoundError, ValidationError } from "../errors.js";
import { asyncHandler } from "./async-handler.js";
import { isCalendarDate, localToday, parseDateRange } from "./dates.js";
import { LogWeightRequestSchema } from "./schemas.js";

export interface WeightRouterDeps {
  db: AppDatabase;
}

function parseId(raw: string | undefined): number {
  const id = Number(raw);
  if (!Number.isInteger(id) || id <= 0) {
    throw new ValidationError("Weight entry id must be a positive integer");
  }
  return id;
}

export function createWeightRouter({ db }: WeightRouterDeps): Router {
  const router = Router();

  router.post(
    "/",
    asyncHandler(async (req, res) => {
      const parsed = LogWeightRequestSchema.safeParse(req.body);
      if (!parsed.success) {
        throw new ValidationError(
          parsed.error.issues.map((issue) => `${issue.path.join(".")}: ${issue.message}`).join("; "),
        );
      }
      const today = localToday();
      const day = parsed.data.day ?? today;
      if (!isCalendarDate(day)) {
        throw new ValidationError("day: must be a real date in YYYY-MM-DD format");
      }
      // ISO dates compare correctly as strings.
      if (day > today) {
        throw new ValidationError("day: can't be in the future");
      }
      res.status(201).json(createWeightLog(db, { weightKg: parsed.data.weightKg, day }));
    }),
  );

  router.get(
    "/",
    asyncHandler(async (req, res) => {
      const { startDate, endDate } = parseDateRange(req.query);
      res.json(listWeightLogs(db, startDate, endDate));
    }),
  );

  router.delete(
    "/:id",
    asyncHandler(async (req, res) => {
      const id = parseId(req.params.id);
      if (!deleteWeightLog(db, id)) throw new NotFoundError(`Weight entry ${id} not found`);
      res.status(204).send();
    }),
  );

  return router;
}
