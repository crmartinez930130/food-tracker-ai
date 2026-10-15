import { z } from "zod";
import { MealAnalysisItemSchema } from "../services/meal-analysis-schema.js";
import { MEAL_TYPES } from "../db/schema.js";

const MealTypeSchema = z.enum(MEAL_TYPES);

/**
 * The UUID a client generates once per meal or water entry and repeats on every retry of its
 * creation. Lower-cased so the same id in another letter case finds the same entry. Nullable
 * because the app encodes its defaults, sending `null` when it has none.
 */
const ClientIdSchema = z
  .uuid()
  .transform((id) => id.toLowerCase())
  .nullable()
  .optional();

export const SaveMealRequestSchema = z.object({
  items: z.array(MealAnalysisItemSchema),
  notes: z.string().nullable().optional(),
  occurredAt: z.string().nullable().optional(),
  mealType: MealTypeSchema.nullable().optional(),
  clientId: ClientIdSchema,
});

/** PUT replaces the whole goal, so all four targets are required. */
export const UpdateGoalsRequestSchema = z.object({
  targetCalories: z.number().positive(),
  targetProteinGrams: z.number().nonnegative(),
  targetFatGrams: z.number().nonnegative(),
  targetCarbsGrams: z.number().nonnegative(),
  // Optional so a client that only sends the nutrition targets keeps the stored water settings.
  targetWaterMl: z.number().int().min(1).max(20000).optional(),
  waterGlassMl: z.number().int().min(1).max(5000).optional(),
});

export const UpdateMealRequestSchema = z.object({
  items: z.array(MealAnalysisItemSchema).optional(),
  notes: z.string().nullable().optional(),
  occurredAt: z.string().nullable().optional(),
  mealType: MealTypeSchema.nullable().optional(),
  isFavorite: z.boolean().optional(),
});

/** `day` is optional (defaults to today) and checked as a real, non-future date in the route. */
export const LogWaterRequestSchema = z.object({
  amountMl: z.number().int().min(1).max(5000),
  loggedAt: z.iso.datetime({ offset: true }).optional(),
  clientId: ClientIdSchema,
});

export const LogWeightRequestSchema = z.object({
  weightKg: z.number().positive().max(500),
  day: z.string().optional(),
});
