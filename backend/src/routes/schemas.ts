import { z } from "zod";
import { MealAnalysisItemSchema } from "../services/meal-analysis-schema.js";

export const SaveMealRequestSchema = z.object({
  items: z.array(MealAnalysisItemSchema),
  notes: z.string().nullable().optional(),
  occurredAt: z.string().nullable().optional(),
});

export const UpdateMealRequestSchema = z.object({
  items: z.array(MealAnalysisItemSchema).optional(),
  notes: z.string().nullable().optional(),
  occurredAt: z.string().nullable().optional(),
});
