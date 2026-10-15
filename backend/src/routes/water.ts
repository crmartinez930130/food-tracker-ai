import { Router } from "express";
import type { AppDatabase } from "../db/client.js";
import { deleteWaterLog, logWater, waterForDay } from "../db/water-repository.js";
import { NotFoundError, ValidationError } from "../errors.js";
import { asyncHandler } from "./async-handler.js";
import { invalidDateMessage, isCalendarDate } from "./dates.js";
import { LogWaterRequestSchema } from "./schemas.js";
import { validationErrorFrom } from "./validation.js";

export interface WaterRouterDeps {
  db: AppDatabase;
}

/** Tolerated phone-vs-server clock skew before a `loggedAt` counts as "in the future". */
const FUTURE_MARGIN_MS = 5 * 60 * 1000;

function parseId(raw: string | undefined): number {
  const id = Number(raw);
  if (!Number.isInteger(id) || id <= 0) {
    throw new ValidationError("Water entry id must be a positive integer");
  }
  return id;
}

export function createWaterRouter({ db }: WaterRouterDeps): Router {
  const router = Router();

  router.post(
    "/",
    asyncHandler(async (req, res) => {
      const parsed = LogWaterRequestSchema.safeParse(req.body);
      if (!parsed.success) {
        throw validationErrorFrom(parsed.error);
      }
      const now = Date.now();
      const loggedAtMs = parsed.data.loggedAt ? Date.parse(parsed.data.loggedAt) : now;
      if (loggedAtMs > now + FUTURE_MARGIN_MS) {
        throw new ValidationError("loggedAt: can't be in the future");
      }
      // Stored as UTC ISO whatever offset was sent, so entries sort correctly as strings. A retry of
      // an entry already stored (same clientId) gets that entry back with 200.
      const loggedAt = new Date(loggedAtMs).toISOString();
      const { entry, created } = logWater(db, parsed.data.amountMl, loggedAt, parsed.data.clientId);
      res.status(created ? 201 : 200).json(entry);
    }),
  );

  router.get(
    "/",
    asyncHandler(async (req, res) => {
      const date = req.query.date;
      if (!isCalendarDate(date)) {
        throw new ValidationError(invalidDateMessage("date"));
      }
      res.json(waterForDay(db, date));
    }),
  );

  router.delete(
    "/:id",
    asyncHandler(async (req, res) => {
      const id = parseId(req.params.id);
      if (!deleteWaterLog(db, id)) throw new NotFoundError(`Water entry ${id} not found`);
      res.status(204).send();
    }),
  );

  return router;
}
