import { Router } from "express";
import multer from "multer";
import type Anthropic from "@anthropic-ai/sdk";
import type { AppDatabase } from "../db/client.js";
import {
  deleteMeal,
  firstMealDate,
  getMealById,
  listFavoriteMeals,
  listMealsByDate,
  saveMeal,
  updateMeal,
} from "../db/meal-repository.js";
import { exportFileName, exportMealsCsv } from "../services/csv-export.js";
import { detectImageType, preprocessImage } from "../services/image-preprocessing.js";
import { analyzeMeal, parseAnalysisLanguage, parseAnalysisPortion } from "../services/vision-analysis.js";
import { NotFoundError, ValidationError } from "../errors.js";
import { SaveMealRequestSchema, UpdateMealRequestSchema } from "./schemas.js";
import { asyncHandler } from "./async-handler.js";
import { rateLimit, type RateLimiter } from "../http/rate-limit.js";
import { invalidDateMessage, isCalendarDate, parseDateRange } from "./dates.js";
import { validationErrorFrom } from "./validation.js";


export interface MealsRouterDeps {
  db: AppDatabase;
  anthropicClient: Anthropic;
  model: string;
  analyzeLimiter: RateLimiter;
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

/** Most ingredient photos one analysis accepts; each adds input tokens to the call. */
export const MAX_INGREDIENT_PHOTOS = 5;

/** Most bytes all photos of one analysis may add up to (each is also capped at 15 MB). */
export const MAX_TOTAL_UPLOAD_BYTES = 40 * 1024 * 1024;

function parseId(raw: string): number {
  const id = Number(raw);
  if (!Number.isInteger(id)) {
    throw new ValidationError(`Invalid meal id: ${raw}`);
  }
  return id;
}

export function createMealsRouter({ db, anthropicClient, model, analyzeLimiter }: MealsRouterDeps): Router {
  const router = Router();

  router.post(
    "/analyze",
    // Before multer: a rejected call never buffers its upload.
    rateLimit(analyzeLimiter),
    upload.fields([
      { name: "photo", maxCount: 1 },
      { name: "ingredients", maxCount: MAX_INGREDIENT_PHOTOS },
    ]),
    asyncHandler(async (req, res) => {
      // Multer puts the form's text fields in req.body; every photo is optional.
      const files = (req.files ?? {}) as Record<string, Express.Multer.File[] | undefined>;
      const photo = files.photo?.[0];
      const ingredientFiles = files.ingredients ?? [];
      const rawDescription = req.body?.description;
      const description = typeof rawDescription === "string" ? rawDescription.trim() : "";
      if (description.length > MAX_DESCRIPTION_LENGTH) {
        throw new ValidationError(`description: must be at most ${MAX_DESCRIPTION_LENGTH} characters`);
      }
      if (!photo && ingredientFiles.length === 0 && !description) {
        throw new ValidationError("Provide a photo, ingredient photos, a description, or a mix");
      }
      const uploads = [...(photo ? [photo] : []), ...ingredientFiles];
      if (uploads.reduce((total, file) => total + file.size, 0) > MAX_TOTAL_UPLOAD_BYTES) {
        throw new ValidationError("Photos too large together; the limit is 40 MB");
      }
      if (uploads.some((file) => !detectImageType(file.buffer))) {
        throw new ValidationError("Unsupported image format; only JPEG and PNG are accepted");
      }
      const image = photo ? await preprocessImage(photo.buffer) : undefined;
      const ingredients = await Promise.all(ingredientFiles.map((file) => preprocessImage(file.buffer)));
      const result = await analyzeMeal(
        anthropicClient,
        {
          image,
          ingredients,
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
      // A retry of a save already stored (same clientId) gets that meal back with 200.
      const { entry, created } = saveMeal(db, parsed.data);
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
