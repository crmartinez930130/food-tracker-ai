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

export const UpdateMealRequestSchema = z.object({
  items: z.array(MealAnalysisItemSchema).optional(),
  notes: z.string().nullable().optional(),
  occurredAt: z.string().nullable().optional(),
  mealType: MealTypeSchema.nullable().optional(),
});
