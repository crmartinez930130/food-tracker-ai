import { z } from "zod";
import { MealAnalysisItemSchema } from "../services/meal-analysis-schema.js";
import { MEAL_TYPES } from "../db/schema.js";

const MealTypeSchema = z.enum(MEAL_TYPES);

export const SaveMealRequestSchema = z.object({
  items: z.array(MealAnalysisItemSchema),
  notes: z.string().nullable().optional(),
  occurredAt: z.string().nullable().optional(),
  mealType: MealTypeSchema.nullable().optional(),
});

/** PUT replaces the whole goal, so all four targets are required. */
export const UpdateGoalsRequestSchema = z.object({
  targetCalories: z.number().positive(),
  targetProteinGrams: z.number().nonnegative(),
  targetFatGrams: z.number().nonnegative(),
  targetCarbsGrams: z.number().nonnegative(),
});

export const UpdateMealRequestSchema = z.object({
  items: z.array(MealAnalysisItemSchema).optional(),
  notes: z.string().nullable().optional(),
  occurredAt: z.string().nullable().optional(),
  mealType: MealTypeSchema.nullable().optional(),
});
