import { Router } from "express";
import multer from "multer";
import type Anthropic from "@anthropic-ai/sdk";
import type { AppDatabase } from "../db/client.js";
import {
  createMeal,
  deleteMeal,
  firstMealDate,
  getMealById,
  listFavoriteMeals,
  listMealsByDate,
  updateMeal,
} from "../db/meal-repository.js";
import { exportFileName, exportMealsCsv } from "../services/csv-export.js";
import { preprocessImage } from "../services/image-preprocessing.js";
import { analyzeMeal, parseAnalysisLanguage, parseAnalysisPortion } from "../services/vision-analysis.js";
import { NotFoundError, ValidationError } from "../errors.js";
import { SaveMealRequestSchema, UpdateMealRequestSchema } from "./schemas.js";
import { asyncHandler } from "./async-handler.js";
import { invalidDateMessage, isCalendarDate, parseDateRange } from "./dates.js";
import { validationErrorFrom } from "./validation.js";


export interface MealsRouterDeps {
  db: AppDatabase;
  anthropicClient: Anthropic;
  model: string;
}

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 15 * 1024 * 1024 },
  fileFilter: (_req, file, cb) => {
    if (file.mimetype === "image/jpeg" || file.mimetype === "image/png") {
      cb(null, true);
    } else {
      cb(new ValidationError("Unsupported file type; only JPEG and PNG images are accepted"));
    }
  },
});

/** Longest free-text meal description the analysis accepts. */
const MAX_DESCRIPTION_LENGTH = 500;

function parseId(raw: string): number {
  const id = Number(raw);
  if (!Number.isInteger(id)) {
    throw new ValidationError(`Invalid meal id: ${raw}`);
  }
  return id;
}

export function createMealsRouter({ db, anthropicClient, model }: MealsRouterDeps): Router {
  const router = Router();

  router.post(
    "/analyze",
    upload.single("photo"),
    asyncHandler(async (req, res) => {
      // Multer puts the form's text fields in req.body (the photo itself is optional).
      const rawDescription = req.body?.description;
      const description = typeof rawDescription === "string" ? rawDescription.trim() : "";
      if (description.length > MAX_DESCRIPTION_LENGTH) {
        throw new ValidationError(`description: must be at most ${MAX_DESCRIPTION_LENGTH} characters`);
      }
      if (!req.file && !description) {
        throw new ValidationError("Provide a photo, a description, or both");
      }
      const image = req.file ? await preprocessImage(req.file.buffer) : undefined;
      const result = await analyzeMeal(
        anthropicClient,
        {
          image,
          description: description || undefined,
          portion: parseAnalysisPortion(req.body?.portion),
          language: parseAnalysisLanguage(req.body?.language),
        },
        model,
      );
      res.status(200).json(result.analysis);
    }),
  );

  router.post(
    "/",
    asyncHandler(async (req, res) => {
      const parsed = SaveMealRequestSchema.safeParse(req.body);
      if (!parsed.success) {
        throw validationErrorFrom(parsed.error);
      }
      const entry = createMeal(db, parsed.data);
      res.status(201).json(entry);
    }),
  );

  router.get(
    "/",
    asyncHandler(async (req, res) => {
      const date = req.query.date;
      if (!isCalendarDate(date)) {
        throw new ValidationError(invalidDateMessage("date"));
      }
      res.json(listMealsByDate(db, date));
    }),
  );

  // Like "/favorites", these must stay above "/:id".
  // Lets clients bound date pickers to where the data actually starts (see the export picker).
  router.get(
    "/first-date",
    asyncHandler(async (_req, res) => {
      res.json({ date: firstMealDate(db) });
    }),
  );

  router.get(
    "/export",
    asyncHandler(async (req, res) => {
      const { startDate, endDate } = parseDateRange(req.query);
      res
        .status(200)
        .attachment(exportFileName(startDate, endDate))
        .type("text/csv; charset=utf-8")
        .send(exportMealsCsv(db, startDate, endDate));
    }),
  );

  // Must stay above "/:id", or Express would route "favorites" there as an (invalid) id.
  router.get(
    "/favorites",
    asyncHandler(async (_req, res) => {
      res.json(listFavoriteMeals(db));
    }),
  );

  router.get(
    "/:id",
    asyncHandler(async (req, res) => {
      const id = parseId(req.params.id);
      const entry = getMealById(db, id);
      if (!entry) throw new NotFoundError(`Meal ${id} not found`);
      res.json(entry);
    }),
  );

  router.patch(
    "/:id",
    asyncHandler(async (req, res) => {
      const id = parseId(req.params.id);
      const parsed = UpdateMealRequestSchema.safeParse(req.body);
      if (!parsed.success) {
        throw validationErrorFrom(parsed.error);
      }
      const entry = updateMeal(db, id, parsed.data);
      if (!entry) throw new NotFoundError(`Meal ${id} not found`);
      res.json(entry);
    }),
  );

  router.delete(
    "/:id",
    asyncHandler(async (req, res) => {
      const id = parseId(req.params.id);
      const deleted = deleteMeal(db, id);
      if (!deleted) throw new NotFoundError(`Meal ${id} not found`);
      res.status(204).send();
    }),
  );

  return router;
}
